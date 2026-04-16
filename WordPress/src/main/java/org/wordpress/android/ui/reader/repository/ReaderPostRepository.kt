package org.wordpress.android.ui.reader.repository

import com.android.volley.VolleyError
import com.wordpress.rest.RestRequest
import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.wordpress.android.WordPress
import org.wordpress.android.WordPress.Companion.getRestClientUtilsV1_2
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsPostTagProvider.Companion.BLOGGING_PROMPT_TAG
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.actions.ReaderActions
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResultListener
import org.wordpress.android.ui.reader.exceptions.ReaderPostFetchException
import org.wordpress.android.ui.reader.repository.usecases.ParseDiscoverCardsJsonUseCase
import org.wordpress.android.ui.reader.repository.usecases.tags.GetFollowedTagsUseCase
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter
import org.wordpress.android.ui.reader.sources.ReaderPostLocalSource
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.PerAppLocaleManager
import org.wordpress.android.util.UrlUtils
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Reusable
class ReaderPostRepository @Inject constructor(
    private val perAppLocaleManager: PerAppLocaleManager,
    private val localSource: ReaderPostLocalSource,
    private val getFollowedTagsUseCase: GetFollowedTagsUseCase,
    private val parseDiscoverCardsJsonUseCase: ParseDiscoverCardsJsonUseCase,
    private val appPrefsWrapper: AppPrefsWrapper,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    @Named(APPLICATION_SCOPE) private val applicationScope: CoroutineScope,
) {
    /**
     * Fetches and returns the most recent posts for the passed tag, respecting the maxPosts limit.
     * It always fetches the most recent posts, saves them to the local DB and returns the latest from that cache.
     */
    suspend fun fetchNewerPostsForTag(tag: ReaderTag, maxPosts: Int = 10): ReaderPostList = withContext(ioDispatcher) {
        suspendCancellableCoroutine { cont ->
            val resultListener = UpdateResultListener { result ->
                if (result == ReaderActions.UpdateResult.FAILED) {
                    cont.resumeWithException(
                        ReaderPostFetchException("Failed to fetch newer posts for tag: ${tag.tagSlug}")
                    )
                } else {
                    val posts = ReaderPostTable.getPostsWithTag(tag, maxPosts, false)
                    cont.resume(posts)
                }
            }
            requestPostsWithTag(tag, ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER, resultListener)
        }
    }

    fun requestPostsWithTag(
        tag: ReaderTag,
        updateAction: ReaderPostServiceStarter.UpdateAction,
        resultListener: UpdateResultListener
    ) {
        // The Discover "Recommended" and "Latest" sub-tabs use the v2 cards-style pipeline.
        // Freshly Pressed falls through to the regular tag-based flow below, which hits the
        // v1.2 /freshly-pressed endpoint carried on tag.endpoint.
        if (tag.tagType == ReaderTagType.DEFAULT && tag.tagSlug in DISCOVER_STREAM_TAG_SLUGS) {
            requestPostsForDiscoverStream(tag, updateAction, resultListener)
            return
        }
        val path = getRelativeEndpointForTag(tag)
        if (path.isNullOrBlank()) {
            resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED)
            return
        }
        val sb = StringBuilder(path)

        // append #posts to retrieve
        sb.append("?number=").append(ReaderConstants.READER_MAX_POSTS_TO_REQUEST)

        // return newest posts first (this is the default, but make it explicit since it's important)
        sb.append("&order=DESC")

        val beforeDate: String? = when (updateAction) {
            ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER -> {
                // request posts older than the oldest existing post with this tag
                ReaderPostTable.getOldestDateWithTag(tag)
            }

            ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER_THAN_GAP -> {
                // request posts older than the post with the gap marker for this tag
                ReaderPostTable.getGapMarkerDateForTag(tag)
            }

            ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER,
            ReaderPostServiceStarter.UpdateAction.REQUEST_REFRESH -> null
        }

        if (!beforeDate.isNullOrBlank()) {
            sb.append("&before=").append(UrlUtils.urlEncode(beforeDate))
        }
        sb.append("&meta=site,likes")
        sb.append("&lang=").append(perAppLocaleManager.getCurrentLocaleLanguageCode())

        val listener = RestRequest.Listener { jsonObject: JSONObject? ->
            // remember when this tag was updated if newer posts were requested
            if (updateAction == ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER ||
                updateAction == ReaderPostServiceStarter.UpdateAction.REQUEST_REFRESH
            ) {
                ReaderTagTable.setTagLastUpdated(tag)
            }
            handleUpdatePostsResponse(tag, jsonObject, updateAction, resultListener)
        }

        val errorListener = RestRequest.ErrorListener { volleyError: VolleyError? ->
            AppLog.e(AppLog.T.READER, volleyError)
            resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED)
        }

        getRestClientUtilsV1_2().get(sb.toString(), null, null, listener, errorListener)
    }

    fun requestPostsForBlog(
        blogId: Long,
        updateAction: ReaderPostServiceStarter.UpdateAction,
        resultListener: UpdateResultListener
    ) {
        var path = "read/sites/$blogId/posts/?meta=site,likes"

        // append the date of the oldest cached post in this blog when requesting older posts
        if (updateAction == ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER) {
            val dateOldest = ReaderPostTable.getOldestPubDateInBlog(blogId)
            if (!dateOldest.isNullOrBlank()) {
                path += "&before=" + UrlUtils.urlEncode(dateOldest)
            }
        }
        val listener = RestRequest.Listener { jsonObject ->
            handleUpdatePostsResponse(null, jsonObject, updateAction, resultListener)
        }
        val errorListener = RestRequest.ErrorListener { volleyError ->
            AppLog.e(AppLog.T.READER, volleyError)
            resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED)
        }
        AppLog.d(AppLog.T.READER, "updating posts in blog $blogId")
        getRestClientUtilsV1_2().getWithLocale(path, null, null, listener, errorListener)
    }

    fun requestPostsForFeed(
        feedId: Long,
        updateAction: ReaderPostServiceStarter.UpdateAction,
        resultListener: UpdateResultListener
    ) {
        var path = "read/feed/$feedId/posts/?meta=site,likes"
        if (updateAction == ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER) {
            val dateOldest = ReaderPostTable.getOldestPubDateInFeed(feedId)
            if (!dateOldest.isNullOrBlank()) {
                path += "&before=" + UrlUtils.urlEncode(dateOldest)
            }
        }
        val listener = RestRequest.Listener { jsonObject ->
            handleUpdatePostsResponse(null, jsonObject, updateAction, resultListener, feedId)
        }
        val errorListener = RestRequest.ErrorListener { volleyError ->
            AppLog.e(AppLog.T.READER, volleyError)
            resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED)
        }
        AppLog.d(AppLog.T.READER, "updating posts in feed $feedId")
        getRestClientUtilsV1_2().getWithLocale(path, null, null, listener, errorListener)
    }

    /**
     * called after requesting posts with a specific tag or in a specific blog/feed
     *
     * @param requestedFeedId If provided, ensures all posts have this feedId set. This is needed
     *        because the API response may not include feed_ID for external feeds, but we need it
     *        to properly query posts later.
     */
    private fun handleUpdatePostsResponse(
        tag: ReaderTag?,
        jsonObject: JSONObject?,
        updateAction: ReaderPostServiceStarter.UpdateAction,
        resultListener: UpdateResultListener,
        requestedFeedId: Long? = null
    ) {
        if (jsonObject == null) {
            resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED)
            return
        }

        // this should ideally be done using coroutines, but this class is currently being used from Java, which makes
        // it difficult to use coroutines. This should be refactored to use coroutines when possible.
        object : Thread() {
            override fun run() {
                val serverPosts = ReaderPostList.fromJson(jsonObject)
                // For feed requests, always set the feedId on all posts to the requested feedId.
                // The API response may not include feed_ID, or may include a different value,
                // but we need the feedId to match what we'll query for later.
                if (requestedFeedId != null && requestedFeedId != 0L) {
                    serverPosts.forEach { post ->
                        post.feedId = requestedFeedId
                    }
                }
                val updateResult = localSource.saveUpdatedPosts(serverPosts, updateAction, tag)
                resultListener.onUpdateResult(updateResult)
            }
        }.start()
    }

    /**
     * Requests posts for a Discover "Recommended" or "Latest" sub-tab using the v2
     * /read/streams/discover endpoint. Latest adds sort=date while Recommended uses
     * the server default ordering. Freshly Pressed uses a separate v1.2 endpoint and
     * is handled by [requestPostsWithTag].
     *
     * Pagination is cursor-based via an opaque page_handle stored per-stream in AppPrefs.
     * First-page requests also include a "refresh" counter so the server returns a different
     * shard of content, matching the existing ReaderDiscoverLogic behavior.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun requestPostsForDiscoverStream(
        tag: ReaderTag,
        updateAction: ReaderPostServiceStarter.UpdateAction,
        resultListener: UpdateResultListener
    ) {
        applicationScope.launch(ioDispatcher) {
            try {
                val params = mutableMapOf<String, String>()

                // Use the user's followed tags to seed the discover stream. If the user doesn't
                // follow anything (ignoring the default dailyprompt tag) fall back to
                // "dailyprompt,wordpress" — mirrors ReaderDiscoverLogic / iOS behavior.
                val userTags = getFollowedTagsUseCase.get()
                params["tags"] = if (userTags.none { it.tagSlug != BLOGGING_PROMPT_TAG }) {
                    "$BLOGGING_PROMPT_TAG,wordpress"
                } else {
                    userTags.joinToString(",") { it.tagSlug }
                }

                // Latest sorts by date; Recommended uses the server's default order.
                if (tag.tagSlug == ReaderTag.TAG_SLUG_LATEST) {
                    params["sort"] = "date"
                }

                // REQUEST_OLDER_THAN_GAP is intentionally treated as a first-page refresh:
                // the cursor-based streams endpoint has no equivalent to ReaderPostTable's
                // gap markers, so starting fresh is the safest recovery path.
                val isFirstPage =
                    updateAction == ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER ||
                        updateAction == ReaderPostServiceStarter.UpdateAction.REQUEST_REFRESH ||
                        updateAction == ReaderPostServiceStarter.UpdateAction.REQUEST_OLDER_THAN_GAP

                if (isFirstPage) {
                    // Clear the stored cursor so the next "load more" starts from the new first
                    // page, then bump the refresh counter so the server rotates the shard.
                    appPrefsWrapper.setReaderDiscoverStreamPageHandle(tag.tagSlug, null)
                    params["refresh"] =
                        appPrefsWrapper.getReaderCardsRefreshCounter().toString()
                    appPrefsWrapper.incrementReaderCardsRefreshCounter()
                } else {
                    // REQUEST_OLDER: resume pagination with the previously-stored cursor.
                    val pageHandle =
                        appPrefsWrapper.getReaderDiscoverStreamPageHandle(tag.tagSlug)
                    if (pageHandle.isNullOrEmpty()) {
                        resultListener.onUpdateResult(ReaderActions.UpdateResult.UNCHANGED)
                        return@launch
                    }
                    params["page_handle"] = pageHandle
                }

                params["_locale"] = perAppLocaleManager.getCurrentLocaleLanguageCode()

                val listener = RestRequest.Listener { jsonObject: JSONObject? ->
                    applicationScope.launch(ioDispatcher) {
                        handleDiscoverStreamResponse(
                            tag, jsonObject, updateAction, resultListener
                        )
                    }
                }
                val errorListener = RestRequest.ErrorListener { volleyError: VolleyError? ->
                    AppLog.e(AppLog.T.READER, volleyError)
                    resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED)
                }

                WordPress.getRestClientUtilsV2().get(
                    tag.endpoint,
                    params,
                    null,
                    listener,
                    errorListener
                )
            } catch (e: Exception) {
                AppLog.e(AppLog.T.READER, "Discover stream request failed", e)
                resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED)
            }
        }
    }

    /**
     * Parses a /read/streams/discover response: filters the cards array down to post cards,
     * converts them into ReaderPosts, stores the next page handle, and saves the posts keyed
     * by the requesting stream tag via ReaderPostLocalSource (so that gap handling and the
     * existing REQUEST_REFRESH/REQUEST_NEWER semantics keep working).
     *
     * Must be called from a coroutine on [ioDispatcher] (it does blocking DB work).
     */
    private fun handleDiscoverStreamResponse(
        tag: ReaderTag,
        jsonObject: JSONObject?,
        updateAction: ReaderPostServiceStarter.UpdateAction,
        resultListener: UpdateResultListener
    ) {
        if (jsonObject == null) {
            resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED)
            return
        }
        try {
            val serverPosts = parsePostCards(jsonObject)

            // Remember when the tag was last updated for first-page requests, matching the
            // behavior of the regular tag-based flow in requestPostsWithTag.
            if (updateAction == ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER ||
                updateAction == ReaderPostServiceStarter.UpdateAction.REQUEST_REFRESH
            ) {
                ReaderTagTable.setTagLastUpdated(tag)
            }

            // Store the next_page_handle for this stream (empty means we're at the end).
            val nextPageHandle = parseDiscoverCardsJsonUseCase.parseNextPageHandle(jsonObject)
                .takeIf { it.isNotEmpty() }
            appPrefsWrapper.setReaderDiscoverStreamPageHandle(tag.tagSlug, nextPageHandle)

            val updateResult = localSource.saveUpdatedPosts(serverPosts, updateAction, tag)
            resultListener.onUpdateResult(updateResult)
        } catch (e: JSONException) {
            AppLog.e(AppLog.T.READER, e)
            resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED)
        }
    }

    private fun parsePostCards(jsonObject: JSONObject): ReaderPostList {
        val posts = ReaderPostList()
        val cardsJson = jsonObject.optJSONArray(ReaderConstants.JSON_CARDS)
            ?: return posts
        val seenPostIds = HashSet<Long>()
        for (i in 0 until cardsJson.length()) {
            val post = parsePostCard(cardsJson.optJSONObject(i)) ?: continue
            if (seenPostIds.add(post.postId)) {
                posts.add(post)
            }
        }
        return posts
    }

    private fun parsePostCard(cardJson: JSONObject?): ReaderPost? {
        if (cardJson == null ||
            cardJson.optString(ReaderConstants.JSON_CARD_TYPE) != ReaderConstants.JSON_CARD_POST
        ) return null
        return try {
            parseDiscoverCardsJsonUseCase.parsePostCard(cardJson)
        } catch (e: JSONException) {
            AppLog.w(
                AppLog.T.READER,
                "Failed to parse discover post card: ${e.message}"
            )
            null
        }
    }

    /**
     * returns the endpoint to use when requesting posts with the passed tag
     */
    private fun getRelativeEndpointForTag(tag: ReaderTag): String? {
        val endpoint = tag.endpoint?.takeIf { it.isNotBlank() } // if passed tag has an assigned endpoint, use it
            ?: ReaderTagTable.getEndpointForTag(tag)?.takeIf { it.isNotBlank() } // check the db for the endpoint

        return endpoint
            ?.let { getRelativeEndpoint(it) }
            ?: if (tag.tagType == ReaderTagType.DEFAULT) {
                // never hand craft the endpoint for default tags, since these MUST be updated using their endpoints
                null
            } else {
                formatRelativeEndpointForTag(tag.tagSlug)
            }
    }

    /**
     * returns the passed endpoint without the unnecessary path - this is
     * needed because as of 20-Feb-2015 the /read/menu/ call returns the
     * full path but we don't want to use the full path since it may change
     * between API versions (as it did when we moved from v1 to v1.1)
     *
     * ex: https://public-api.wordpress.com/rest/v1/read/tags/fitness/posts
     * becomes just read/tags/fitness/posts
     */
    @Suppress("MagicNumber")
    private fun getRelativeEndpoint(endpoint: String): String {
        return endpoint.takeIf { it.startsWith("http") }
            ?.let {
                var pos = it.indexOf("/read/")
                if (pos > -1) {
                    return@let it.substring(pos + 1)
                }
                pos = it.indexOf("/v1/")
                if (pos > -1) {
                    return@let it.substring(pos + 4)
                }
                return@let it
            }
            ?: endpoint
    }

    companion object {
        private val DISCOVER_STREAM_TAG_SLUGS = setOf(
            ReaderTag.TAG_SLUG_RECOMMENDED,
            ReaderTag.TAG_SLUG_LATEST,
        )

        private fun formatRelativeEndpointForTag(tagSlug: String): String {
            return String.format(Locale.US, "read/tags/%s/posts", ReaderUtils.sanitizeWithDashes(tagSlug))
        }

        fun formatFullEndpointForTag(tagSlug: String): String {
            return (getRestClientUtilsV1_2().restClient.endpointURL + formatRelativeEndpointForTag(tagSlug))
        }
    }
}
