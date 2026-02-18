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
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.postsrs.data.PostRsRestClient
import org.wordpress.android.ui.postsrs.data.PostRsRestClient.PostActionResult
import org.wordpress.android.ui.postsrs.data.WpSelfHostedServiceProvider
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SiteUtils
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
@Suppress("LargeClass")
class PostRsListViewModel @Inject constructor(
    selectedSiteRepository: SelectedSiteRepository,
    private val serviceProvider: WpSelfHostedServiceProvider,
    private val restClient: PostRsRestClient,
    private val resourceProvider: ResourceProvider,
    private val postStore: PostStore,
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
) : ViewModel() {
    private val _tabStates = MutableStateFlow<Map<PostRsListTab, PostTabUiState>>(emptyMap())
    val tabStates: StateFlow<Map<PostRsListTab, PostTabUiState>> = _tabStates.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private var activeSearchTab = PostRsListTab.PUBLISHED

    private val collections = mutableMapOf<PostRsListTab, ObservableMetadataCollection>()
    private val initializingTabs = mutableSetOf<PostRsListTab>()
    private val userRefreshingTabs = mutableSetOf<PostRsListTab>()

    private val _events = Channel<PostRsListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _site: SiteModel? = selectedSiteRepository.getSelectedSite()
    private val site: SiteModel
        get() = requireNotNull(_site) { "No selected site — Activity should have finished" }

    init {
        if (_site == null) {
            _events.trySend(PostRsListEvent.ShowToast(R.string.blog_not_found))
            _events.trySend(PostRsListEvent.Finish)
        } else {
            @OptIn(FlowPreview::class)
            viewModelScope.launch {
                _searchQuery
                    .debounce(SEARCH_DEBOUNCE_MS)
                    .filter { it.length >= MIN_SEARCH_QUERY_LENGTH }
                    .collect {
                        clearCollections()
                        _tabStates.value = PostRsListTab.entries.associateWith {
                            PostTabUiState(isLoading = true)
                        }
                        initTab(activeSearchTab)
                    }
            }
        }
    }

    /**
     * Looks up a post in the local FluxC database and emits
     * an [PostRsListEvent.EditPost] to open the editor.
     */
    @MainThread
    fun openPost(remotePostId: Long) {
        val post = getFluxCPost(remotePostId) ?: return
        _events.trySend(PostRsListEvent.EditPost(site, post))
    }

    /** Emits a [PostRsListEvent.CreatePost] for the selected site. */
    @MainThread
    fun createNewPost() {
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
     * Updates the search query. Non-blank queries are debounced before triggering an API call.
     * Blank queries immediately clear results so the idle state appears without delay.
     */
    @MainThread
    fun onSearchQueryChanged(query: String, activeTab: PostRsListTab) {
        activeSearchTab = activeTab
        _searchQuery.value = query
        if (query.isBlank()) clearCollections()
    }

    /**
     * Closes search mode: clears the query, tears down all collections, and immediately
     * re-initializes [activeTab] so the normal tab content appears without debounce delay.
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
    fun onPostMenuAction(remotePostId: Long, action: PostRsMenuAction) {
        val post = findPost(remotePostId)

        when (action) {
            PostRsMenuAction.VIEW -> {
                val url = post?.link
                if (url == null) {
                    AppLog.w(AppLog.T.POSTS, "No link for post $remotePostId")
                    return
                }
                _events.trySend(PostRsListEvent.ViewPost(url))
            }
            PostRsMenuAction.READ ->
                _events.trySend(PostRsListEvent.ReadPost(site.siteId, remotePostId))
            PostRsMenuAction.SHARE -> {
                val url = post?.link
                if (url == null) {
                    AppLog.w(AppLog.T.POSTS, "No link for post $remotePostId")
                    return
                }
                _events.trySend(PostRsListEvent.SharePost(url, post.title))
            }
            PostRsMenuAction.BLAZE -> {
                val post = getFluxCPost(remotePostId) ?: return
                _events.trySend(PostRsListEvent.PromoteWithBlaze(site, post))
            }
            PostRsMenuAction.STATS -> _events.trySend(
                PostRsListEvent.ViewStats(
                    site = site, postId = remotePostId,
                    title = post?.title ?: "", url = post?.link ?: ""
                )
            )
            PostRsMenuAction.COMMENTS ->
                _events.trySend(PostRsListEvent.ViewComments(site.siteId, remotePostId))
            PostRsMenuAction.TRASH ->
                _pendingConfirmation.value = PendingConfirmation.Trash(remotePostId)
            PostRsMenuAction.DELETE_PERMANENTLY ->
                _pendingConfirmation.value = PendingConfirmation.Delete(remotePostId)
            PostRsMenuAction.PUBLISH -> publishPost(site, remotePostId)
            PostRsMenuAction.MOVE_TO_DRAFT -> moveToDraft(site, remotePostId)
            PostRsMenuAction.DUPLICATE ->
                _events.trySend(PostRsListEvent.ShowToast(R.string.post_rs_not_implemented_yet))
        }
    }

    @MainThread
    fun onConfirmPendingAction() {
        when (val confirmation = _pendingConfirmation.value) {
            is PendingConfirmation.Trash -> trashPost(site, confirmation.postId)
            is PendingConfirmation.Delete -> deletePost(site, confirmation.postId)
            null -> Unit
        }
        _pendingConfirmation.value = null
    }

    @MainThread
    fun onDismissPendingAction() {
        _pendingConfirmation.value = null
    }

    private fun trashPost(site: SiteModel, postId: Long) {
        if (!checkNetwork()) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { restClient.trashPost(site, postId) }
            handleActionResult(result, postId, R.string.post_rs_trashed, R.string.post_rs_error_trash)
        }
    }

    private fun deletePost(site: SiteModel, postId: Long) {
        if (!checkNetwork()) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { restClient.deletePost(site, postId) }
            handleActionResult(result, postId, R.string.post_rs_deleted, R.string.post_rs_error_delete)
        }
    }

    private fun publishPost(site: SiteModel, postId: Long) {
        if (!checkNetwork()) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                restClient.updatePostStatus(site, postId, PostStatus.Publish)
            }
            handleActionResult(result, postId, R.string.post_rs_published, R.string.post_rs_error_update_status)
        }
    }

    private fun moveToDraft(site: SiteModel, postId: Long) {
        if (!checkNetwork()) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                restClient.updatePostStatus(site, postId, PostStatus.Draft)
            }
            handleActionResult(result, postId, R.string.post_rs_moved_to_draft, R.string.post_rs_error_update_status)
        }
    }

    private fun checkNetwork(): Boolean {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _events.trySend(PostRsListEvent.ShowToast(R.string.no_network_message))
            return false
        }
        return true
    }

    private fun handleActionResult(
        result: PostActionResult,
        postId: Long,
        successResId: Int,
        errorResId: Int
    ) {
        when (result) {
            is PostActionResult.Success -> {
                removePostFromState(postId)
                _events.trySend(PostRsListEvent.ShowToast(successResId))
                refreshAllTabs()
            }
            is PostActionResult.Error -> {
                AppLog.e(AppLog.T.POSTS, "Post action failed: ${result.message}")
                _events.trySend(PostRsListEvent.ShowToast(errorResId))
            }
        }
    }

    private fun refreshAllTabs() {
        collections.keys.toList().forEach { tab -> refreshTab(tab) }
    }

    private fun removePostFromState(postId: Long) {
        _tabStates.value = _tabStates.value.mapValues { (_, state) ->
            val filtered = state.posts.filter { it.remotePostId != postId }
            if (filtered.size != state.posts.size) state.copy(posts = filtered) else state
        }
    }

    private fun findPost(remotePostId: Long): PostRsUiModel? {
        for (state in _tabStates.value.values) {
            for (post in state.posts) {
                if (post.remotePostId == remotePostId) return post
            }
        }
        return null
    }

    /**
     * Looks up a post in the local FluxC database and shows a toast if not found.
     * This FluxC dependency is temporary and will be removed once the editor
     * supports loading posts via wordpress-rs.
     */
    private fun getFluxCPost(remotePostId: Long): PostModel? {
        val post = postStore.getPostByRemotePostId(remotePostId, site)
        if (post == null) {
            _events.trySend(PostRsListEvent.ShowToast(R.string.post_not_found))
        }
        return post
    }

    private fun getMenuActions(
        tab: PostRsListTab,
        hasPassword: Boolean,
        commentsOpen: Boolean
    ): List<PostRsMenuAction> = buildList {
        when (tab) {
            PostRsListTab.PUBLISHED -> {
                add(PostRsMenuAction.VIEW)
                add(PostRsMenuAction.READ)
                add(PostRsMenuAction.MOVE_TO_DRAFT)
                add(PostRsMenuAction.DUPLICATE)
                add(PostRsMenuAction.SHARE)
                if (!hasPassword && blazeFeatureUtils.isSiteBlazeEligible(site)) {
                    add(PostRsMenuAction.BLAZE)
                }
                if (SiteUtils.isAccessedViaWPComRest(site) && site.hasCapabilityViewStats) {
                    add(PostRsMenuAction.STATS)
                }
                if (commentsOpen) add(PostRsMenuAction.COMMENTS)
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

    /**
     * Maps a [PostStatus] to the corresponding [PostRsListTab]. Used during search when
     * posts from all statuses are shown together and menu actions must be per-post.
     */
    private fun tabForStatus(status: PostStatus?): PostRsListTab {
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
     * Initializes the observable collection for [tab] if it hasn't been created yet.
     * Creates the service, registers observers, then triggers the first refresh.
     */
    @MainThread
    fun initTab(tab: PostRsListTab) {
        if (collections.containsKey(tab) || initializingTabs.contains(tab)) return

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
                AppLog.e(AppLog.T.POSTS, "Failed to init RS post list tab", e)
                initializingTabs.remove(tab)
                updateTabUiState(tab) {
                    PostTabUiState(
                        error = e.message ?: resourceProvider.getString(R.string.error_generic)
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
            status = if (query.isNotBlank()) ALL_STATUSES else tab.statuses,
            order = tab.order,
            orderby = WpApiParamPostsOrderBy.DATE,
            search = query.ifBlank { null }
        )
        service.posts().getObservablePostMetadataCollectionWithEditContext(
            endpointType = PostEndpointType.Posts,
            filter = filter,
            perPage = PAGE_SIZE.toUInt()
        )
    }

    private fun registerObservers(tab: PostRsListTab, collection: ObservableMetadataCollection) {
        collection.addDataObserver {
            viewModelScope.launch { loadItemsForTab(tab) }
        }
        collection.addListInfoObserver {
            viewModelScope.launch { updateListInfoForTab(tab) }
        }
    }

    /**
     * Triggers a refresh for the given tab's collection. The PTR indicator is only shown
     * when [isUserRefresh] is true (i.e. the user explicitly pulled to refresh).
     */
    @MainThread
    fun refreshTab(tab: PostRsListTab, isUserRefresh: Boolean = false) {
        val collection = collections[tab] ?: return

        if (isUserRefresh) {
            userRefreshingTabs.add(tab)
            updateTabUiState(tab) { copy(isRefreshing = true, error = null) }
        } else {
            updateTabUiState(tab) { copy(error = null) }
        }

        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                withContext(Dispatchers.IO) { collection.refresh() }
                loadItemsForTab(tab)
                updateListInfoForTab(tab)
            } catch (e: Exception) {
                AppLog.e(AppLog.T.POSTS, "Failed to refresh tab $tab", e)
                userRefreshingTabs.remove(tab)
                updateTabUiState(tab) {
                    copy(
                        isLoading = false, isRefreshing = false,
                        error = e.message ?: resourceProvider.getString(R.string.error_generic)
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
        if (current.isLoadingMore || !current.canLoadMore) return

        updateTabUiState(tab) { copy(isLoadingMore = true) }

        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                withContext(Dispatchers.IO) { collection.loadNextPage() }
                loadItemsForTab(tab)
                updateListInfoForTab(tab)
            } catch (e: Exception) {
                AppLog.e(AppLog.T.POSTS, "Failed to load more for tab $tab", e)
                updateTabUiState(tab) { copy(isLoadingMore = false) }
            }
        }
    }

    /** Reads cached items from the collection and maps them to [PostRsUiModel] instances. */
    private suspend fun loadItemsForTab(tab: PostRsListTab) {
        val collection = collections[tab] ?: return

        @Suppress("TooGenericExceptionCaught")
        try {
            val isSearch = _searchQuery.value.isNotBlank()
            val items = withContext(Dispatchers.IO) {
                collection.loadItems().map { item ->
                    item.state.toUiModel(item.id, showStatus = isSearch)
                }
            }
            val uiModels = items.map { model ->
                val effectiveTab = if (isSearch) tabForStatus(model.status) else tab
                model.copy(actions = getMenuActions(effectiveTab, model.hasPassword, model.commentsOpen))
            }
            updateTabUiState(tab) { copy(posts = uiModels, isLoading = false, error = null) }
        } catch (e: Exception) {
            AppLog.e(AppLog.T.POSTS, "Failed to load items for tab $tab", e)
        }
    }

    /**
     * Reads pagination and sync state from the collection's list info and updates
     * the tab's UI state accordingly.
     */
    private suspend fun updateListInfoForTab(tab: PostRsListTab) {
        val collection = collections[tab] ?: return

        val listInfo = withContext(Dispatchers.IO) { collection.listInfo() }
        val morePages = listInfo?.hasMorePages ?: false
        val fetchingFirstPage = listInfo?.state == ListState.FETCHING_FIRST_PAGE
        val isUserRefresh = userRefreshingTabs.contains(tab)

        if (!fetchingFirstPage) userRefreshingTabs.remove(tab)

        updateTabUiState(tab) {
            copy(
                isRefreshing = isUserRefresh && fetchingFirstPage,
                isLoadingMore = listInfo?.state == ListState.FETCHING_NEXT_PAGE,
                canLoadMore = morePages,
                error = if (listInfo?.state == ListState.ERROR) {
                    listInfo.errorMessage ?: resourceProvider.getString(R.string.error_generic)
                } else {
                    null
                }
            )
        }
    }

    /** Returns the current UI state for [tab], or a default loading state. */
    private fun getTabUiState(tab: PostRsListTab): PostTabUiState {
        return _tabStates.value[tab] ?: PostTabUiState(isLoading = true)
    }

    /** Updates the UI state for [tab] by applying [update] to the current state. */
    private fun updateTabUiState(tab: PostRsListTab, update: PostTabUiState.() -> PostTabUiState) {
        _tabStates.value += (tab to getTabUiState(tab).update())
    }

    override fun onCleared() {
        super.onCleared()
        collections.values.forEach { it.close() }
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val SEARCH_DEBOUNCE_MS = 250L
        internal const val MIN_SEARCH_QUERY_LENGTH = 3
        private val ALL_STATUSES = PostRsListTab.entries.flatMap { it.statuses }.distinct()
    }
}
