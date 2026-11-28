package org.wordpress.android.ui.reader.viewmodels

import android.webkit.URLUtil
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
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.datasets.wrappers.ReaderSearchTableWrapper
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ReaderActionBuilder
import org.wordpress.android.fluxc.model.ReaderSiteModel
import org.wordpress.android.fluxc.store.ReaderStore
import org.wordpress.android.fluxc.store.ReaderStore.OnReaderSitesSearched
import org.wordpress.android.fluxc.store.ReaderStore.ReaderSearchSitesPayload
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.repository.ReaderBlogRepository
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

/**
 * Owns the Reader search screen: the query, the recent-query suggestions and the Sites tab. The Posts
 * tab is the post list fragment, which [ReaderSearchEvent.SubmitPostSearch] drives.
 */
@Suppress("LongParameterList")
@HiltViewModel
class ReaderSearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val dispatcher: Dispatcher,
    // must be injected so the dispatcher has a store to handle the site search action
    @Suppress("unused") private val readerStore: ReaderStore,
    private val searchTable: ReaderSearchTableWrapper,
    private val blogTable: ReaderBlogTableWrapper,
    private val blogRepository: ReaderBlogRepository,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val readerTracker: ReaderTracker,
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableStateFlow(
        ReaderSearchUiState(
            query = savedStateHandle[KEY_QUERY] ?: "",
            submittedQuery = savedStateHandle[KEY_SUBMITTED_QUERY],
            selectedTab = savedStateHandle[KEY_SELECTED_TAB] ?: ReaderSearchTab.POSTS
        )
    )
    val uiState: StateFlow<ReaderSearchUiState> = _uiState.asStateFlow()

    private val _events = Channel<ReaderSearchEvent>(Channel.BUFFERED)
    val events: Flow<ReaderSearchEvent> = _events.receiveAsFlow()

    private var suggestionsJob: Job? = null
    private var urlLookupJob: Job? = null
    private var lastTappedSiteKey: String? = null

    init {
        dispatcher.register(this)
        loadSuggestions()
        // after process death the post list restores its own results, but the sites have to be fetched again
        _uiState.value.submittedQuery?.let {
            searchSites(it, offset = 0)
            lookUpUrl(it)
        }
    }

    override fun onCleared() {
        dispatcher.unregister(this)
    }

    fun onQueryChanged(query: String) {
        setState { it.copy(query = query) }
        // emptying the field ends the search, as it did with the old SearchView
        if (query.isBlank() && _uiState.value.submittedQuery != null) {
            clearSearch()
        }
        loadSuggestions()
    }

    fun onSearchSubmitted() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) return

        setState { it.withoutSites().copy(query = query, submittedQuery = query) }
        launch(bgDispatcher) { searchTable.addOrUpdateQuery(query) }
        readerTracker.trackQuery(AnalyticsTracker.Stat.READER_SEARCH_PERFORMED, query)
        _events.trySend(ReaderSearchEvent.SubmitPostSearch(query))
        searchSites(query, offset = 0)
        lookUpUrl(query)
    }

    fun onSuggestionClicked(query: String) {
        setState { it.copy(query = query) }
        onSearchSubmitted()
    }

    fun onSuggestionDeleteClicked(query: String) {
        launch {
            withContext(bgDispatcher) { searchTable.deleteQuery(query) }
            loadSuggestions()
        }
    }

    fun onClearHistoryConfirmed() {
        readerTracker.track(AnalyticsTracker.Stat.READER_SEARCH_HISTORY_CLEARED)
        launch {
            withContext(bgDispatcher) { searchTable.deleteAllQueries() }
            loadSuggestions()
        }
    }

    fun onTabSelected(tab: ReaderSearchTab) = setState { it.copy(selectedTab = tab) }

    /*
     * re-runs the search once the device is back online, since it likely failed while offline
     */
    fun onConnectionRestored() {
        val state = _uiState.value
        if (state.query == state.submittedQuery) {
            onSearchSubmitted()
        }
    }

    fun onLoadMoreSites() {
        val state = _uiState.value
        val query = state.submittedQuery ?: return
        if (!state.canRequestMoreSites) return
        searchSites(query, offset = state.nextSitesOffset)
    }

    fun onSiteClicked(site: ReaderSearchSite) {
        // a URL the server couldn't find has nothing to preview, though following it may still discover a feed
        if (site.siteId == 0L && site.feedId == 0L) return
        lastTappedSiteKey = site.key
        _events.trySend(ReaderSearchEvent.ShowSitePreview(site))
    }

    /*
     * the user may have changed the follow status of the site they opened, so check it against the db
     */
    fun onResume() {
        val key = lastTappedSiteKey ?: return
        lastTappedSiteKey = null
        val site = _uiState.value.displayedSites.firstOrNull { it.key == key } ?: return
        launch {
            val isFollowing = withContext(bgDispatcher) { blogTable.isSiteFollowed(site.siteId, site.feedId) }
            updateSite(key) { it.copy(isFollowing = isFollowing) }
        }
    }

    fun onSiteFollowClicked(site: ReaderSearchSite) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _events.trySend(ReaderSearchEvent.ShowToast(R.string.no_network_message))
            return
        }

        val isAskingToFollow = !site.isFollowing
        updateSite(site.key) { it.copy(isFollowing = isAskingToFollow, isFollowEnabled = false) }
        launch {
            val followUrl = site.followUrl
            val succeeded = if (followUrl != null && isAskingToFollow) {
                followByUrl(site, followUrl)
            } else {
                blogRepository.followFeed(site.feedId, isAskingToFollow, ReaderTracker.SOURCE_SEARCH)
            }
            updateSite(site.key) {
                it.copy(isFollowing = if (succeeded) isAskingToFollow else !isAskingToFollow, isFollowEnabled = true)
            }
            if (!succeeded) {
                _events.trySend(
                    ReaderSearchEvent.ShowToast(
                        if (isAskingToFollow) {
                            R.string.reader_toast_err_unable_to_follow_blog
                        } else {
                            R.string.reader_toast_err_unable_to_unfollow_blog
                        }
                    )
                )
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReaderSitesSearched(event: OnReaderSitesSearched) {
        val state = _uiState.value
        if (event.searchTerm != state.submittedQuery) return
        // a later page that doesn't follow the current list belongs to a search that has since restarted
        if (event.offset > 0 && event.offset != state.nextSitesOffset) return

        setState { state ->
            if (event.isError) {
                state.copy(
                    isLoadingSites = false,
                    isLoadingMoreSites = false,
                    // stop paging, or scrolling to the end would retry the failed page in a loop
                    canLoadMoreSites = false,
                    sitesRequestFailed = state.sites.isEmpty()
                )
            } else {
                val newSites = event.sites.map { it.toSearchSite() }
                state.copy(
                    sites = (if (event.offset == 0) newSites else state.sites + newSites).distinctBy { it.key },
                    // the server's offset, since dropping duplicates can leave fewer rows than it returned
                    nextSitesOffset = event.offset + event.sites.size,
                    isLoadingSites = false,
                    isLoadingMoreSites = false,
                    canLoadMoreSites = event.canLoadMore,
                    sitesRequestFailed = false
                )
            }
        }
    }

    private fun searchSites(query: String, offset: Int) {
        setState {
            it.copy(isLoadingSites = offset == 0, isLoadingMoreSites = offset > 0, sitesRequestFailed = false)
        }
        val payload = ReaderSearchSitesPayload(
            query,
            ReaderConstants.READER_MAX_SEARCH_RESULTS_TO_REQUEST,
            offset,
            false
        )
        dispatcher.dispatch(ReaderActionBuilder.newReaderSearchSitesAction(payload))
    }

    /*
     * A query that looks like a site or feed address gets a row of its own above the search results, so readers can
     * subscribe to any site or RSS/Atom feed by pasting its URL, which a keyword search rarely finds
     */
    private fun lookUpUrl(query: String) {
        urlLookupJob?.cancel()
        val url = searchUrl(query) ?: return
        setState { it.copy(selectedTab = ReaderSearchTab.SITES, isLookingUpUrl = true) }
        urlLookupJob = launch {
            val blog = blogRepository.lookupFeedUrl(url)
            if (_uiState.value.submittedQuery != query) return@launch
            setState { it.copy(urlSite = blog.toUrlSite(url), isLookingUpUrl = false) }
        }
    }

    /*
     * follows by URL, which discovers the feed if the lookup didn't, then picks up the ids the follow saved so the
     * row can be unfollowed and previewed
     */
    private suspend fun followByUrl(site: ReaderSearchSite, url: String): Boolean {
        val blog = if (site.siteId != 0L || site.feedId != 0L) blogRepository.lookupFeedUrl(url) else null
        if (!blogRepository.followFeedUrl(url, blog, ReaderTracker.SOURCE_SEARCH)) return false
        // the ids can change the row's key, so this update also finishes the follow
        blogRepository.lookupFeedUrl(url)?.let { followed ->
            updateSite(site.key) { followed.toUrlSite(url).copy(isFollowing = true) }
        }
        return true
    }

    private fun ReaderBlog?.toUrlSite(url: String) = ReaderSearchSite(
        siteId = this?.blogId ?: 0,
        feedId = this?.feedId ?: 0,
        title = this?.takeIf { it.hasName() }?.name ?: UrlUtils.getHost(url),
        url = this?.takeIf { it.hasUrl() }?.url ?: url,
        iconUrl = this?.imageUrl,
        isFollowing = this?.isFollowing == true,
        followUrl = url
    )

    private fun clearSearch() {
        setState { it.withoutSites().copy(submittedQuery = null, selectedTab = ReaderSearchTab.POSTS) }
        _events.trySend(ReaderSearchEvent.ClearPostSearch)
    }

    private fun loadSuggestions() {
        val prefix = _uiState.value.query.trim().ifEmpty { null }
        suggestionsJob?.cancel()
        suggestionsJob = launch {
            val max = if (prefix == null) MAX_SUGGESTIONS_WHEN_EMPTY else MAX_SUGGESTIONS
            val suggestions = withContext(bgDispatcher) { searchTable.getQueries(prefix, max) }
            setState { it.copy(suggestions = suggestions, suggestionsQuery = prefix.orEmpty()) }
        }
    }

    private fun updateSite(key: String, transform: (ReaderSearchSite) -> ReaderSearchSite) = setState { state ->
        state.copy(
            sites = state.sites.map { if (it.key == key) transform(it) else it },
            urlSite = state.urlSite?.let { if (it.key == key) transform(it) else it }
        )
    }

    private fun setState(transform: (ReaderSearchUiState) -> ReaderSearchUiState) {
        val state = _uiState.updateAndGet(transform)
        savedStateHandle[KEY_QUERY] = state.query
        savedStateHandle[KEY_SUBMITTED_QUERY] = state.submittedQuery
        savedStateHandle[KEY_SELECTED_TAB] = state.selectedTab
    }

    private fun ReaderSearchUiState.withoutSites() = copy(
        sites = emptyList(),
        urlSite = null,
        isLookingUpUrl = false,
        isLoadingSites = false,
        isLoadingMoreSites = false,
        canLoadMoreSites = false,
        nextSitesOffset = 0,
        sitesRequestFailed = false
    )

    private fun ReaderSiteModel.toSearchSite() = ReaderSearchSite(
        siteId = siteId,
        feedId = feedId,
        title = title,
        url = url,
        iconUrl = iconUrl,
        isFollowing = isFollowing
    )

    companion object {
        private const val KEY_QUERY = "query"
        private const val KEY_SUBMITTED_QUERY = "submitted_query"
        private const val KEY_SELECTED_TAB = "selected_tab"
        private const val MAX_SUGGESTIONS = 5
        private const val MAX_SUGGESTIONS_WHEN_EMPTY = 10

        /** The query as a site or feed address, or null if it doesn't look like one */
        internal fun searchUrl(query: String): String? {
            if (query.any { it.isWhitespace() } || !query.contains('.')) return null
            val url = UrlUtils.normalizeUrl(if (query.contains("://")) query else "https://$query")
            val host = UrlUtils.getHost(url)
            // a TLD after the last dot, so "3.5" or "e.g." aren't addresses
            val tld = host.substringAfterLast('.', "")
            return url.takeIf { URLUtil.isNetworkUrl(it) && tld.length >= 2 && tld.all { c -> c.isLetter() } }
        }
    }
}

data class ReaderSearchUiState(
    val query: String = "",
    val submittedQuery: String? = null,
    val suggestions: List<String> = emptyList(),
    // the trimmed query the suggestions were loaded for, since they load after the query changes
    val suggestionsQuery: String = "",
    val selectedTab: ReaderSearchTab = ReaderSearchTab.POSTS,
    val sites: List<ReaderSearchSite> = emptyList(),
    val isLoadingSites: Boolean = false,
    val isLoadingMoreSites: Boolean = false,
    val canLoadMoreSites: Boolean = false,
    val nextSitesOffset: Int = 0,
    val sitesRequestFailed: Boolean = false,
    // the site or feed at the address the query looks like, shown above the search results
    val urlSite: ReaderSearchSite? = null,
    val isLookingUpUrl: Boolean = false,
) {
    val displayedSites: List<ReaderSearchSite>
        get() = urlSite?.let { url -> listOf(url) + sites.filterNot { it.key == url.key } } ?: sites

    /** Waiting on everything the Sites tab could show */
    val isLoadingDisplayedSites: Boolean
        get() = (isLoadingSites || isLookingUpUrl) && displayedSites.isEmpty()

    /*
     * Results stay up while the user edits the query, unless there are suggestions to offer for it
     */
    val content: ReaderSearchContent
        get() = when {
            submittedQuery != null && (query.trim() == submittedQuery || !hasSuggestionsForQuery) ->
                ReaderSearchContent.RESULTS
            suggestions.isNotEmpty() -> ReaderSearchContent.SUGGESTIONS
            else -> ReaderSearchContent.EXPLAINER
        }

    val canRequestMoreSites: Boolean
        get() = canLoadMoreSites && !isLoadingSites && !isLoadingMoreSites

    private val hasSuggestionsForQuery: Boolean
        get() = suggestions.isNotEmpty() && suggestionsQuery == query.trim()
}

enum class ReaderSearchContent { EXPLAINER, SUGGESTIONS, RESULTS }

enum class ReaderSearchTab(@StringRes val labelRes: Int) {
    POSTS(R.string.posts),
    SITES(R.string.sites)
}

data class ReaderSearchSite(
    val siteId: Long,
    val feedId: Long,
    val title: String?,
    val url: String?,
    val iconUrl: String?,
    val isFollowing: Boolean,
    val isFollowEnabled: Boolean = true,
    // set for the row found from a URL query, which follows by URL so the server can discover the feed
    val followUrl: String? = null,
) {
    // feed and site ids are separate id spaces, so prefix them to keep LazyColumn keys unique
    val key: String
        get() = if (feedId != 0L) "feed:$feedId" else "site:$siteId"
}

sealed class ReaderSearchEvent {
    data class SubmitPostSearch(val query: String) : ReaderSearchEvent()
    data object ClearPostSearch : ReaderSearchEvent()
    data class ShowSitePreview(val site: ReaderSearchSite) : ReaderSearchEvent()
    data class ShowToast(@StringRes val messageRes: Int) : ReaderSearchEvent()
}
