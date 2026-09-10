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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus as FluxCPostStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.datasource.PostViewsDataResult
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.postsrs.data.PostRsRestClient
import org.wordpress.android.ui.postsrs.data.WpServiceProvider
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.rs.RsCommentCountFetcher
import org.wordpress.android.ui.rs.RsPostChangeListener
import org.wordpress.android.ui.rs.RsTabLoading
import org.wordpress.android.ui.rs.RsTabRefreshJobs
import org.wordpress.android.ui.rs.RsUploadedPost
import org.wordpress.android.ui.rs.toRsPostStatus
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import rs.wordpress.cache.kotlin.ObservableMetadataCollection
import rs.wordpress.cache.kotlin.getObservablePostMetadataCollectionWithEditContext
import rs.wordpress.cache.kotlin.hasMorePages
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostStatus
import uniffi.wp_api.PostUpdateParams
import uniffi.wp_api.WpApiParamPostsOrderBy
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
    private val fluxCBridge: PostRsFluxCBridge,
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val accountStore: AccountStore,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val changeListener: RsPostChangeListener,
    private val statsDataSource: StatsDataSource,
    private val commentCountFetcher: RsCommentCountFetcher,
) : ViewModel() {
    private val _tabStates = MutableStateFlow<Map<PostRsListTab, PostTabUiState>>(emptyMap())
    val tabStates: StateFlow<Map<PostRsListTab, PostTabUiState>> = _tabStates.asStateFlow()

    private val _isOpeningPost = MutableStateFlow(false)
    val isOpeningPost: StateFlow<Boolean> = _isOpeningPost.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private var activeSearchTab = PostRsListTab.PUBLISHED

    private val collections = mutableMapOf<PostRsListTab, ObservableMetadataCollection>()
    private val initializingTabs = mutableSetOf<PostRsListTab>()
    private val userRefreshingTabs = mutableSetOf<PostRsListTab>()
    private val refreshJobs = RsTabRefreshJobs<PostRsListTab>()

    private var isScreenVisible = false
    private var hasDeferredChange = false
    private var pendingReveal: PostRsReveal? = null

    /** Tabs whose collection has completed at least one fetch, so an empty list means empty. */
    private val fetchedTabs = mutableSetOf<PostRsListTab>()
    private val resolveImageJobs = mutableMapOf<PostRsListTab, Job>()
    private val resolveAuthorJobs = mutableMapOf<PostRsListTab, Job>()
    /**
     * Outstanding metric fetches, cancelled only at teardown.
     *
     * Deliberately not cancelled when the visible rows change: a cancelled fetch releases its
     * in-flight claim without filling the cache, and the next visible set would skip those ids as
     * "already in flight", stranding their rows on the loading skeleton with nothing left to
     * resolve them. Volume is bounded by [visiblePostIds] instead.
     */
    private val metricJobs = mutableSetOf<Job>()

    /** The rows on screen right now, so work queued for rows scrolled past can be dropped. */
    private var visiblePostIds = emptySet<Long>()

    /**
     * Row metrics keyed by remote post id, so scrolling back to a row does not refetch it and a
     * cache reload does not blank the numbers out. Only touched from the main dispatcher.
     *
     * A present key means "fetched"; a null value means the fetch came back with nothing usable.
     * Rows distinguish the two so a failure clears the loading skeleton rather than pinning it.
     *
     * The two are kept apart because they do not reach equally far: comment counts come from the
     * site's own REST API and work everywhere, while view counts need WordPress.com stats.
     */
    private val viewCountCache = mutableMapOf<Long, Long?>()
    private val commentCountCache = mutableMapOf<Long, Long?>()
    private val inFlightViewCounts = mutableSetOf<Long>()
    private val inFlightCommentCounts = mutableSetOf<Long>()
    private var lastTrackedTab: PostRsListTab? = null

    private val _events = Channel<PostRsListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _snackbarMessages = Channel<SnackbarMessage>(Channel.BUFFERED)
    val snackbarMessages = _snackbarMessages.receiveAsFlow()

    private val _revealRequests = Channel<PostRsReveal>(Channel.BUFFERED)
    val revealRequests = _revealRequests.receiveAsFlow()

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

    /**
     * View counts come from the WP.com stats endpoint, so they need a WP.com site ID and the
     * capability to read stats. Self-hosted sites reached over application passwords have neither
     * and show no view counts - their comment counts still work, since those come from the site's
     * own REST API.
     */
    private val canFetchViewCounts: Boolean by lazy {
        _site != null &&
            _site.siteId > 0 &&
            SiteUtils.isAccessedViaWPComRest(_site) &&
            _site.hasCapabilityViewStats
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
            // Subscribe before starting the listener - its flow has no replay, so a change
            // reported in between would be dropped.
            viewModelScope.launch {
                changeListener.changes.collect { onRemoteChangeDetected() }
            }
            viewModelScope.launch {
                changeListener.uploads.collect { onPostUploaded(it) }
            }
            changeListener.start(site, isPages = false)
        }
    }

    /** Called when the screen becomes visible, and again whenever it returns from the background. */
    @MainThread
    fun onScreenVisible() {
        isScreenVisible = true
        if (hasDeferredChange) onRemoteChangeDetected()
        emitPendingReveal()
    }

    @MainThread
    fun onScreenHidden() {
        isScreenVisible = false
    }

    /**
     * Refreshes the list after FluxC reported a change the rs collections can't see - a post saved
     * in the editor, for instance - or remembers to.
     *
     * Most of these arrive while the editor covers the list, and refreshing a screen nobody is
     * looking at spends a request per open tab on a result that may be superseded before it is
     * seen. A refresh while offline could only fail, and [refreshAllTabs] would then mark every
     * tab with an error the user never asked for. Either way the change is remembered, however
     * many arrive, and the list catches up with a single refresh in [onScreenVisible].
     */
    private fun onRemoteChangeDetected() {
        hasDeferredChange = true
        if (!isScreenVisible || !networkUtilsWrapper.isNetworkAvailable()) return
        hasDeferredChange = false
        refreshAllTabs()
    }

    /**
     * Remembers to point the user at a post the editor just saved: it lands on whichever tab its
     * status belongs to, not necessarily the one being looked at. Held until [onScreenVisible],
     * because the upload usually finishes while the editor still covers the list.
     */
    private fun onPostUploaded(upload: RsUploadedPost) {
        val status = upload.status.toRsPostStatus() ?: return
        pendingReveal = PostRsReveal(tabForStatus(status), upload.remotePostId)
        emitPendingReveal()
    }

    private fun emitPendingReveal() {
        if (!isScreenVisible) return
        val reveal = pendingReveal ?: return
        pendingReveal = null
        _revealRequests.trySend(reveal)
    }

    /**
     * Bridges the post into FluxC's database and emits an
     * [PostRsListEvent.EditPost] to open the editor.
     * If the post is trashed, shows a confirmation dialog first.
     */
    @MainThread
    fun openPost(remotePostId: Long, tab: PostRsListTab) {
        if (_isOpeningPost.value) return
        if (tab == PostRsListTab.TRASHED) {
            analyticsTracker.track(
                Stat.POST_LIST_ITEM_SELECTED,
                site,
                mapOf(
                    TRACKS_ACTION to "move_to_draft",
                    TRACKS_POST_ID to remotePostId
                )
            )
            _pendingConfirmation.value =
                PendingConfirmation.MoveToDraft(remotePostId)
            return
        }
        analyticsTracker.track(
            Stat.POST_LIST_ITEM_SELECTED,
            site,
            mapOf(
                TRACKS_ACTION to "edit",
                TRACKS_POST_ID to remotePostId
            )
        )
        bridgeAndOpen(remotePostId)
    }

    /**
     * Refreshes all currently initialized tabs. Called when
     * returning from the settings screen after saving changes.
     */
    @MainThread
    fun refreshAllTabs() {
        restClient.clearCaches()
        val tabs = collections.keys.toList()
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            // Every tab would report the same connection failure, so record the failure on each
            // and send one message for the whole fan-out. It has to be sent here rather than by
            // a nominated tab: a tab only offers a snackbar when it has content to keep, so
            // picking one that turned out to be empty would swallow the message entirely.
            val anyTabKeepsItsPosts = tabs.any { getTabUiState(it).posts.isNotEmpty() }
            tabs.forEach { onRefreshFailed(it, e = null, showSnackbar = false) }
            if (anyTabKeepsItsPosts) {
                _snackbarMessages.trySend(
                    SnackbarMessage(
                        message = friendlyErrorMessage(null),
                        actionLabel = resourceProvider.getString(R.string.retry),
                        onAction = { refreshAllTabs() }
                    )
                )
            }
            return
        }
        tabs.forEach { tab ->
            refreshTab(tab)
        }
    }

    /** Tracks a tab change event when the user swipes or taps a tab. */
    @MainThread
    fun onTabChanged(tab: PostRsListTab) {
        if (tab == lastTrackedTab) return
        lastTrackedTab = tab
        analyticsTracker.track(
            Stat.POST_LIST_TAB_CHANGED,
            site,
            mapOf(TRACKS_SELECTED_TAB to tab.name.lowercase())
        )
    }

    /** Emits a [PostRsListEvent.CreatePost] for the selected site. */
    @MainThread
    fun createNewPost() {
        analyticsTracker.track(
            Stat.POST_LIST_CREATE_POST_TAPPED,
            site,
            mapOf(TRACKS_ACTION to TRACKS_CREATE_NEW_POST)
        )
        _events.trySend(PostRsListEvent.CreatePost(site))
    }

    /**
     * Clears all cached collections and tab states so the list
     * appears empty while the user types a search query.
     */
    @MainThread
    fun onSearchOpen() {
        analyticsTracker.track(Stat.POST_LIST_SEARCH_ACCESSED, site)
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
        analyticsTracker.track(
            Stat.POST_LIST_AUTHOR_FILTER_CHANGED,
            site,
            mapOf(TRACKS_SELECTED_AUTHOR_FILTER to selection.toString())
        )
        appPrefsWrapper.postListAuthorSelection = selection
        _authorFilter.value = selection
        clearCollections()
        initTab(activeTab)
    }

    /** Routes a menu action tap to the appropriate event or dialog. */
    @MainThread
    @Suppress("LongMethod", "ReturnCount")
    fun onPostMenuAction(remotePostId: Long, action: PostRsMenuAction) {
        analyticsTracker.track(
            Stat.POST_LIST_BUTTON_PRESSED,
            site,
            mapOf(
                TRACKS_ACTION to action.toAnalyticsAction(),
                TRACKS_POST_ID to remotePostId
            )
        )
        val post = findPost(remotePostId)

        when (action) {
            PostRsMenuAction.SETTINGS ->
                _events.trySend(PostRsListEvent.OpenPostSettings(remotePostId))
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
            PostRsMenuAction.BLAZE -> bridgeAndOpen(remotePostId, blaze = true)
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
                val post = bridgePostOrNull(postId)
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
                AppLog.e(AppLog.T.POSTS, "Move to draft failed", e)
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

    @Suppress("TooGenericExceptionCaught")
    private suspend fun bridgePostOrNull(remotePostId: Long) = try {
        val lastModified = findPost(remotePostId)?.lastModified
        withContext(Dispatchers.IO) {
            fluxCBridge.fetchAndBridge(remotePostId, site, lastModified)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppLog.e(AppLog.T.POSTS, "Bridge post failed", e)
        _snackbarMessages.trySend(
            SnackbarMessage(friendlyErrorMessage(e, R.string.post_not_found))
        )
        null
    }

    /**
     * Duplicates a post by creating a new draft with the same content.
     * Bridges the source post into FluxC first so we can read its
     * content, then creates a new local draft from it.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun duplicatePost(remotePostId: Long) {
        if (!checkNetwork()) return
        _isOpeningPost.value = true
        viewModelScope.launch {
            try {
                val lastModified = findPost(remotePostId)?.lastModified
                val postToCopy = withContext(Dispatchers.IO) {
                    fluxCBridge.fetchAndBridge(
                        remotePostId, site, lastModified
                    )
                }
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.T.POSTS, "Duplicate post failed", e)
                _snackbarMessages.trySend(
                    SnackbarMessage(
                        friendlyErrorMessage(e, R.string.post_not_found)
                    )
                )
            } finally {
                _isOpeningPost.value = false
            }
        }
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

    private fun friendlyErrorMessage(
        e: Exception? = null,
        defaultResId: Int? = null,
    ): String = PostRsErrorUtils.friendlyErrorMessage(
        e, defaultResId, resourceProvider, networkUtilsWrapper
    )

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
     * Fetches the post via the bridge (fast path from FluxC cache
     * or slow path via wordpress-rs), then opens the editor or
     * Blaze promotion screen.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun bridgeAndOpen(
        remotePostId: Long,
        blaze: Boolean = false
    ) {
        if (!checkNetwork()) return
        _isOpeningPost.value = true
        viewModelScope.launch {
            try {
                val lastModified = findPost(remotePostId)?.lastModified
                val post = withContext(Dispatchers.IO) {
                    fluxCBridge.fetchAndBridge(
                        remotePostId, site, lastModified
                    )
                }
                if (blaze) {
                    _events.trySend(
                        PostRsListEvent.PromoteWithBlaze(site, post)
                    )
                } else {
                    _events.trySend(
                        PostRsListEvent.EditPost(site, post)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.T.POSTS, "Bridge post failed", e)
                _snackbarMessages.trySend(
                    SnackbarMessage(
                        friendlyErrorMessage(e, R.string.post_not_found)
                    )
                )
            } finally {
                _isOpeningPost.value = false
            }
        }
    }

    private fun getMenuActions(
        tab: PostRsListTab,
        hasPassword: Boolean,
        commentsOpen: Boolean
    ): List<PostRsMenuAction> = buildList {
        when (tab) {
            PostRsListTab.PUBLISHED -> {
                add(PostRsMenuAction.SETTINGS)
                add(PostRsMenuAction.VIEW)
                add(PostRsMenuAction.READ)
                add(PostRsMenuAction.MOVE_TO_DRAFT)
                add(PostRsMenuAction.DUPLICATE)
                add(PostRsMenuAction.SHARE)
                if (!hasPassword && blazeFeatureUtils.isSiteBlazeEligible(site)) {
                    add(PostRsMenuAction.BLAZE)
                }
                if (SiteUtils.isAccessedViaWPComRest(site) &&
                    site.hasCapabilityViewStats
                ) {
                    add(PostRsMenuAction.STATS)
                }
                if (commentsOpen) add(PostRsMenuAction.COMMENTS)
                add(PostRsMenuAction.TRASH)
            }
            PostRsListTab.DRAFTS -> {
                add(PostRsMenuAction.SETTINGS)
                add(PostRsMenuAction.VIEW)
                add(PostRsMenuAction.READ)
                if (site.hasCapabilityPublishPosts) {
                    add(PostRsMenuAction.PUBLISH)
                }
                add(PostRsMenuAction.DUPLICATE)
                add(PostRsMenuAction.SHARE)
                add(PostRsMenuAction.TRASH)
            }
            PostRsListTab.SCHEDULED -> {
                add(PostRsMenuAction.SETTINGS)
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
        refreshJobs.clear()
        fetchedTabs.clear()
        resolveImageJobs.values.forEach { it.cancel() }
        resolveImageJobs.clear()
        resolveAuthorJobs.values.forEach { it.cancel() }
        resolveAuthorJobs.clear()
        metricJobs.forEach { it.cancel() }
        metricJobs.clear()
        visiblePostIds = emptySet()
        viewCountCache.clear()
        commentCountCache.clear()
        inFlightViewCounts.clear()
        inFlightCommentCounts.clear()
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
                        isAuthError = PostRsErrorUtils.isAuthError(e)
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
            updateTabUiState(tab) {
                copy(
                    isLoading = RsTabLoading.onRefreshStarted(
                        hasItems = posts.isNotEmpty(),
                        hasFetched = tab in fetchedTabs
                    ),
                    error = null
                )
            }
        }

        when {
            // A refresh with no connection can only fail, so report it without the round trip.
            !networkUtilsWrapper.isNetworkAvailable() ->
                onRefreshFailed(tab, e = null, showSnackbar = isUserRefresh)

            // The tab is already refreshing. Its progress state is set above either way, and
            // the request is replayed by [startRefresh] once the running one finishes.
            refreshJobs.deferIfRunning(tab) -> Unit

            else -> startRefresh(tab, collection, isUserRefresh)
        }
    }

    /** Runs the one refresh a tab is allowed at a time, then replays any request it deferred. */
    private fun startRefresh(
        tab: PostRsListTab,
        collection: ObservableMetadataCollection,
        isUserRefresh: Boolean
    ) {
        val job = viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                withContext(Dispatchers.IO) { collection.refresh() }
                fetchedTabs.add(tab)
                // Drop only the "nothing to show" entries so a transient failure is retried,
                // while numbers already fetched stay put.
                viewCountCache.entries.removeAll { it.value == null }
                commentCountCache.entries.removeAll { it.value == null }
                userRefreshingTabs.remove(tab)
                // Read the fetched items and end both progress states here rather than relying
                // on the collection observers, which aren't guaranteed to fire for a refresh.
                loadItemsForTab(tab)
                updateTabUiState(tab) { copy(isLoading = false, isRefreshing = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onRefreshFailed(tab, e, showSnackbar = isUserRefresh)
            }
            if (refreshJobs.onFinished(tab)) refreshTab(tab)
        }
        refreshJobs.onStarted(tab, job)
    }

    /**
     * A tab that already has posts on screen keeps them and offers a retry snackbar; an empty one
     * shows the full-screen error state.
     *
     * [e] is null when no request was made because the device is offline. The message is the same
     * either way - [friendlyErrorMessage] reports the network error whenever the device is offline,
     * regardless of what failed.
     *
     * [showSnackbar] is false when the user didn't ask for this refresh - [initTab] refreshes each
     * tab as the pager settles on it, and interrupting someone browsing cached posts with an error
     * they didn't provoke is noise. The full-screen error still covers a tab with nothing to show.
     * It's also false when another tab is already reporting the same failure.
     */
    private fun onRefreshFailed(tab: PostRsListTab, e: Exception?, showSnackbar: Boolean = true) {
        e?.let { AppLog.e(AppLog.T.POSTS, "Failed to refresh tab $tab", it) }
        userRefreshingTabs.remove(tab)
        val message = friendlyErrorMessage(e)
        val authError = PostRsErrorUtils.isAuthError(e)
        if (getTabUiState(tab).posts.isNotEmpty()) {
            updateTabUiState(tab) {
                copy(isLoading = false, isRefreshing = false, error = null)
            }
            if (showSnackbar) {
                _snackbarMessages.trySend(
                    SnackbarMessage(
                        message = message,
                        actionLabel = if (authError) null
                            else resourceProvider.getString(R.string.retry),
                        // Tapping retry is the user asking, so the result has to be reported -
                        // a silent second failure looks like the button did nothing.
                        onAction = if (authError) null
                            else ({ refreshTab(tab, isUserRefresh = true) })
                    )
                )
            }
        } else {
            updateTabUiState(tab) {
                copy(
                    isLoading = false, isRefreshing = false,
                    error = message,
                    isAuthError = authError
                )
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
            val nowLabel = resourceProvider.getString(R.string.rs_date_now)
            val items = withContext(Dispatchers.IO) {
                collection.loadItems().map { item ->
                    item.state.toUiModel(item.id, nowLabel, showStatus = isSearch)
                }
            }
            val existingPosts = getTabUiState(tab).posts
            val uiModels = items.map { model ->
                val effectiveTab = if (isSearch) tabForStatus(model.status) else tab
                val existing = existingPosts
                    .firstOrNull { it.remotePostId == model.remotePostId }
                model.copy(
                    actions = getMenuActions(effectiveTab, model.hasPassword, model.commentsOpen),
                    featuredImageUrl = if (
                        model.featuredImageId != 0L &&
                        model.featuredImageId == existing?.featuredImageId
                    ) {
                        existing.featuredImageUrl
                    } else {
                        null
                    },
                    authorDisplayName = if (
                        model.authorId != 0L &&
                        model.authorId == existing?.authorId
                    ) {
                        existing.authorDisplayName
                    } else {
                        null
                    },
                    // Read straight from the metrics cache: rebuilding from the collection would
                    // otherwise blank out numbers already fetched on every change it reports.
                    viewCount = viewCountCache[model.remotePostId],
                    commentCount = commentCountCache[model.remotePostId],
                    // Search mixes statuses into one list, and metrics are only fetched for
                    // published posts, so skeletons there would never resolve.
                    areMetricsPending = expectsMetrics(tab) &&
                        !isSearch &&
                        isAnyMetricOutstanding(model.remotePostId)
                )
            }
            updateTabUiState(tab) {
                copy(
                    posts = uiModels,
                    isLoading = RsTabLoading.onItemsLoaded(
                        wasLoading = isLoading,
                        hasItems = uiModels.isNotEmpty()
                    ),
                    error = null
                )
            }
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
                restClient.fetchMediaUrls(
                    site, unresolvedIds, THUMBNAIL_SIZE_DP,
                    THUMBNAIL_ASPECT
                )
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
     * Fetches all-time views and comment counts for the rows currently on screen.
     *
     * The stats API has no batched "metrics for these posts" call - `fetchPostViews` answers for a
     * single post - so this is driven by scroll position rather than by the page load, and capped
     * by a semaphore. Each new visible set cancels the previous batch, so flinging past a hundred
     * rows does not queue a hundred requests; whatever was already in flight finishes and lands in
     * the cache.
     *
     * Published posts only: an unpublished post has no view history to report.
     */
    /**
     * Whether rows on [tab] should expect metrics at all, and so whether to show a skeleton.
     *
     * Comment counts work on every site, so any published list expects something; view counts are
     * an extra that only WordPress.com-connected sites add on top.
     */
    private fun expectsMetrics(tab: PostRsListTab) = tab == PostRsListTab.PUBLISHED

    /** Whether either metric is still expected for [postId] but has not arrived. */
    private fun isAnyMetricOutstanding(postId: Long) =
        !commentCountCache.containsKey(postId) ||
            (canFetchViewCounts && !viewCountCache.containsKey(postId))

    /**
     * Fetches metrics for the rows currently on screen.
     *
     * Driven by scroll position rather than by the page load because neither source is free: view
     * counts are one request per post, and comment counts are one per visible batch. Each new
     * visible set cancels the previous batch, so flinging past a hundred rows does not queue a
     * hundred requests; whatever was already in flight finishes and lands in the cache.
     *
     * Published posts only - a draft has neither a view history nor comments.
     */
    @MainThread
    fun onRowsVisible(tab: PostRsListTab, postIds: List<Long>) {
        if (!expectsMetrics(tab)) return
        visiblePostIds = postIds.toSet()
        fetchCommentCounts(tab, postIds)
        fetchViewCounts(tab, postIds)
    }

    /**
     * Fetches comment counts for the visible rows in a single batched request.
     *
     * Unlike view counts this needs no WordPress.com account or stats capability: it reads the
     * site's own REST API through whichever transport the site already uses.
     */
    @MainThread
    private fun fetchCommentCounts(tab: PostRsListTab, postIds: List<Long>) {
        val wanted = postIds.filter {
            !commentCountCache.containsKey(it) && it !in inFlightCommentCounts
        }
        if (wanted.isEmpty()) return

        inFlightCommentCounts.addAll(wanted)
        track(viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val counts = withContext(Dispatchers.IO) {
                    commentCountFetcher.fetchCommentCounts(site, wanted)
                }
                // Anything the fetch could not answer for is recorded as "nothing to show" so its
                // row stops waiting; a refresh clears those entries and tries again.
                wanted.forEach { commentCountCache[it] = counts[it] }
                applyMetrics(tab, wanted)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.T.POSTS, "Failed to fetch comment counts", e)
                wanted.forEach { commentCountCache[it] = null }
                applyMetrics(tab, wanted)
            } finally {
                inFlightCommentCounts.removeAll(wanted.toSet())
            }
        })
    }

    /**
     * Fetches all-time view counts for the visible rows, a few at a time.
     *
     * The stats API answers for one post at a time, so this is capped by a semaphore and each row
     * is merged in as soon as it lands rather than waiting for the whole batch.
     */
    @MainThread
    private fun fetchViewCounts(tab: PostRsListTab, postIds: List<Long>) {
        // The stats data source authenticates with the WP.com bearer token and throws if it is
        // asked for data before being given one.
        val accessToken = accountStore.accessToken.takeUnless { it.isNullOrEmpty() }
        val wanted = postIds.filter {
            !viewCountCache.containsKey(it) && it !in inFlightViewCounts
        }
        if (!canFetchViewCounts || accessToken == null || wanted.isEmpty()) return

        track(viewModelScope.launch {
            statsDataSource.init(accessToken)
            val gate = Semaphore(MAX_CONCURRENT_VIEW_FETCHES)
            wanted.forEach { postId ->
                launch {
                    gate.withPermit {
                        // Re-checked after waiting for a permit rather than before queuing: by the
                        // time a slot frees up the user may have scrolled well past this row, and
                        // fetching it would spend a request on something off screen.
                        if (postId in visiblePostIds) fetchViewCountFor(tab, postId)
                    }
                }
            }
        })
    }

    /** Keeps a job around so teardown can cancel it, and forgets it once it finishes. */
    private fun track(job: Job) {
        metricJobs.add(job)
        job.invokeOnCompletion { metricJobs.remove(job) }
    }

    /**
     * Fetches one post's view count.
     *
     * Metrics decorate the rows; the list is perfectly usable without them, so nothing in this path
     * is allowed to take the screen down.
     */
    private suspend fun fetchViewCountFor(tab: PostRsListTab, postId: Long) {
        if (!inFlightViewCounts.add(postId)) return
        @Suppress("TooGenericExceptionCaught")
        try {
            val result = withContext(Dispatchers.IO) {
                statsDataSource.fetchPostViews(siteId = site.siteId, postId = postId)
            }
            viewCountCache[postId] = (result as? PostViewsDataResult.Success)?.data?.totalViews
            applyMetrics(tab, listOf(postId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(AppLog.T.POSTS, "Failed to fetch view count for post $postId", e)
            viewCountCache[postId] = null
            applyMetrics(tab, listOf(postId))
        } finally {
            inFlightViewCounts.remove(postId)
        }
    }

    /** Pushes whatever the caches now hold for [postIds] onto the tab's rows. */
    @MainThread
    private fun applyMetrics(tab: PostRsListTab, postIds: List<Long>) {
        val touched = postIds.toSet()
        updateTabUiState(tab) {
            copy(
                posts = posts.map { post ->
                    if (post.remotePostId in touched) {
                        post.copy(
                            viewCount = viewCountCache[post.remotePostId],
                            commentCount = commentCountCache[post.remotePostId],
                            areMetricsPending = isAnyMetricOutstanding(post.remotePostId)
                        )
                    } else {
                        post
                    }
                }
            )
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
            // This observer reports the same failure the refresh's catch block does, so it needs
            // the same rule: stay quiet unless the user asked for the refresh. Otherwise a
            // background one (initTab, as the pager settles) interrupts with an error nobody
            // provoked. The state above still syncs either way.
            if (isUserRefresh) {
                _snackbarMessages.trySend(
                    SnackbarMessage(
                        message = errorMessage.orEmpty(),
                        actionLabel = if (authError) null
                            else resourceProvider.getString(R.string.retry),
                        // As above: the user asked, so a second failure has to be reported.
                        onAction = if (authError) null
                            else ({ refreshTab(tab, isUserRefresh = true) })
                    )
                )
            }
        } else {
            updateTabUiState(tab) {
                copy(
                    isLoading = RsTabLoading.onListInfoChanged(
                        wasLoading = isLoading,
                        isFetchingFirstPage = fetchingFirstPage,
                        hasItems = posts.isNotEmpty(),
                        hasFetched = tab in fetchedTabs
                    ),
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

    public override fun onCleared() {
        super.onCleared()
        changeListener.stop()
        collections.values.forEach { it.close() }
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val SEARCH_DEBOUNCE_MS = 250L
        internal const val MIN_SEARCH_QUERY_LENGTH = 3
        private const val THUMBNAIL_SIZE_DP = 64

        /**
         * View counts are one request each, so a screenful is fetched a few at a time rather than
         * all at once. Comment counts need no such cap - they come back in one batched request.
         */
        private const val MAX_CONCURRENT_VIEW_FETCHES = 4

        /** Rows show the thumbnail in a square slot, cropped to fill. */
        private const val THUMBNAIL_ASPECT = 1f
        private val ALL_STATUSES = PostRsListTab.entries.flatMap { it.statuses }.distinct()

        private const val TRACKS_SELECTED_TAB = "selected_tab"
        private const val TRACKS_SELECTED_AUTHOR_FILTER = "author_filter_selection"
        private const val TRACKS_ACTION = "action"
        private const val TRACKS_POST_ID = "post_id"
        private const val TRACKS_CREATE_NEW_POST = "create_new_post"
    }
}

private fun PostRsMenuAction.toAnalyticsAction(): String = when (this) {
    PostRsMenuAction.SETTINGS -> "settings"
    PostRsMenuAction.VIEW -> "view"
    PostRsMenuAction.READ -> "read"
    PostRsMenuAction.PUBLISH -> "publish"
    PostRsMenuAction.MOVE_TO_DRAFT -> "move_to_draft"
    PostRsMenuAction.DUPLICATE -> "copy"
    PostRsMenuAction.SHARE -> "share"
    PostRsMenuAction.BLAZE -> "promote_with_blaze"
    PostRsMenuAction.STATS -> "stats"
    PostRsMenuAction.COMMENTS -> "comments"
    PostRsMenuAction.TRASH -> "trash"
    PostRsMenuAction.DELETE_PERMANENTLY -> "delete"
}
