package org.wordpress.android.ui.reader.viewmodels

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.repository.ReaderSubsEvent
import org.wordpress.android.ui.reader.repository.ReaderSubsRepository
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.EnumSet
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

/**
 * Owns Manage subscriptions: the followed tags and sites, the site search, and following a tag or URL
 * typed into the entry field.
 */
@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class ReaderSubsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ReaderSubsRepository,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val readerTracker: ReaderTracker,
    private val resourceProvider: ResourceProvider,
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableStateFlow(
        ReaderSubsUiState(
            selectedTab = initialTab(),
            siteSearchQuery = savedStateHandle[KEY_SITE_SEARCH_QUERY],
            entry = savedStateHandle[KEY_ENTRY] ?: ""
        )
    )
    val uiState: StateFlow<ReaderSubsUiState> = _uiState.asStateFlow()

    private val _events = Channel<ReaderSubsUiEvent>(Channel.BUFFERED)
    val events: Flow<ReaderSubsUiEvent> = _events.receiveAsFlow()

    // every followed site, sorted - the state holds the ones matching the search
    private var followedSites = emptyList<ReaderSubsSite>()

    // the tags followed when the screen opened, so the result can say whether they changed
    private var initialTagSlugs: Set<String>? = null

    // follow requests in flight, keyed by tag slug or site key, so a reload keeps their buttons disabled
    private val pendingTagFollows = mutableMapOf<String, Boolean>()
    private val pendingSiteFollows = mutableMapOf<String, Boolean>()

    // false until the tags and followed sites have been synced, which waits for a connection
    private var hasRequestedUpdate = false

    private var tagsJob: Job? = null
    private var sitesJob: Job? = null

    init {
        readerTracker.track(Stat.READER_MANAGE_VIEW_DISPLAYED)
        requestInitialUpdate()
        loadTags()
        launch {
            repository.events.collect { event ->
                when (event) {
                    ReaderSubsEvent.FollowedTagsFetched -> loadTags()
                    is ReaderSubsEvent.FollowedBlogsFetched -> if (event.didChange) loadSites()
                }
            }
        }
    }

    /*
     * the user may have changed what they follow from a site preview, so reload the sites
     */
    fun onResume() {
        requestInitialUpdate()
        loadSites()
    }

    fun onBack() = readerTracker.track(Stat.READER_MANAGE_VIEW_DISMISSED)

    fun hasChangedTags(): Boolean {
        val initial = initialTagSlugs ?: return false
        val current = _uiState.value.tags.orEmpty().filter { it.isFollowing }.map { it.slug }.toSet()
        return initial != current
    }

    fun onTabSelected(tab: ReaderSubsTab) {
        savedStateHandle[KEY_SELECTED_TAB] = tab
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onInterestsClosed() {
        requestUpdate(EnumSet.of(UpdateTask.TAGS))
    }

    fun onSiteSearchOpened() = setSiteSearchQuery("")

    fun onSiteSearchQueryChanged(query: String) = setSiteSearchQuery(query)

    fun onSiteSearchClosed() = setSiteSearchQuery(null)

    fun onSiteClicked(site: ReaderSubsSite) {
        _events.trySend(ReaderSubsUiEvent.ShowSitePreview(site.blogId, site.feedId, site.isFollowing))
    }

    fun onTagFollowClicked(item: ReaderSubsTag) {
        if (!checkConnection()) return
        val follow = !item.isFollowing
        // tracked up front, as before, so leaving the screen mid-request doesn't lose it
        trackTag(if (follow) Stat.READER_TAG_FOLLOWED else Stat.READER_TAG_UNFOLLOWED, item.slug)
        pendingTagFollows[item.slug] = follow
        updateTag(item.slug) { it.copy(isFollowing = follow, isFollowEnabled = false) }
        launch {
            val succeeded = repository.followTag(item.tag, follow)
            pendingTagFollows.remove(item.slug)
            updateTag(item.slug) { it.copy(isFollowEnabled = true) }
            if (!succeeded) {
                showMessage(R.string.reader_toast_err_removing_tag)
                loadTags()
            }
        }
    }

    fun onSiteFollowClicked(site: ReaderSubsSite) {
        if (!checkConnection()) return
        val follow = !site.isFollowing
        pendingSiteFollows[site.key] = follow
        updateSite(site.key) { it.copy(isFollowing = follow, isFollowEnabled = false) }
        launch {
            val succeeded = repository.followSite(site.blogId, site.feedId, follow)
            pendingSiteFollows.remove(site.key)
            updateSite(site.key) { it.copy(isFollowing = if (succeeded) follow else !follow, isFollowEnabled = true) }
            if (succeeded) {
                requestUpdate(EnumSet.of(UpdateTask.TAGS))
            } else {
                showMessage(
                    if (follow) {
                        R.string.reader_toast_err_unable_to_follow_blog
                    } else {
                        R.string.reader_toast_err_unable_to_unfollow_blog
                    }
                )
            }
        }
    }

    fun onEntryChanged(entry: String) = setEntry(entry.take(MAX_ENTRY_LENGTH))

    /*
     * follow the tag or url the user typed into the entry field
     */
    fun onAddClicked() {
        val entry = _uiState.value.entry.trim()
        if (entry.isEmpty() || _uiState.value.isAdding) return
        val isUrl = !entry.contains(" ") && (entry.contains(".") || entry.contains("://"))
        if (isUrl) addAsUrl(entry) else addAsTag(entry)
    }

    private fun addAsTag(entry: String) {
        when {
            !repository.isValidTagName(entry) -> showMessage(R.string.reader_toast_err_tag_not_valid)
            repository.isFollowedTagName(entry) -> showMessage(R.string.reader_toast_err_tag_already_following)
            checkConnection() -> followNewTag(entry)
        }
    }

    private fun followNewTag(name: String) {
        clearEntry()
        val tag = repository.createFollowedTag(name)
        _uiState.update { it.copy(isAdding = true) }
        launch {
            val succeeded = repository.followTag(tag, true)
            _uiState.update { it.copy(isAdding = false) }
            loadTags()
            if (succeeded) {
                showMessage(resourceProvider.getString(R.string.reader_label_added_tag, tag.label))
                trackTag(Stat.READER_TAG_FOLLOWED, tag.tagSlug)
            } else {
                showMessage(R.string.reader_toast_err_adding_tag)
            }
        }
    }

    private fun addAsUrl(entry: String) {
        val url = repository.normalizeEntryUrl(entry)
        when {
            // not a valid URL, so add the original entry as a tag
            url == null -> addAsTag(entry)
            repository.isFollowedUrl(url) -> showMessage(R.string.reader_toast_err_already_follow_this_blog)
            checkConnection() -> followNewUrl(url)
        }
    }

    private fun followNewUrl(url: String) {
        _uiState.update { it.copy(isAdding = true) }
        launch {
            val failedStatusCode = repository.checkUrlReachable(url)
            if (failedStatusCode != null) {
                _uiState.update { it.copy(isAdding = false) }
                showMessage(getUrlErrorMessage(failedStatusCode))
                return@launch
            }
            val succeeded = repository.followUrl(url)
            _uiState.update { it.copy(isAdding = false) }
            if (succeeded) {
                clearEntry()
                showMessage(R.string.reader_label_blog_subscribed)
                loadSites()
                // the site may belong to a tag we don't have yet, and the filter sheet lists the followed sites
                requestUpdate(EnumSet.of(UpdateTask.TAGS, UpdateTask.FOLLOWED_BLOGS))
            } else {
                showMessage(R.string.reader_toast_err_unable_to_follow_blog)
            }
        }
    }

    private fun getUrlErrorMessage(statusCode: Int): String = when (statusCode) {
        HTTP_UNAUTHORIZED -> resourceProvider.getString(R.string.reader_toast_err_follow_not_authorized_to_access_blog)
        // 0 can happen when the host name isn't found
        0, HTTP_NOT_FOUND -> resourceProvider.getString(R.string.reader_toast_err_follow_blog_could_not_be_found)
        else -> resourceProvider.getString(R.string.reader_toast_err_unable_to_follow_blog) + " ($statusCode)"
    }

    private fun clearEntry() {
        setEntry("")
        _events.trySend(ReaderSubsUiEvent.HideKeyboard)
    }

    /*
     * saved like the tab and search query, so a half-typed entry survives process death
     */
    private fun setEntry(entry: String) {
        savedStateHandle[KEY_ENTRY] = entry
        _uiState.update { it.copy(entry = entry) }
    }

    private fun loadTags() {
        tagsJob?.cancel()
        tagsJob = launch {
            val tags = repository.getFollowedTags().map { tag ->
                val pendingFollow = pendingTagFollows[tag.tagSlug]
                if (pendingFollow == null) {
                    ReaderSubsTag(tag)
                } else {
                    ReaderSubsTag(tag, isFollowing = pendingFollow, isFollowEnabled = false)
                }
            }
            if (initialTagSlugs == null) {
                initialTagSlugs = tags.map { it.slug }.toSet()
            }
            _uiState.update { it.copy(tags = tags) }
        }
    }

    private fun loadSites() {
        sitesJob?.cancel()
        sitesJob = launch {
            followedSites = repository.getFollowedBlogs()
                .map { blog ->
                    val site = blog.toSubsSite()
                    pendingSiteFollows[site.key]?.let { site.copy(isFollowing = it, isFollowEnabled = false) } ?: site
                }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            applySiteSearch()
        }
    }

    private fun setSiteSearchQuery(query: String?) {
        savedStateHandle[KEY_SITE_SEARCH_QUERY] = query
        _uiState.update { it.copy(siteSearchQuery = query) }
        if (_uiState.value.sites != null) applySiteSearch()
    }

    private fun applySiteSearch() {
        val query = _uiState.value.siteSearchQuery?.lowercase(Locale.getDefault()).orEmpty()
        val sites = if (query.isEmpty()) {
            followedSites
        } else {
            followedSites.filter {
                it.title.lowercase(Locale.getDefault()).contains(query) ||
                        it.subtitle.lowercase(Locale.ROOT).contains(query)
            }
        }
        _uiState.update { it.copy(sites = sites) }
    }

    private fun updateTag(slug: String, transform: (ReaderSubsTag) -> ReaderSubsTag) = _uiState.update { state ->
        state.copy(tags = state.tags?.map { if (it.slug == slug) transform(it) else it })
    }

    private fun updateSite(key: String, transform: (ReaderSubsSite) -> ReaderSubsSite) {
        followedSites = followedSites.map { if (it.key == key) transform(it) else it }
        applySiteSearch()
    }

    private fun requestInitialUpdate() {
        if (!hasRequestedUpdate) {
            hasRequestedUpdate = requestUpdate(EnumSet.of(UpdateTask.TAGS, UpdateTask.FOLLOWED_BLOGS))
        }
    }

    /*
     * returns false if there's no connection to request the update with
     */
    private fun requestUpdate(tasks: EnumSet<UpdateTask>): Boolean {
        val isAvailable = networkUtilsWrapper.isNetworkAvailable()
        if (isAvailable) repository.requestUpdate(tasks)
        return isAvailable
    }

    private fun checkConnection(): Boolean {
        val isAvailable = networkUtilsWrapper.isNetworkAvailable()
        if (!isAvailable) showMessage(R.string.no_network_message)
        return isAvailable
    }

    private fun showMessage(@StringRes messageRes: Int) = showMessage(resourceProvider.getString(messageRes))

    private fun showMessage(message: String) {
        _events.trySend(ReaderSubsUiEvent.ShowMessage(message))
    }

    private fun trackTag(stat: Stat, slug: String) = readerTracker.trackTag(stat, slug, ReaderTracker.SOURCE_SETTINGS)

    /*
     * the tab saved in this instance, else the one the caller asked for
     */
    private fun initialTab(): ReaderSubsTab = savedStateHandle.get<ReaderSubsTab>(KEY_SELECTED_TAB)
        ?: savedStateHandle.get<Int>(ReaderConstants.ARG_SUBS_TAB_POSITION)?.let { ReaderSubsTab.entries.getOrNull(it) }
        ?: ReaderSubsTab.TAGS

    private fun ReaderBlog.toSubsSite(): ReaderSubsSite {
        val host = repository.getHost(url)
        return ReaderSubsSite(
            blogId = blogId,
            feedId = feedId,
            title = name.ifEmpty { host },
            subtitle = when {
                url.isNotEmpty() -> host
                feedUrl.isNotEmpty() -> repository.getHost(feedUrl)
                else -> ""
            },
            iconUrl = imageUrl,
            isFollowing = isFollowing
        )
    }

    companion object {
        private const val KEY_SELECTED_TAB = "selected_tab"
        private const val KEY_SITE_SEARCH_QUERY = "site_search_query"
        private const val KEY_ENTRY = "entry"
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_NOT_FOUND = 404
        private const val MAX_ENTRY_LENGTH = 96
    }
}

