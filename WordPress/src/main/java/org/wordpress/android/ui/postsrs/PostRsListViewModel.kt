package org.wordpress.android.ui.postsrs

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.postsrs.data.WpSelfHostedServiceProvider
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import rs.wordpress.cache.kotlin.ObservableMetadataCollection
import rs.wordpress.cache.kotlin.getObservablePostMetadataCollectionWithEditContext
import rs.wordpress.cache.kotlin.hasMorePages
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostStatus
import uniffi.wp_api.WpApiParamPostsOrderBy
import uniffi.wp_mobile.PostListFilter
import uniffi.wp_mobile_cache.ListState
import javax.inject.Inject

@HiltViewModel
class PostRsListViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val serviceProvider: WpSelfHostedServiceProvider,
    private val resourceProvider: ResourceProvider,
    private val postStore: PostStore,
    private val blazeFeatureUtils: BlazeFeatureUtils,
) : ViewModel() {
    private val _tabStates =
        MutableStateFlow<Map<PostRsListTab, PostTabUiState>>(emptyMap())
    val tabStates: StateFlow<Map<PostRsListTab, PostTabUiState>> =
        _tabStates.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> =
        _isSearchActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private var activeSearchTab = PostRsListTab.PUBLISHED

    private val collections =
        mutableMapOf<PostRsListTab, ObservableMetadataCollection>()
    private val initializingTabs = mutableSetOf<PostRsListTab>()
    private val userRefreshingTabs = mutableSetOf<PostRsListTab>()

    private val _events = Channel<PostRsListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _trashConfirmPostId = MutableStateFlow<Long?>(null)
    val trashConfirmPostId: StateFlow<Long?> =
        _trashConfirmPostId.asStateFlow()

    private val _deleteConfirmPostId = MutableStateFlow<Long?>(null)
    val deleteConfirmPostId: StateFlow<Long?> =
        _deleteConfirmPostId.asStateFlow()

    init {
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            _searchQuery
                .debounce(SEARCH_DEBOUNCE_MS)
                .filter { it.length >= MIN_SEARCH_QUERY_LENGTH }
                .collect {
                    clearCollections()
                    _tabStates.value = PostRsListTab.entries
                        .associateWith {
                            PostTabUiState(isLoading = true)
                        }
                    initTab(activeSearchTab)
                }
        }
    }

    /**
     * Looks up a post in the local FluxC database and emits an
     * [PostRsListEvent] to open the editor or show an error.
     * This FluxC dependency will be removed once the editor
     * supports loading posts via wordpress-rs.
     */
    @MainThread
    fun openPost(remotePostId: Long) {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            _events.trySend(
                PostRsListEvent.ShowError(R.string.blog_not_found)
            )
            return
        }
        val post = postStore.getPostByRemotePostId(
            remotePostId, site
        )
        if (post == null) {
            _events.trySend(
                PostRsListEvent.ShowError(R.string.post_not_found)
            )
            return
        }
        _events.trySend(
            PostRsListEvent.EditPost(site, post)
        )
    }

    /** Emits a [PostRsListEvent.CreatePost] for the selected site. */
    @MainThread
    fun createNewPost() {
        val site = selectedSiteRepository.getSelectedSite()
            ?: return
        _events.trySend(PostRsListEvent.CreatePost(site))
    }

    /**
     * Clears all cached collections and tab states so the list
     * appears empty while the user types a search query.
     */
    @MainThread
    fun onSearchOpen() {
        _isSearchActive.value = true
        clearCollections()
    }

    /**
     * Updates the search query. Non-blank queries are debounced
     * before triggering an API call. Blank queries immediately
     * clear results so the idle state appears without delay.
     */
    @MainThread
    fun onSearchQueryChanged(
        query: String,
        activeTab: PostRsListTab
    ) {
        activeSearchTab = activeTab
        _searchQuery.value = query
        if (query.isBlank()) {
            clearCollections()
        }
    }

    /**
     * Closes search mode: clears the query, tears down all
     * collections, and immediately re-initializes [activeTab]
     * so the normal tab content appears without debounce delay.
     */
    @MainThread
    fun onSearchClose(activeTab: PostRsListTab) {
        _isSearchActive.value = false
        _searchQuery.value = ""
        clearCollections()
        initTab(activeTab)
    }

    /** Routes a menu action tap to the appropriate event or dialog. */
    @MainThread
    @Suppress("LongMethod", "ReturnCount")
    fun onPostMenuAction(
        remotePostId: Long,
        action: PostRsMenuAction
    ) {
        val site =
            selectedSiteRepository.getSelectedSite() ?: return
        val post = findPost(remotePostId)

        when (action) {
            PostRsMenuAction.VIEW -> {
                val url = post?.link ?: return
                _events.trySend(PostRsListEvent.ViewPost(url))
            }
            PostRsMenuAction.READ -> _events.trySend(
                PostRsListEvent.ReadPost(
                    site.siteId, remotePostId
                )
            )
            PostRsMenuAction.SHARE -> {
                val url = post?.link ?: return
                _events.trySend(
                    PostRsListEvent.SharePost(
                        url, post.title
                    )
                )
            }
            PostRsMenuAction.BLAZE -> {
                val fluxcPost =
                    postStore.getPostByRemotePostId(
                        remotePostId, site
                    ) ?: return
                _events.trySend(
                    PostRsListEvent.PromoteWithBlaze(
                        site, fluxcPost
                    )
                )
            }
            PostRsMenuAction.STATS -> _events.trySend(
                PostRsListEvent.ViewStats(
                    site = site,
                    postId = remotePostId,
                    title = post?.title ?: "",
                    url = post?.link ?: ""
                )
            )
            PostRsMenuAction.COMMENTS -> _events.trySend(
                PostRsListEvent.ViewComments(
                    site.siteId, remotePostId
                )
            )
            PostRsMenuAction.TRASH ->
                _trashConfirmPostId.value = remotePostId
            PostRsMenuAction.DELETE_PERMANENTLY ->
                _deleteConfirmPostId.value = remotePostId
            PostRsMenuAction.PUBLISH ->
                sendNotImplemented()
            PostRsMenuAction.MOVE_TO_DRAFT ->
                sendNotImplemented()
            PostRsMenuAction.DUPLICATE ->
                sendNotImplemented()
        }
    }

    @MainThread
    fun onConfirmTrash() {
        _trashConfirmPostId.value = null
        sendNotImplemented()
    }

    @MainThread
    fun onDismissTrash() {
        _trashConfirmPostId.value = null
    }

    @MainThread
    fun onConfirmDelete() {
        _deleteConfirmPostId.value = null
        sendNotImplemented()
    }

    @MainThread
    fun onDismissDelete() {
        _deleteConfirmPostId.value = null
    }

    private fun sendNotImplemented() {
        _events.trySend(
            PostRsListEvent.ShowError(
                R.string.post_rs_not_implemented_yet
            )
        )
    }

    private fun findPost(remotePostId: Long): PostRsUiModel? {
        return _tabStates.value.values
            .flatMap { it.posts }
            .firstOrNull { it.remotePostId == remotePostId }
    }

    private fun getMenuActions(
        tab: PostRsListTab,
        hasPassword: Boolean
    ): List<PostRsMenuAction> {
        val site = selectedSiteRepository.getSelectedSite()
        return buildList {
            when (tab) {
                PostRsListTab.PUBLISHED -> {
                    add(PostRsMenuAction.VIEW)
                    add(PostRsMenuAction.READ)
                    add(PostRsMenuAction.MOVE_TO_DRAFT)
                    add(PostRsMenuAction.DUPLICATE)
                    add(PostRsMenuAction.SHARE)
                    if (site != null &&
                        !hasPassword &&
                        blazeFeatureUtils
                            .isSiteBlazeEligible(site)
                    ) {
                        add(PostRsMenuAction.BLAZE)
                    }
                    add(PostRsMenuAction.STATS)
                    add(PostRsMenuAction.COMMENTS)
                    add(PostRsMenuAction.TRASH)
                }
                PostRsListTab.DRAFTS -> {
                    add(PostRsMenuAction.VIEW)
                    add(PostRsMenuAction.READ)
                    add(PostRsMenuAction.PUBLISH)
                    add(PostRsMenuAction.DUPLICATE)
                    add(PostRsMenuAction.SHARE)
                    add(PostRsMenuAction.TRASH)
                }
                PostRsListTab.SCHEDULED -> {
                    add(PostRsMenuAction.VIEW)
                    add(PostRsMenuAction.READ)
                    add(PostRsMenuAction.SHARE)
                    add(PostRsMenuAction.TRASH)
                }
                PostRsListTab.TRASHED -> {
                    add(PostRsMenuAction.MOVE_TO_DRAFT)
                    add(PostRsMenuAction.DELETE_PERMANENTLY)
                }
            }
        }
    }

    /**
     * Maps a [PostStatus] to the corresponding [PostRsListTab].
     * Used during search when posts from all statuses are
     * shown together and menu actions must be per-post.
     */
    private fun tabForStatus(
        status: PostStatus?
    ): PostRsListTab {
        if (status == null) return PostRsListTab.PUBLISHED
        return PostRsListTab.entries.firstOrNull { tab ->
            tab.statuses.any { it == status }
        } ?: PostRsListTab.PUBLISHED
    }

    private fun clearCollections() {
        collections.values.forEach { it.close() }
        collections.clear()
        initializingTabs.clear()
        userRefreshingTabs.clear()
        _tabStates.value = emptyMap()
    }

    /**
     * Initializes the observable collection for [tab] if it hasn't been
     * created yet. Creates the service, registers observers, then
     * triggers the first refresh.
     */
    @MainThread
    @Suppress("ReturnCount")
    fun initTab(tab: PostRsListTab) {
        if (collections.containsKey(tab) || initializingTabs.contains(tab)) {
            return
        }

        val site = selectedSiteRepository.getSelectedSite() ?: run {
            updateTabUiState(tab) {
                PostTabUiState(
                    error = resourceProvider.getString(
                        R.string.stats_todays_stats_no_site_selected
                    )
                )
            }
            return
        }

        initializingTabs.add(tab)

        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val collection = createCollection(site, tab)
                collections[tab] = collection
                initializingTabs.remove(tab)
                registerObservers(tab, collection)
                refreshTab(tab)
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to init RS post list tab",
                    e
                )
                initializingTabs.remove(tab)
                updateTabUiState(tab) {
                    PostTabUiState(
                        error = e.message
                            ?: resourceProvider.getString(
                                R.string.error_generic
                            )
                    )
                }
            }
        }
    }

    private suspend fun createCollection(
        site: SiteModel,
        tab: PostRsListTab
    ): ObservableMetadataCollection = withContext(Dispatchers.IO) {
        val service = serviceProvider.getService(site)
        val query = _searchQuery.value
        val filter = PostListFilter(
            status = if (query.isNotBlank()) {
                ALL_STATUSES
            } else {
                tab.statuses
            },
            order = tab.order,
            orderby = WpApiParamPostsOrderBy.DATE,
            search = query.ifBlank { null }
        )
        service.posts()
            .getObservablePostMetadataCollectionWithEditContext(
                endpointType = PostEndpointType.Posts,
                filter = filter,
                perPage = PAGE_SIZE.toUInt()
            )
    }

    private fun registerObservers(
        tab: PostRsListTab,
        collection: ObservableMetadataCollection
    ) {
        collection.addDataObserver {
            viewModelScope.launch { loadItemsForTab(tab) }
        }
        collection.addListInfoObserver {
            viewModelScope.launch { updateListInfoForTab(tab) }
        }
    }

    /**
     * Triggers a refresh for the given tab's collection. The PTR
     * indicator is only shown when [isUserRefresh] is true (i.e.
     * the user explicitly pulled to refresh).
     */
    @MainThread
    fun refreshTab(
        tab: PostRsListTab,
        isUserRefresh: Boolean = false
    ) {
        val collection = collections[tab] ?: return

        if (isUserRefresh) {
            userRefreshingTabs.add(tab)
            updateTabUiState(tab) {
                copy(isRefreshing = true, error = null)
            }
        } else {
            updateTabUiState(tab) { copy(error = null) }
        }

        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                withContext(Dispatchers.IO) {
                    collection.refresh()
                }
                loadItemsForTab(tab)
                updateListInfoForTab(tab)
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to refresh tab $tab",
                    e
                )
                userRefreshingTabs.remove(tab)
                updateTabUiState(tab) {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message
                            ?: resourceProvider.getString(
                                R.string.error_generic
                            )
                    )
                }
            }
        }
    }

    /** Loads the next page of posts for [tab] if not already loading. */
    @MainThread
    fun loadMorePosts(tab: PostRsListTab) {
        val collection = collections[tab] ?: return
        val current = getTabUiState(tab)
        if (current.isLoadingMore || !current.canLoadMore) {
            return
        }

        updateTabUiState(tab) { copy(isLoadingMore = true) }

        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                withContext(Dispatchers.IO) {
                    collection.loadNextPage()
                }
                loadItemsForTab(tab)
                updateListInfoForTab(tab)
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to load more for tab $tab",
                    e
                )
                updateTabUiState(tab) {
                    copy(isLoadingMore = false)
                }
            }
        }
    }

    /**
     * Reads cached items from the collection and maps them to
     * [PostRsUiModel] instances for display.
     */
    private suspend fun loadItemsForTab(tab: PostRsListTab) {
        val collection = collections[tab] ?: return

        @Suppress("TooGenericExceptionCaught")
        try {
            val isSearch = _searchQuery.value.isNotBlank()
            val items = withContext(Dispatchers.IO) {
                collection.loadItems().map { item ->
                    item.state.toUiModel(
                        item.id,
                        showStatus = isSearch
                    )
                }
            }
            val uiModels = items.map { model ->
                val effectiveTab = if (isSearch) {
                    tabForStatus(model.status)
                } else {
                    tab
                }
                model.copy(
                    actions = getMenuActions(
                        effectiveTab, model.hasPassword
                    )
                )
            }
            updateTabUiState(tab) {
                copy(
                    posts = uiModels,
                    isLoading = false,
                    error = null
                )
            }
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.POSTS,
                "Failed to load items for tab $tab",
                e
            )
        }
    }

    /**
     * Reads pagination and sync state from the collection's list info
     * and updates the tab's UI state accordingly.
     */
    private suspend fun updateListInfoForTab(tab: PostRsListTab) {
        val collection = collections[tab] ?: return

        val listInfo = withContext(Dispatchers.IO) {
            collection.listInfo()
        }
        val morePages = listInfo?.hasMorePages ?: false
        val fetchingFirstPage =
            listInfo?.state == ListState.FETCHING_FIRST_PAGE
        val isUserRefresh = userRefreshingTabs.contains(tab)

        if (!fetchingFirstPage) {
            userRefreshingTabs.remove(tab)
        }

        updateTabUiState(tab) {
            copy(
                isRefreshing = isUserRefresh && fetchingFirstPage,
                isLoadingMore =
                    listInfo?.state == ListState.FETCHING_NEXT_PAGE,
                canLoadMore = morePages,
                error = if (listInfo?.state == ListState.ERROR) {
                    listInfo.errorMessage
                        ?: resourceProvider.getString(
                            R.string.error_generic
                        )
                } else {
                    null
                }
            )
        }
    }

    /** Returns the current UI state for [tab], or a default loading state. */
    private fun getTabUiState(tab: PostRsListTab): PostTabUiState {
        return _tabStates.value[tab]
            ?: PostTabUiState(isLoading = true)
    }

    /** Updates the UI state for [tab] by applying [update] to the current state. */
    private fun updateTabUiState(
        tab: PostRsListTab,
        update: PostTabUiState.() -> PostTabUiState
    ) {
        val current = getTabUiState(tab)
        _tabStates.value += (tab to current.update())
    }

    override fun onCleared() {
        super.onCleared()
        collections.values.forEach { it.close() }
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val SEARCH_DEBOUNCE_MS = 250L
        internal const val MIN_SEARCH_QUERY_LENGTH = 3
        private val ALL_STATUSES =
            PostRsListTab.entries
                .flatMap { it.statuses }.distinct()
    }
}
