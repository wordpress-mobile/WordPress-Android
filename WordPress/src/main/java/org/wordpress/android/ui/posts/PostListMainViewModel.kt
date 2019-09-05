package org.wordpress.android.ui.posts

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_LIST_AUTHOR_FILTER_CHANGED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_LIST_SEARCH_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_LIST_TAB_CHANGED
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ListActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.PostListType.DRAFTS
import org.wordpress.android.ui.posts.PostListType.PUBLISHED
import org.wordpress.android.ui.posts.PostListType.SCHEDULED
import org.wordpress.android.ui.posts.PostListType.TRASHED
import org.wordpress.android.ui.posts.PostListViewLayoutType.COMPACT
import org.wordpress.android.ui.posts.PostListViewLayoutType.STANDARD
import org.wordpress.android.ui.posts.PostListViewLayoutTypeMenuUiState.CompactViewLayoutTypeMenuUiState
import org.wordpress.android.ui.posts.PostListViewLayoutTypeMenuUiState.StandardViewLayoutTypeMenuUiState
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.uploads.UploadStarter
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.ToastUtils.Duration
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.DialogHolder
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder
import org.wordpress.android.viewmodel.posts.PostFetcher
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.LocalPostId
import org.wordpress.android.viewmodel.posts.PostListViewModelConnector
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

private const val SCROLL_TO_DELAY = 50L
private const val SEARCH_COLLAPSE_DELAY = 500L
private val FAB_VISIBLE_POST_LIST_PAGES = listOf(PUBLISHED, DRAFTS)
val POST_LIST_PAGES = listOf(PUBLISHED, DRAFTS, SCHEDULED, TRASHED)
private const val TRACKS_SELECTED_TAB = "selected_tab"
private const val TRACKS_SELECTED_AUTHOR_FILTER = "author_filter_selection"

class PostListMainViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val postStore: PostStore,
    private val accountStore: AccountStore,
    uploadStore: UploadStore,
    mediaStore: MediaStore,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val prefs: AppPrefsWrapper,
    private val postListEventListenerFactory: PostListEventListener.Factory,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val uploadStarter: UploadStarter
) : ViewModel(), LifecycleOwner, CoroutineScope {
    private val lifecycleRegistry = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    private val scrollToTargetPostJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + scrollToTargetPostJob

    private lateinit var site: SiteModel

    private val _viewState = MutableLiveData<PostListMainViewState>()
    val viewState: LiveData<PostListMainViewState> = _viewState

    private val _postListAction = SingleLiveEvent<PostListAction>()
    val postListAction: LiveData<PostListAction> = _postListAction

    private val _authorSelectionUpdated = MutableLiveData<AuthorFilterSelection>()
    val authorSelectionUpdated = _authorSelectionUpdated

    private val _selectTab = SingleLiveEvent<Int>()
    val selectTab = _selectTab as LiveData<Int>

    private val _scrollToLocalPostId = SingleLiveEvent<LocalPostId>()
    val scrollToLocalPostId = _scrollToLocalPostId as LiveData<LocalPostId>

    private val _snackBarMessage = SingleLiveEvent<SnackbarMessageHolder>()
    val snackBarMessage = _snackBarMessage as LiveData<SnackbarMessageHolder>

    private val _toastMessage = SingleLiveEvent<ToastMessageHolder>()
    val toastMessage: LiveData<ToastMessageHolder> = _toastMessage

    private val _dialogAction = SingleLiveEvent<DialogHolder>()
    val dialogAction: LiveData<DialogHolder> = _dialogAction

    private val _postUploadAction = SingleLiveEvent<PostUploadAction>()
    val postUploadAction: LiveData<PostUploadAction> = _postUploadAction

    private val _viewLayoutType = MutableLiveData<PostListViewLayoutType>()
    val viewLayoutType: LiveData<PostListViewLayoutType> = _viewLayoutType

    private val _viewLayoutTypeMenuUiState = MutableLiveData<PostListViewLayoutTypeMenuUiState>()
    val viewLayoutTypeMenuUiState: LiveData<PostListViewLayoutTypeMenuUiState> = _viewLayoutTypeMenuUiState

    private val _isSearchExpanded = MutableLiveData<Boolean>()
    val isSearchExpanded: LiveData<Boolean> = _isSearchExpanded

    private val _isSearchAvailable = MutableLiveData<Boolean>()
    val isSearchAvailable: LiveData<Boolean> = _isSearchAvailable

    private val _searchQuery = MutableLiveData<String>()
    val searchQuery: LiveData<String> = _searchQuery

    private val uploadStatusTracker = PostListUploadStatusTracker(uploadStore = uploadStore)
    private val featuredImageTracker = PostListFeaturedImageTracker(dispatcher = dispatcher, mediaStore = mediaStore)

    private val postFetcher by lazy {
        PostFetcher(lifecycle, dispatcher)
    }

    private val postListDialogHelper: PostListDialogHelper by lazy {
        PostListDialogHelper(
                showDialog = { _dialogAction.postValue(it) },
                checkNetworkConnection = this::checkNetworkConnection
        )
    }

    private val postConflictResolver: PostConflictResolver by lazy {
        PostConflictResolver(
                dispatcher = dispatcher,
                site = site,
                getPostByLocalPostId = postStore::getPostByLocalPostId,
                invalidateList = this::invalidateAllLists,
                checkNetworkConnection = this::checkNetworkConnection,
                showSnackbar = { _snackBarMessage.postValue(it) },
                showToast = { _toastMessage.postValue(it) }
        )
    }

    private val postActionHandler: PostActionHandler by lazy {
        PostActionHandler(
                dispatcher = dispatcher,
                site = site,
                postStore = postStore,
                postListDialogHelper = postListDialogHelper,
                doesPostHaveUnhandledConflict = postConflictResolver::doesPostHaveUnhandledConflict,
                triggerPostListAction = { _postListAction.postValue(it) },
                triggerPostUploadAction = { _postUploadAction.postValue(it) },
                invalidateList = this::invalidateAllLists,
                checkNetworkConnection = this::checkNetworkConnection,
                showSnackbar = { _snackBarMessage.postValue(it) },
                showToast = { _toastMessage.postValue(it) }
        )
    }

    /**
     * Filtering by author is disable on:
     * 1) Self-hosted sites - The XMLRPC api doesn't support filtering by author.
     * 2) Jetpack sites - we need to pass in the self-hosted user id to be able to filter for authors
     * which we currently can't
     * 3) Sites on which the user doesn't have permissions to edit posts of other users.
     *
     * This behavior is consistent with Calypso as of 11/4/2019.
     */
    private val isFilteringByAuthorSupported: Boolean by lazy {
        site.isWPCom && site.hasCapabilityEditOthersPosts
    }

    init {
        lifecycleRegistry.markState(Lifecycle.State.CREATED)
    }

    fun start(site: SiteModel) {
        this.site = site

        if (isSearchExpanded.value == true) {
            setViewLayoutAndIcon(COMPACT, false)
        } else {
            setUserPreferredViewLayoutType()
        }

        val authorFilterSelection: AuthorFilterSelection = if (isFilteringByAuthorSupported) {
            prefs.postListAuthorSelection
        } else {
            AuthorFilterSelection.EVERYONE
        }

        postListEventListenerFactory.createAndStartListening(
                lifecycle = lifecycle,
                dispatcher = dispatcher,
                postStore = postStore,
                site = site,
                postActionHandler = postActionHandler,
                handlePostUpdatedWithoutError = postConflictResolver::onPostSuccessfullyUpdated,
                handlePostUploadedWithoutError = {
                    refreshAllLists()
                },
                triggerPostUploadAction = { _postUploadAction.postValue(it) },
                invalidateUploadStatus = {
                    uploadStatusTracker.invalidateUploadStatus(it)
                    invalidateAllLists()
                },
                invalidateFeaturedMedia = {
                    featuredImageTracker.invalidateFeaturedMedia(it)
                    invalidateAllLists()
                }
        )

        _isSearchAvailable.value = SiteUtils.isAccessedViaWPComRest(site)
        _authorSelectionUpdated.value = authorFilterSelection
        _viewState.value = PostListMainViewState(
                isFabVisible = FAB_VISIBLE_POST_LIST_PAGES.contains(POST_LIST_PAGES.first()) &&
                        isSearchExpanded.value != true,
                isAuthorFilterVisible = isFilteringByAuthorSupported,
                authorFilterSelection = authorFilterSelection,
                authorFilterItems = getAuthorFilterItems(authorFilterSelection, accountStore.account?.avatarUrl)
        )
        lifecycleRegistry.markState(Lifecycle.State.STARTED)

        uploadStarter.queueUploadFromSite(site)
    }

    override fun onCleared() {
        lifecycleRegistry.markState(Lifecycle.State.DESTROYED)
        scrollToTargetPostJob.cancel() // cancels all coroutines with the default coroutineContext
        super.onCleared()
    }

    /*
     * FUTURE_REFACTOR: We shouldn't need to pass the AuthorFilterSelection to fragments and get it back, we have that
     * info already
     */
    fun getPostListViewModelConnector(
        postListType: PostListType
    ): PostListViewModelConnector {
        return PostListViewModelConnector(
                site = site,
                postListType = postListType,
                postActionHandler = postActionHandler,
                getUploadStatus = uploadStatusTracker::getUploadStatus,
                doesPostHaveUnhandledConflict = postConflictResolver::doesPostHaveUnhandledConflict,
                postFetcher = postFetcher,
                getFeaturedImageUrl = featuredImageTracker::getFeaturedImageUrl
        )
    }

    fun onSearchExpanded(restorePreviousSearch: Boolean) {
        if (isSearchExpanded.value != true) {
            setViewLayoutAndIcon(COMPACT, false)
            AnalyticsUtils.trackWithSiteDetails(POST_LIST_SEARCH_ACCESSED, site)

            if (!restorePreviousSearch) {
                clearSearch()
            }

            _isSearchExpanded.value = true
            _viewState.value = _viewState.value?.copy(isFabVisible = false)
        }
    }

    fun onSearchCollapsed(delay: Long = SEARCH_COLLAPSE_DELAY) {
        setUserPreferredViewLayoutType()
        _isSearchExpanded.value = false
        clearSearch()

        launch {
            delay(delay)
            withContext(mainDispatcher) {
                _viewState.value = _viewState.value?.copy(isFabVisible = true)
            }
        }
    }

    fun onSearch(searchQuery: String) {
        _searchQuery.value = searchQuery
    }

    private fun clearSearch() {
        _searchQuery.value = null
    }

    fun newPost() {
        postActionHandler.newPost()
    }

    fun updateAuthorFilterSelection(selectionId: Long) {
        val selection = AuthorFilterSelection.fromId(selectionId)

        updateViewStateTriggerPagerChange(
                authorFilterSelection = selection,
                authorFilterItems = getAuthorFilterItems(selection, accountStore.account?.avatarUrl)
        )
        if (isFilteringByAuthorSupported) {
            prefs.postListAuthorSelection = selection
        }
    }

    fun onTabChanged(position: Int) {
        val currentPage = POST_LIST_PAGES[position]
        updateViewStateTriggerPagerChange(isFabVisible = FAB_VISIBLE_POST_LIST_PAGES.contains(currentPage))

        AnalyticsUtils.trackWithSiteDetails(
                POST_LIST_TAB_CHANGED,
                site,
                mutableMapOf(TRACKS_SELECTED_TAB to currentPage.toString() as Any)
        )
    }

    fun showTargetPost(targetPostId: Int) {
        val postModel = postStore.getPostByLocalPostId(targetPostId)
        if (postModel == null) {
            _snackBarMessage.value = SnackbarMessageHolder(R.string.error_post_does_not_exist)
        } else {
            launch(mainDispatcher) {
                val targetTab = PostListType.fromPostStatus(PostStatus.fromPost(postModel))
                _selectTab.value = POST_LIST_PAGES.indexOf(targetTab)
                // we need to make sure the ViewPager initializes the targetTab fragment before we can propagate
                // the targetPostId to it
                withContext(bgDispatcher) {
                    delay(SCROLL_TO_DELAY)
                }
                _scrollToLocalPostId.value = LocalPostId(LocalId(postModel.id))
            }
        }
    }

    fun handleEditPostResult(data: Intent?) {
        postActionHandler.handleEditPostResult(data)
    }

    // BasicFragmentDialog Events

    fun onPositiveClickedForBasicDialog(instanceTag: String) {
        postListDialogHelper.onPositiveClickedForBasicDialog(
                instanceTag = instanceTag,
                trashPostWithLocalChanges = postActionHandler::trashPostWithLocalChanges,
                deletePost = postActionHandler::deletePost,
                publishPost = postActionHandler::publishPost,
                updateConflictedPostWithRemoteVersion = postConflictResolver::updateConflictedPostWithRemoteVersion
        )
    }

    fun onNegativeClickedForBasicDialog(instanceTag: String) {
        postListDialogHelper.onNegativeClickedForBasicDialog(
                instanceTag = instanceTag,
                updateConflictedPostWithLocalVersion = postConflictResolver::updateConflictedPostWithLocalVersion
        )
    }

    fun onDismissByOutsideTouchForBasicDialog(instanceTag: String) {
        postListDialogHelper.onDismissByOutsideTouchForBasicDialog(
                instanceTag = instanceTag,
                updateConflictedPostWithLocalVersion = postConflictResolver::updateConflictedPostWithLocalVersion
        )
    }

    /**
     * Only the non-null variables will be changed in the current state
     */
    private fun updateViewStateTriggerPagerChange(
        isFabVisible: Boolean? = null,
        isAuthorFilterVisible: Boolean? = null,
        authorFilterSelection: AuthorFilterSelection? = null,
        authorFilterItems: List<AuthorFilterListItemUIState>? = null
    ) {
        val currentState = requireNotNull(viewState.value) {
            "updateViewStateTriggerPagerChange should not be called before the initial state is set"
        }

        _viewState.value = PostListMainViewState(
                isFabVisible ?: currentState.isFabVisible,
                isAuthorFilterVisible ?: currentState.isAuthorFilterVisible,
                authorFilterSelection ?: currentState.authorFilterSelection,
                authorFilterItems ?: currentState.authorFilterItems
        )

        if (authorFilterSelection != null && currentState.authorFilterSelection != authorFilterSelection) {
            _authorSelectionUpdated.value = authorFilterSelection

            AnalyticsUtils.trackWithSiteDetails(
                    POST_LIST_AUTHOR_FILTER_CHANGED,
                    site,
                    mutableMapOf(TRACKS_SELECTED_AUTHOR_FILTER to authorFilterSelection.toString() as Any)
            )
        }
    }

    private fun invalidateAllLists() {
        val listTypeIdentifier = PostListDescriptor.calculateTypeIdentifier(site.id)
        dispatcher.dispatch(ListActionBuilder.newListDataInvalidatedAction(listTypeIdentifier))
    }

    private fun refreshAllLists() {
        val listTypeIdentifier = PostListDescriptor.calculateTypeIdentifier(site.id)
        dispatcher.dispatch(ListActionBuilder.newListRequiresRefreshAction(listTypeIdentifier))
    }

    private fun checkNetworkConnection(): Boolean =
            if (networkUtilsWrapper.isNetworkAvailable()) {
                true
            } else {
                _toastMessage.postValue(ToastMessageHolder(R.string.no_network_message, Duration.SHORT))
                false
            }

    fun toggleViewLayout() {
        val currentLayoutType = viewLayoutType.value ?: PostListViewLayoutType.defaultValue
        val toggledValue = when (currentLayoutType) {
            STANDARD -> COMPACT
            COMPACT -> STANDARD
        }
        AnalyticsUtils.trackAnalyticsPostListToggleLayout(toggledValue)
        setViewLayoutAndIcon(toggledValue, true)
    }

    private fun setViewLayoutAndIcon(layout: PostListViewLayoutType, storeIntoPreferences: Boolean = true) {
        _viewLayoutType.value = layout
        _viewLayoutTypeMenuUiState.value = when (layout) {
            STANDARD -> StandardViewLayoutTypeMenuUiState
            COMPACT -> CompactViewLayoutTypeMenuUiState
        }
        if (storeIntoPreferences) {
            prefs.postListViewLayoutType = layout
        }
    }

    private fun setUserPreferredViewLayoutType() {
        val savedLayoutType = prefs.postListViewLayoutType
        setViewLayoutAndIcon(savedLayoutType, false)
    }
}