/**
 * [tags] and [sites] are null until they've loaded. [siteSearchQuery] is null when the site search is closed.
 */
data class ReaderSubsUiState(
    val selectedTab: ReaderSubsTab,
    val tags: List<ReaderSubsTag>? = null,
    val sites: List<ReaderSubsSite>? = null,
    val siteSearchQuery: String? = null,
    val entry: String = "",
    val isAdding: Boolean = false
)

// the order matches ReaderSubsActivity.TAB_IDX_FOLLOWED_TAGS and TAB_IDX_FOLLOWED_BLOGS
enum class ReaderSubsTab(@StringRes val labelRes: Int) {
    TAGS(R.string.reader_page_followed_tags_text_title),
    SITES(R.string.reader_page_followed_blogs_title)
}

data class ReaderSubsTag(
    val tag: ReaderTag,
    val isFollowing: Boolean = true,
    val isFollowEnabled: Boolean = true
) {
    val slug: String get() = tag.tagSlug
}

data class ReaderSubsSite(
    val blogId: Long,
    val feedId: Long,
    val title: String,
    val subtitle: String,
    val iconUrl: String,
    val isFollowing: Boolean,
    val isFollowEnabled: Boolean = true
) {
    val key: String get() = "$blogId:$feedId"
}

sealed class ReaderSubsUiEvent {
    data class ShowMessage(val message: String) : ReaderSubsUiEvent()
    data class ShowSitePreview(val blogId: Long, val feedId: Long, val isFollowing: Boolean) : ReaderSubsUiEvent()
    data object HideKeyboard : ReaderSubsUiEvent()
}
