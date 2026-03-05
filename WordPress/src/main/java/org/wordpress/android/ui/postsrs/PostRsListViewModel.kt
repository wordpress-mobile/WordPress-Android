package org.wordpress.android.ui.postsrs

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
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
import org.wordpress.android.fluxc.model.post.PostStatus as FluxCPostStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.postsrs.data.PostRsRestClient
import org.wordpress.android.ui.postsrs.data.WpServiceProvider
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.viewmodel.ResourceProvider
import rs.wordpress.cache.kotlin.ObservableMetadataCollection
import rs.wordpress.cache.kotlin.getObservablePostMetadataCollectionWithEditContext
import rs.wordpress.cache.kotlin.hasMorePages
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostStatus
import uniffi.wp_api.PostUpdateParams
import uniffi.wp_api.RequestExecutionErrorReason
import uniffi.wp_api.WpApiException
import uniffi.wp_api.WpApiParamPostsOrderBy
import uniffi.wp_api.WpErrorCode
import uniffi.wp_mobile.FetchException
import uniffi.wp_mobile.PostListFilter
import uniffi.wp_mobile_cache.ListState
import javax.inject.Inject

@HiltViewModel
@Suppress("LargeClass")
class PostRsListViewModel @Inject constructor(
    selectedSiteRepository: SelectedSiteRepository,
    private val serviceProvider: WpServiceProvider,
    private val restClient: PostRsRestClient,
    private val resourceProvider: ResourceProvider,
    private val postStore: PostStore,
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val accountStore: AccountStore,
    private val appPrefsWrapper: AppPrefsWrapper,
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
    private val resolveImageJobs = mutableMapOf<PostRsListTab, Job>()
    private val resolveAuthorJobs = mutableMapOf<PostRsListTab, Job>()

    private val _events = Channel<PostRsListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _snackbarMessages = Channel<SnackbarMessage>(Channel.BUFFERED)
    val snackbarMessages = _snackbarMessages.receiveAsFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _site: SiteModel? = selectedSiteRepository.getSelectedSite()
    private val site: SiteModel
        get() = requireNotNull(_site) { "No selected site — Activity should have finished" }
    private val postService by lazy { serviceProvider.getService(site).posts() }

    val avatarUrl: String? = accountStore.account?.avatarUrl

    val isAuthorFilterSupported: Boolean by lazy {
        _site != null &&
            _site.isUsingWpComRestApi &&
            _site.hasCapabilityEditOthersPosts &&
            _site.isSingleUserSite == false
    }

    private val _authorFilter = MutableStateFlow(
        if (isAuthorFilterSupported) {
            appPrefsWrapper.postListAuthorSelection
        } else {
            AuthorFilterSelection.EVERYONE
        }
    )
    val authorFilter: StateFlow<AuthorFilterSelection> = _authorFilter.asStateFlow()

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
     * If the post is trashed, shows a confirmation dialog first.
     */
    @MainThread
    fun openPost(remotePostId: Long, tab: PostRsListTab) {
        if (tab == PostRsListTab.TRASHED) {
            _pendingConfirmation.value =
                PendingConfirmation.MoveToDraft(remotePostId)
            return
        }
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

    /**
     * Changes the author filter, persists the preference, then tears down
     * and rebuilds all collections so the new filter takes effect.
     */
    @MainThread
    fun onAuthorFilterChanged(selection: AuthorFilterSelection, activeTab: PostRsListTab) {
        if (selection == _authorFilter.value) return
        appPrefsWrapper.postListAuthorSelection = selection
        _authorFilter.value = selection
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
            PostRsMenuAction.PUBLISH -> publishPost(remotePostId)
            PostRsMenuAction.MOVE_TO_DRAFT -> moveToDraft(remotePostId)
            PostRsMenuAction.DUPLICATE -> duplicatePost(remotePostId)
        }
    }

    @MainThread
    fun onConfirmPendingAction() {
        when (val confirmation = _pendingConfirmation.value) {
            is PendingConfirmation.Trash -> trashPost(confirmation.postId)
            is PendingConfirmation.Delete -> deletePost(confirmation.postId)
            is PendingConfirmation.MoveToDraft ->
                moveToDraftAndEdit(confirmation.postId)
            null -> Unit
        }
        _pendingConfirmation.value = null
    }

    @MainThread
    fun onDismissPendingAction() {
        _pendingConfirmation.value = null
    }

    private fun trashPost(postId: Long) = executePostMutation(
        successMessageResId = R.string.post_rs_trashed,
        errorMessageResId = R.string.post_rs_error_trash,
        logTag = "Trash"
    ) {
        postService.trashPost(PostEndpointType.Posts, postId)
    }

    private fun deletePost(postId: Long) = executePostMutation(
        successMessageResId = R.string.post_rs_deleted,
        errorMessageResId = R.string.post_rs_error_delete,
        logTag = "Delete"
    ) {
        postService.deletePostPermanently(PostEndpointType.Posts, postId)
    }

    private fun publishPost(postId: Long) = executePostMutation(
        successMessageResId = R.string.post_rs_published,
        errorMessageResId = R.string.post_rs_error_update_status,
        logTag = "Publish"
    ) {
        postService.updatePost(
            PostEndpointType.Posts, postId,
            postStatusUpdate(PostStatus.Publish)
        )
    }

    private fun moveToDraft(postId: Long) = executePostMutation(
        successMessageResId = R.string.post_rs_moved_to_draft,
        errorMessageResId = R.string.post_rs_error_update_status,
        logTag = "Move to draft"
    ) {
        postService.updatePost(
            PostEndpointType.Posts, postId,
            postStatusUpdate(PostStatus.Draft)
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private fun moveToDraftAndEdit(postId: Long) {
        if (!checkNetwork()) return
        updateTabUiState(PostRsListTab.TRASHED) { copy(isRefreshing = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    postService.updatePost(
                        PostEndpointType.Posts, postId,
                        postStatusUpdate(PostStatus.Draft)
                    )
                }
                val post = getFluxCPost(postId)
                if (post != null) {
                    _events.trySend(PostRsListEvent.EditPost(site, post))
                } else {
                    _events.trySend(
                        PostRsListEvent.ShowToast(R.string.post_rs_moved_to_draft)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.T.POSTS, "Move to draft and edit failed", e)
                _snackbarMessages.trySend(
                    SnackbarMessage(
                        friendlyErrorMessage(e, R.string.post_rs_error_update_status)
                    )
                )
            } finally {
                updateTabUiState(PostRsListTab.TRASHED) {
                    copy(isRefreshing = false)
                }
            }
        }
    }

    /**
     * Duplicates a post by creating a new draft with the same content.
     * The FluxC dependency is temporary and will be removed once the
     * editor supports loading posts via wordpress-rs.
     */
    private fun duplicatePost(remotePostId: Long) {
        val postToCopy = getFluxCPost(remotePostId) ?: return
        val newPost = postStore.instantiatePostModel(
            site,
            false,
            postToCopy.title,
            postToCopy.content,
            FluxCPostStatus.DRAFT.toString(),
            postToCopy.categoryIdList,
            postToCopy.postFormat,
            true
        )
        _events.trySend(PostRsListEvent.EditPost(site, newPost))
    }

    /**
     * Extracts the underlying [WpApiException] from a [FetchException.Api]
     * wrapper so that callers can inspect API-level error details (status
     * codes, error reasons) without knowing about the wrapper type.
     */
    private fun unwrapException(e: Exception?): Exception? =
        (e as? FetchException.Api)?.v1 ?: e

    /**
     * Returns true when the exception represents an authentication failure
     * (rejected credentials, missing app-password, etc.).
     */
    private fun isAuthError(e: Exception?): Boolean {
        val apiException = unwrapException(e)
        val reason = (apiException as? WpApiException.RequestExecutionFailed)?.reason
        val errorCode = (apiException as? WpApiException.WpException)?.errorCode
        return reason is RequestExecutionErrorReason.HttpAuthenticationRejectedError ||
            reason is RequestExecutionErrorReason.HttpAuthenticationRequiredError ||
            errorCode is WpErrorCode.Unauthorized ||
            errorCode is WpErrorCode.ApplicationPasswordNotFound ||
            errorCode is WpErrorCode.NoAuthenticatedAppPassword
    }

    private fun checkNetwork(): Boolean {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _snackbarMessages.trySend(
                SnackbarMessage(resourceProvider.getString(R.string.no_network_message))
            )
            return false
        }
        return true
    }

    /**
     * Returns a user-friendly error subtitle based on the exception type.
     * Detects offline, authentication, and generic errors. When a default
     * resource ID is provided and the exception is a WpApiException with a
     * message, that message is used; otherwise falls back to the default.
     */
    private fun friendlyErrorMessage(e: Exception? = null, defaultResId: Int? = null): String {
        val apiException = unwrapException(e)
        val reason = (apiException as? WpApiException.RequestExecutionFailed)?.reason

        val resId = when {
            reason is RequestExecutionErrorReason.DeviceIsOfflineError ||
                !networkUtilsWrapper.isNetworkAvailable() ->
                R.string.error_generic_network

            isAuthError(e) -> R.string.post_rs_error_auth

            defaultResId != null -> defaultResId

            else -> R.string.request_failed_message
        }
        return resourceProvider.getString(resId)
    }

    /** Creates a PostUpdateParams for changing post status. */
    private fun postStatusUpdate(status: PostStatus) = PostUpdateParams(status = status, meta = null)

    /**
     * Executes a post mutation (trash, draft, etc.) with standard error handling.
     * Checks network, launches coroutine, executes operation on IO,
     * shows success/error messages.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun executePostMutation(
        successMessageResId: Int,
        errorMessageResId: Int,
        logTag: String,
        operation: suspend () -> Unit
    ) {
        if (!checkNetwork()) return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { operation() }
                _snackbarMessages.trySend(
                    SnackbarMessage(resourceProvider.getString(successMessageResId))
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.T.POSTS, "$logTag failed", e)
                _snackbarMessages.trySend(
                    SnackbarMessage(friendlyErrorMessage(e, errorMessageResId))
                )
            }
        }
    }

    /** Searches all tab states for a [PostRsUiModel] matching [remotePostId]. */
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
            _snackbarMessages.trySend(
                SnackbarMessage(resourceProvider.getString(R.string.post_not_found))
            )
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
        resolveImageJobs.values.forEach { it.cancel() }
        resolveImageJobs.clear()
        resolveAuthorJobs.values.forEach { it.cancel() }
        resolveAuthorJobs.clear()
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
                loadItemsForTab(tab)
                refreshTab(tab)
            } catch (e: Exception) {
                AppLog.e(AppLog.T.POSTS, "Failed to init RS post list tab", e)
                initializingTabs.remove(tab)
                updateTabUiState(tab) {
                    PostTabUiState(
                        error = friendlyErrorMessage(e),
                        isAuthError = isAuthError(e)
                    )
                }
            }
        }
    }

    /**
     * Builds an observable post collection for the given [tab] on IO.
     * When a search query is active, the filter includes all statuses;
     * otherwise it uses only the statuses for the tab.
     */
    private suspend fun createCollection(
        site: SiteModel,
        tab: PostRsListTab
    ): ObservableMetadataCollection = withContext(Dispatchers.IO) {
        val service = serviceProvider.getService(site)
        val query = _searchQuery.value
        val authorIds = if (_authorFilter.value == AuthorFilterSelection.ME) {
            accountStore.account?.userId?.let { listOf(it) } ?: emptyList()
        } else {
            emptyList()
        }
        val filter = PostListFilter(
            status = if (query.isNotBlank()) ALL_STATUSES else tab.statuses,
            order = tab.order,
            orderby = WpApiParamPostsOrderBy.DATE,
            search = query.ifBlank { null },
            author = authorIds
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
            restClient.clearCaches()
            userRefreshingTabs.add(tab)
            updateTabUiState(tab) { copy(isRefreshing = true, error = null) }
        } else {
            updateTabUiState(tab) { copy(error = null) }
        }

        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                withContext(Dispatchers.IO) { collection.refresh() }
            } catch (e: Exception) {
                AppLog.e(AppLog.T.POSTS, "Failed to refresh tab $tab", e)
                userRefreshingTabs.remove(tab)
                val message = friendlyErrorMessage(e)
                if (getTabUiState(tab).posts.isNotEmpty()) {
                    updateTabUiState(tab) {
                        copy(isLoading = false, isRefreshing = false, error = null)
                    }
                    val authError = isAuthError(e)
                    _snackbarMessages.trySend(
                        SnackbarMessage(
                            message = message,
                            actionLabel = if (authError) null
                                else resourceProvider.getString(R.string.retry),
                            onAction = if (authError) null
                                else ({ refreshTab(tab) })
                        )
                    )
                } else {
                    updateTabUiState(tab) {
                        copy(
                            isLoading = false, isRefreshing = false,
                            error = message,
                            isAuthError = isAuthError(e)
                        )
                    }
                }
            }
        }
    }

    /** Loads the next page of posts for [tab] if not already loading. */
    @MainThread
    fun loadMorePosts(tab: PostRsListTab) {
        val collection = collections[tab] ?: return
        val current = getTabUiState(tab)
        if (current.isLoadingMore || current.isRefreshing || !current.canLoadMore) return

        updateTabUiState(tab) { copy(isLoadingMore = true) }

        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                withContext(Dispatchers.IO) { collection.loadNextPage() }
            } catch (e: Exception) {
                AppLog.e(AppLog.T.POSTS, "Failed to load more for tab $tab", e)
                updateTabUiState(tab) { copy(isLoadingMore = false) }
                _snackbarMessages.trySend(
                    SnackbarMessage(friendlyErrorMessage(e))
                )
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
            val existingPosts = getTabUiState(tab).posts
            val uiModels = items.map { model ->
                val effectiveTab = if (isSearch) tabForStatus(model.status) else tab
                val existing = existingPosts
                    .firstOrNull { it.remotePostId == model.remotePostId }
                model.copy(
                    actions = getMenuActions(effectiveTab, model.hasPassword, model.commentsOpen),
                    featuredImageUrl = existing?.featuredImageUrl,
                    authorDisplayName = existing?.authorDisplayName
                )
            }
            updateTabUiState(tab) { copy(posts = uiModels, isLoading = false, error = null) }
            resolveFeaturedImages(tab, uiModels)
            resolveAuthorNames(tab, uiModels)
        } catch (e: Exception) {
            AppLog.e(AppLog.T.POSTS, "Failed to load items for tab $tab", e)
        }
    }

    /**
     * Fetches featured image URLs for posts that have a non-zero
     * [PostRsUiModel.featuredImageId] but no resolved URL yet.
     * All URLs are fetched in a single batched network call.
     */
    private fun resolveFeaturedImages(
        tab: PostRsListTab,
        posts: List<PostRsUiModel>
    ) {
        val unresolvedIds = posts
            .filter { it.featuredImageId != 0L && it.featuredImageUrl == null }
            .map { it.featuredImageId }
        if (unresolvedIds.isEmpty()) return

        resolveImageJobs[tab]?.cancel()
        resolveImageJobs[tab] = viewModelScope.launch {
            val urls = withContext(Dispatchers.IO) {
                restClient.fetchMediaUrls(site, unresolvedIds)
            }
            if (urls.isEmpty()) return@launch
            updateTabUiState(tab) {
                copy(
                    posts = this.posts.map { post ->
                        val url = urls[post.featuredImageId]
                        if (url != null) {
                            post.copy(featuredImageUrl = url)
                        } else {
                            post
                        }
                    }
                )
            }
        }
    }

    /**
     * Fetches display names for posts that have a non-zero
     * [PostRsUiModel.authorId] but no resolved name yet.
     * Skipped when filtering by "Me" since the user already
     * knows their own name.
     */
    private fun resolveAuthorNames(
        tab: PostRsListTab,
        posts: List<PostRsUiModel>
    ) {
        if (!isAuthorFilterSupported || _authorFilter.value == AuthorFilterSelection.ME) return

        val unresolvedIds = posts
            .filter { it.authorId != 0L && it.authorDisplayName == null }
            .map { it.authorId }
            .distinct()
        if (unresolvedIds.isEmpty()) return

        resolveAuthorJobs[tab]?.cancel()
        resolveAuthorJobs[tab] = viewModelScope.launch {
            val names = withContext(Dispatchers.IO) {
                restClient.fetchUserDisplayNames(site, unresolvedIds)
            }
            if (names.isEmpty()) return@launch
            updateTabUiState(tab) {
                copy(
                    posts = this.posts.map { post ->
                        val name = names[post.authorId]
                        if (name != null) {
                            post.copy(authorDisplayName = name)
                        } else {
                            post
                        }
                    }
                )
            }
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

        val isError = listInfo?.state == ListState.ERROR
        val hasPosts = getTabUiState(tab).posts.isNotEmpty()
        val errorMessage = if (isError) friendlyErrorMessage() else null

        if (isError && hasPosts) {
            val authError = getTabUiState(tab).isAuthError
            updateTabUiState(tab) {
                copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    canLoadMore = morePages,
                    error = null
                )
            }
            _snackbarMessages.trySend(
                SnackbarMessage(
                    message = errorMessage.orEmpty(),
                    actionLabel = if (authError) null
                        else resourceProvider.getString(R.string.retry),
                    onAction = if (authError) null
                        else ({ refreshTab(tab) })
                )
            )
        } else {
            updateTabUiState(tab) {
                copy(
                    isLoading = isLoading && fetchingFirstPage,
                    isRefreshing = isUserRefresh && fetchingFirstPage,
                    isLoadingMore = listInfo?.state
                        == ListState.FETCHING_NEXT_PAGE,
                    canLoadMore = morePages,
                    error = errorMessage
                )
            }
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
