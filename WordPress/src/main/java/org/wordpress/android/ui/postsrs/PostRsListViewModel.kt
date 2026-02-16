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
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.postsrs.data.WpSelfHostedServiceProvider
import org.wordpress.android.util.AppLog
import uniffi.wp_api.PostStatus
import org.wordpress.android.viewmodel.ResourceProvider
import rs.wordpress.cache.kotlin.ObservableMetadataCollection
import rs.wordpress.cache.kotlin.getObservablePostMetadataCollectionWithEditContext
import rs.wordpress.cache.kotlin.hasMorePages
import org.wordpress.android.fluxc.model.post.PostStatus as FluxCPostStatus
import uniffi.wp_api.PostEndpointType
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
    private val accountStore: AccountStore,
    private val blazeFeatureUtils: BlazeFeatureUtils,
) : ViewModel() {
    private val _tabStates =
        MutableStateFlow<Map<PostRsListTab, PostTabUiState>>(emptyMap())
    val tabStates: StateFlow<Map<PostRsListTab, PostTabUiState>> =
        _tabStates.asStateFlow()

    private val _authorFilter =
        MutableStateFlow(AuthorFilterSelection.EVERYONE)
    val authorFilter: StateFlow<AuthorFilterSelection> =
        _authorFilter.asStateFlow()

    val isAuthorFilterVisible: Boolean
        get() {
            val site = selectedSiteRepository.getSelectedSite()
                ?: return false
            return site.isUsingWpComRestApi
                && site.hasCapabilityEditOthersPosts
                && site.isSingleUserSite != null
                && !site.isSingleUserSite
        }

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

    /**
     * Updates the author filter and reloads all tabs.
     */
    @MainThread
    fun onAuthorFilterChanged(
        selection: AuthorFilterSelection,
        activeTab: PostRsListTab
    ) {
        if (_authorFilter.value == selection) return
        _authorFilter.value = selection
        clearCollections()
        initTab(activeTab)
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
        val authorIds = when (_authorFilter.value) {
            AuthorFilterSelection.ME ->
                listOf(accountStore.account.userId)
            AuthorFilterSelection.EVERYONE -> emptyList()
        }
        val filter = PostListFilter(
            status = if (query.isNotBlank()) {
                ALL_STATUSES
            } else {
                tab.statuses
            },
            order = tab.order,
            orderby = WpApiParamPostsOrderBy.DATE,
            search = query.ifBlank { null },
            author = authorIds
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
        val site = selectedSiteRepository.getSelectedSite()

        @Suppress("TooGenericExceptionCaught")
        try {
            val showStatus =
                _searchQuery.value.isNotBlank()
            val uiModels = withContext(Dispatchers.IO) {
                collection.loadItems().map { item ->
                    val model = item.state.toUiModel(
                        item.id,
                        showStatus = showStatus
                    )
                    if (site != null) {
                        model.copy(
                            actions = getMenuActions(
                                model.status,
                                site,
                                model.hasPassword
                            )
                        )
                    } else {
                        model
                    }
                }
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

    @Suppress("CyclomaticComplexMethod")
    @MainThread
    fun onPostMenuAction(
        remotePostId: Long,
        action: PostRsMenuAction
    ) {
        val site =
            selectedSiteRepository.getSelectedSite() ?: return
        val postUi = findPost(remotePostId) ?: return

        when (action) {
            PostRsMenuAction.VIEW ->
                _events.trySend(
                    PostRsListEvent.ViewPost(postUi.link)
                )
            PostRsMenuAction.READ ->
                _events.trySend(
                    PostRsListEvent.ReadPost(
                        site.siteId, remotePostId
                    )
                )
            PostRsMenuAction.SHARE ->
                _events.trySend(
                    PostRsListEvent.SharePost(
                        postUi.link, postUi.title
                    )
                )
            PostRsMenuAction.STATS ->
                _events.trySend(
                    PostRsListEvent.ViewStats(
                        site, remotePostId,
                        postUi.title, postUi.link
                    )
                )
            PostRsMenuAction.COMMENTS ->
                _events.trySend(
                    PostRsListEvent.ViewComments(
                        site.siteId, remotePostId
                    )
                )
            PostRsMenuAction.MOVE_TO_DRAFT,
            PostRsMenuAction.DUPLICATE,
            PostRsMenuAction.BLAZE,
            PostRsMenuAction.TRASH,
            PostRsMenuAction.DELETE_PERMANENTLY ->
                handleFluxCAction(
                    action, remotePostId, site
                )
        }
    }

    private fun handleFluxCAction(
        action: PostRsMenuAction,
        remotePostId: Long,
        site: SiteModel
    ) {
        val post = postStore.getPostByRemotePostId(
            remotePostId, site
        )
        if (post == null) {
            _events.trySend(
                PostRsListEvent.ShowError(
                    R.string.post_not_found
                )
            )
            return
        }
        if (action == PostRsMenuAction.DUPLICATE) {
            duplicatePost(site, post)
            return
        }
        val event = when (action) {
            PostRsMenuAction.MOVE_TO_DRAFT ->
                PostRsListEvent.MoveToDraft(site, post)
            PostRsMenuAction.BLAZE ->
                PostRsListEvent.PromoteWithBlaze(site, post)
            PostRsMenuAction.TRASH ->
                PostRsListEvent.TrashPost(site, post)
            PostRsMenuAction.DELETE_PERMANENTLY ->
                PostRsListEvent.DeletePost(site, post)
            else -> return
        }
        if (action == PostRsMenuAction.MOVE_TO_DRAFT ||
            action == PostRsMenuAction.TRASH ||
            action == PostRsMenuAction.DELETE_PERMANENTLY
        ) {
            removePostFromList(remotePostId)
        }
        _events.trySend(event)
    }

    private fun removePostFromList(remotePostId: Long) {
        _tabStates.value = _tabStates.value.mapValues { (_, state) ->
            val filtered = state.posts.filter {
                it.remotePostId != remotePostId
            }
            if (filtered.size == state.posts.size) {
                state
            } else {
                state.copy(posts = filtered)
            }
        }
    }

    private fun duplicatePost(
        site: SiteModel,
        postToCopy: PostModel
    ) {
        val newPost = postStore.instantiatePostModel(
            site, false,
            postToCopy.title,
            postToCopy.content,
            FluxCPostStatus.DRAFT.toString(),
            postToCopy.categoryIdList,
            postToCopy.postFormat,
            true
        )
        _events.trySend(
            PostRsListEvent.EditPost(site, newPost)
        )
    }

    private fun getMenuActions(
        status: PostStatus?,
        site: SiteModel,
        hasPassword: Boolean
    ): List<PostRsMenuAction> {
        val isTrashed = status is PostStatus.Trash
        val isPublished = status is PostStatus.Publish
        val isPrivate = status is PostStatus.Private
        val isDraft = status is PostStatus.Draft

        if (isTrashed) {
            return listOf(
                PostRsMenuAction.MOVE_TO_DRAFT,
                PostRsMenuAction.DELETE_PERMANENTLY
            )
        }

        return buildList {
            add(PostRsMenuAction.VIEW)
            add(PostRsMenuAction.READ)
            if (isPublished || isPrivate) {
                add(PostRsMenuAction.MOVE_TO_DRAFT)
            }
            if (isPublished || isPrivate || isDraft) {
                add(PostRsMenuAction.DUPLICATE)
            }
            add(PostRsMenuAction.SHARE)
            if (isPublished && !hasPassword &&
                blazeFeatureUtils.isSiteBlazeEligible(site)
            ) {
                add(PostRsMenuAction.BLAZE)
            }
            if (isPublished &&
                site.isUsingWpComRestApi
            ) {
                add(PostRsMenuAction.STATS)
            }
            if (isPublished || isPrivate) {
                add(PostRsMenuAction.COMMENTS)
            }
            add(PostRsMenuAction.TRASH)
        }
    }

    private fun findPost(
        remotePostId: Long
    ): PostRsUiModel? {
        return _tabStates.value.values
            .flatMap { it.posts }
            .firstOrNull { it.remotePostId == remotePostId }
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
        _tabStates.value = _tabStates.value + (tab to current.update())
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

sealed interface PostRsListEvent {
    data class EditPost(
        val site: SiteModel,
        val post: PostModel
    ) : PostRsListEvent

    data class CreatePost(
        val site: SiteModel
    ) : PostRsListEvent

    data class ShowError(
        val messageResId: Int
    ) : PostRsListEvent

    data class ViewPost(val url: String) : PostRsListEvent

    data class ReadPost(
        val blogId: Long,
        val postId: Long
    ) : PostRsListEvent

    data class MoveToDraft(
        val site: SiteModel,
        val post: PostModel
    ) : PostRsListEvent

    data class SharePost(
        val url: String,
        val title: String
    ) : PostRsListEvent

    data class PromoteWithBlaze(
        val site: SiteModel,
        val post: PostModel
    ) : PostRsListEvent

    data class ViewStats(
        val site: SiteModel,
        val remotePostId: Long,
        val postTitle: String,
        val postUrl: String
    ) : PostRsListEvent

    data class ViewComments(
        val blogId: Long,
        val postId: Long
    ) : PostRsListEvent

    data class TrashPost(
        val site: SiteModel,
        val post: PostModel
    ) : PostRsListEvent

    data class DeletePost(
        val site: SiteModel,
        val post: PostModel
    ) : PostRsListEvent
}
