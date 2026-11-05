package org.wordpress.android.ui.reader.repository

import com.wordpress.rest.RestRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.datasets.ReaderBlogTable
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderBlogList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.actions.ReaderActions
import org.wordpress.android.ui.reader.actions.ReaderTagActions
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.JSONUtils
import org.wordpress.android.util.PerAppLocaleManager
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Updates the Reader's followed tags and followed sites from the network and reports changes to them,
 * including follows and unfollows made in the app, through [events]. Updates run in the application scope
 * so they finish, and their results are saved, even if the screen that asked for them goes away. A request
 * for an update that's already running joins it rather than starting another one.
 */
@Singleton
class ReaderTagsAndSitesUpdater @Inject constructor(
    private val accountStore: AccountStore,
    private val perAppLocaleManager: PerAppLocaleManager,
    private val clientUtilsProvider: TagUpdateClientUtilsProvider,
    private val resourceProvider: ResourceProvider,
    @Named(APPLICATION_SCOPE) private val applicationScope: CoroutineScope,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    private val _events = MutableSharedFlow<ReaderTagsAndSitesEvent>(extraBufferCapacity = EVENT_BUFFER)
    val events: SharedFlow<ReaderTagsAndSitesEvent> = _events.asSharedFlow()

    private val runningUpdates = mutableMapOf<UpdateTask, Deferred<Boolean>>()

    fun update(tasks: Set<UpdateTask>) {
        // tags go first (they're first in UpdateTask) since without them the Reader can't show anything
        tasks.sorted().forEach { start(it) }
    }

    /**
     * Updates the followed tags and returns whether the update succeeded
     */
    suspend fun updateTags(): Boolean = start(UpdateTask.TAGS).await()

    /**
     * Returns the interests (tags) the user can pick from, or null if the request failed
     */
    suspend fun fetchInterestTags(): ReaderTagList? {
        AppLog.d(AppLog.T.READER, "reader updater > fetching interest tags")
        val params = mapOf("_locale" to perAppLocaleManager.getCurrentLocaleLanguageCode())
        val json = request { listener, errorListener ->
            clientUtilsProvider.getRestClientForInterestTags()
                .get("read/interests", params, null, listener, errorListener)
        } ?: return null
        return withContext(ioDispatcher) { parseInterestTags(json) }
    }

    /**
     * Follows the passed tags and returns whether it succeeded. The local write happens right away, so
     * callers can read the new follow state before this returns.
     */
    suspend fun followTags(tags: List<ReaderTag>): Boolean = awaitTagAction { listener, isLoggedIn ->
        ReaderTagActions.addTags(tags, listener, isLoggedIn)
    }

    /**
     * Unfollows the passed tag and returns whether it succeeded. The local write happens right away.
     */
    suspend fun unfollowTag(tag: ReaderTag): Boolean = awaitTagAction { listener, isLoggedIn ->
        ReaderTagActions.deleteTag(tag, listener, isLoggedIn)
    }

    /*
     * runs a tag action and reports the change to the followed tags. [start] returns false if the action
     * didn't start.
     */
    private suspend fun awaitTagAction(
        start: (ReaderActions.ActionListener, Boolean) -> Boolean
    ): Boolean = withContext(ioDispatcher) {
        val succeeded = suspendCancellableCoroutine { continuation ->
            val listener = ReaderActions.ActionListener { succeeded ->
                if (continuation.isActive) continuation.resume(succeeded)
            }
            if (!start(listener, accountStore.hasAccessToken()) && continuation.isActive) {
                continuation.resume(false)
            }
        }
        // a failed action is reverted locally, so it's a change too
        _events.emit(ReaderTagsAndSitesEvent.FollowedTagsChanged(ReaderTagTable.getFollowedTags().size, true))
        succeeded
    }

    /*
     * starts the passed update, or returns the running one. This doesn't use async since a failure would
     * cancel the application scope.
     */
    private fun start(task: UpdateTask): Deferred<Boolean> = synchronized(runningUpdates) {
        runningUpdates[task]?.takeIf { it.isActive } ?: CompletableDeferred<Boolean>().also { result ->
            runningUpdates[task] = result
            applicationScope.launch(ioDispatcher) {
                result.complete(
                    when (task) {
                        UpdateTask.TAGS -> requestTags()
                        UpdateTask.FOLLOWED_BLOGS -> requestFollowedBlogs()
                    }
                )
            }
        }
    }

    /*
     * updates the tags the user follows - this also handles the default tags (such as Discover) since
     * they're included in the response
     */
    private suspend fun requestTags(): Boolean {
        AppLog.d(AppLog.T.READER, "reader updater > updating tags")
        val params = mapOf("locale" to perAppLocaleManager.getCurrentLocaleLanguageCode())
        val json = request { listener, errorListener ->
            clientUtilsProvider.getRestClientForTagUpdate()
                .getWithLocale("read/menu", params, null, listener, errorListener)
        } ?: return false

        val serverTopics = ReaderTagList()
        serverTopics.addAll(parseTags(json, "default", ReaderTagType.DEFAULT))
        updateDisplayNames(serverTopics)
        serverTopics.addAll(parseTags(json, "subscribed", ReaderTagType.FOLLOWED))

        // the server doesn't know about the Bookmark tag, so it's added here
        serverTopics.add(
            ReaderTag(
                "",
                resourceProvider.getString(R.string.reader_save_for_later_display_name),
                resourceProvider.getString(R.string.reader_save_for_later_title),
                "",
                ReaderTagType.BOOKMARKED
            )
        )
        // the DISCOVER_POST_CARDS tag stores the posts for the Discover tab
        serverTopics.add(ReaderTag.createDiscoverPostCardsTag())

        val localTopics = ReaderTagList().apply {
            addAll(ReaderTagTable.getDefaultTags())
            addAll(ReaderTagTable.getFollowedTags())
            addAll(ReaderTagTable.getBookmarkTags())
            addAll(ReaderTagTable.getCustomListTags())
            addAll(ReaderTagTable.getDiscoverPostCardsTags())
        }

        val didChange = !localTopics.isSameList(serverTopics)
        if (didChange) {
            AppLog.d(AppLog.T.READER, "reader updater > followed topics changed")
            if (!accountStore.hasAccessToken()) {
                // don't delete the tags a logged out user saved locally
                ReaderTagTable.addOrUpdateTags(serverTopics)
            } else {
                // delete local topics which have been removed from the server, then replace the rest
                ReaderTagTable.deleteTags(localTopics.getDeletions(serverTopics))
                ReaderTagTable.replaceTags(serverTopics)
            }
        }
        _events.emit(ReaderTagsAndSitesEvent.FollowedTagsChanged(ReaderTagTable.getFollowedTags().size, didChange))
        AppPrefs.setReaderTagsUpdatedTimestamp(System.currentTimeMillis())
        return true
    }

    /*
     * sets the display names of the default tags (such as Subscribed and Discover)
     */
    private fun updateDisplayNames(serverTopics: ReaderTagList) {
        serverTopics.forEach { tag ->
            when {
                tag.isFollowedSites -> R.string.reader_subscribed_display_name
                tag.isDiscover -> R.string.reader_discover_display_name
                tag.isPostsILike -> R.string.reader_my_likes_display_name
                else -> null
            }?.let { tag.tagDisplayName = resourceProvider.getString(it) }
        }
    }

    /*
     * requests every page of the sites the user follows, then saves them if they changed
     */
    private suspend fun requestFollowedBlogs(): Boolean {
        val serverBlogs = ReaderBlogList()
        var page = 1
        while (true) {
            AppLog.d(AppLog.T.READER, "reader updater > updating followed blogs. Page requested: $page")
            // ?meta=site,feed returns extra info
            val path = "read/following/mine?number=100&page=$page&meta=site%2Cfeed"
            val json = request { listener, errorListener ->
                WordPress.getRestClientUtilsV1_2().getWithLocale(path, listener, errorListener)
            } ?: return false

            // under rare circumstances the server returns duplicates
            serverBlogs.addAll(ReaderBlogList.fromJson(json).distinctBy { it.blogId })
            // the server doesn't always return as many sites as requested, likely due to deleted or suspended
            // sites, so this asks for another page once more than 90 are returned. At worst that page is empty.
            if (json.optInt("number", 0) > MIN_SITES_FOR_NEXT_PAGE) {
                page = json.optInt("page", 1) + 1
            } else {
                saveFollowedBlogs(serverBlogs, json.optInt("total_subscriptions", 0))
                return true
            }
        }
    }

    private suspend fun saveFollowedBlogs(serverBlogs: ReaderBlogList, totalSites: Int) {
        var didChange = false
        val localBlogs = ReaderBlogTable.getFollowedBlogs()
        if (!localBlogs.isSameList(serverBlogs)) {
            // save the server list if there are *any* changes (including subscription count, description,
            // etc.) but only update the follow status and report a change if a site has been followed or
            // unfollowed since the local list was last updated
            ReaderBlogTable.setFollowedBlogs(serverBlogs)
            if (!localBlogs.hasSameBlogs(serverBlogs)) {
                ReaderPostTable.updateFollowedStatus()
                AppLog.i(AppLog.T.READER, "reader updater > followed blogs changed: $totalSites")
                didChange = true
            }
        }
        _events.emit(ReaderTagsAndSitesEvent.FollowedBlogsChanged(totalSites, didChange))
    }

    /*
     * runs a Volley request and returns its response, or null if it failed
     */
    private suspend fun request(
        start: (RestRequest.Listener, RestRequest.ErrorListener) -> Unit
    ): JSONObject? = suspendCancellableCoroutine { continuation ->
        start(
            { json -> if (continuation.isActive) continuation.resume(json) },
            { volleyError ->
                AppLog.e(AppLog.T.READER, volleyError)
                if (continuation.isActive) continuation.resume(null)
            }
        )
    }

    private fun parseTags(json: JSONObject, name: String, tagType: ReaderTagType): ReaderTagList {
        val topics = ReaderTagList()
        val jsonTopics = json.optJSONObject(name) ?: return topics
        jsonTopics.keys().forEach { internalName ->
            val jsonTopic = jsonTopics.optJSONObject(internalName) ?: return@forEach
            val title = JSONUtils.getStringDecoded(jsonTopic, ReaderConstants.JSON_TAG_TITLE)
            val displayName = JSONUtils.getStringDecoded(jsonTopic, ReaderConstants.JSON_TAG_DISPLAY_NAME)
            val slug = JSONUtils.getStringDecoded(jsonTopic, ReaderConstants.JSON_TAG_SLUG)
            val endpoint = JSONUtils.getString(jsonTopic, ReaderConstants.JSON_TAG_URL)
            // custom lists are included in the response as default tags
            val type = if (tagType == ReaderTagType.DEFAULT && endpoint.contains("/read/list/")) {
                ReaderTagType.CUSTOM_LIST
            } else {
                tagType
            }
            topics.add(ReaderTag(slug, displayName, title, endpoint, type))
        }
        return topics
    }

    private fun parseInterestTags(json: JSONObject): ReaderTagList {
        val interestTags = ReaderTagList()
        val jsonInterests = json.optJSONArray(INTERESTS) ?: return interestTags
        for (i in 0 until jsonInterests.length()) {
            val jsonInterest = jsonInterests.optJSONObject(i) ?: continue
            val title = JSONUtils.getStringDecoded(jsonInterest, ReaderConstants.JSON_TAG_TITLE)
            val slug = JSONUtils.getStringDecoded(jsonInterest, ReaderConstants.JSON_TAG_SLUG)
            interestTags.add(ReaderTag(slug, title, title, "", ReaderTagType.INTERESTS))
        }
        return interestTags
    }

    companion object {
        private const val EVENT_BUFFER = 16
        private const val INTERESTS = "interests"
        private const val MIN_SITES_FOR_NEXT_PAGE = 90
    }
}

enum class UpdateTask {
    TAGS,
    FOLLOWED_BLOGS
}

sealed class ReaderTagsAndSitesEvent {
    data class FollowedTagsChanged(val totalTags: Int, val didChange: Boolean) : ReaderTagsAndSitesEvent()
    data class FollowedBlogsChanged(val totalSubscriptions: Int, val didChange: Boolean) : ReaderTagsAndSitesEvent()
}
