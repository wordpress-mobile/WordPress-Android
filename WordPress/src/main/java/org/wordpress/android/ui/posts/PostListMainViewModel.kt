package org.wordpress.android.ui.posts

import android.annotation.SuppressLint
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_LIST_AUTHOR_FILTER_CHANGED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_LIST_CREATE_POST_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_LIST_SEARCH_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_LIST_TAB_CHANGED
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ListActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.PostModel
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
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.uploads.UploadActionUseCase
import org.wordpress.android.ui.uploads.UploadStarter
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.ToastUtils.Duration
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.viewmodel.Event
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
private val FAB_VISIBLE_POST_LIST_PAGES = listOf(PUBLISHED, DRAFTS, SCHEDULED, TRASHED)
val POST_LIST_PAGES = listOf(PUBLISHED, DRAFTS, SCHEDULED, TRASHED)
private const val TRACKS_SELECTED_TAB = "selected_tab"
private const val TRACKS_SELECTED_AUTHOR_FILTER = "author_filter_selection"
private const val TRACKS_ACTION = "action"
private const val TRACKS_CREATE_NEW_POST = "create_new_post"

class PostListMainViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val postStore: PostStore,
    private val accountStore: AccountStore,
    uploadActionUseCase: UploadActionUseCase,
    uploadStore: UploadStore,
    mediaStore: MediaStore,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val prefs: AppPrefsWrapper,
    private val postListEventListenerFactory: PostListEventListener.Factory,
    private val previewStateHelper: PreviewStateHelper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val savePostToDbUseCase: SavePostToDbUseCase,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val uploadStarter: UploadStarter,
    private val postConflictResolutionFeatureUtils: PostConflictResolutionFeatureUtils,
    private val postConflictDetector: PostConflictDetector
) : ViewModel(), CoroutineScope {
    private var isStarted = false

    private val lifecycleOwner = object : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle = lifecycleRegistry
    }

    private val scrollToTargetPostJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + scrollToTargetPostJob

    private lateinit var site: SiteModel
    private lateinit var editPostRepository: EditPostRepository
    var currentBottomSheetPostId: LocalId? = null

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

    private val _openPrepublishingBottomSheet = MutableLiveData<Event<Unit>>()
    val openPrepublishingBottomSheet: LiveData<Event<Unit>> = _openPrepublishingBottomSheet

    private val _snackBarMessage = SingleLiveEvent<SnackbarMessageHolder>()
    val snackBarMessage = _snackBarMessage as LiveData<SnackbarMessageHolder>

    private val _previewState = SingleLiveEvent<PostListRemotePreviewState>()
    val previewState = _previewState as LiveData<PostListRemotePreviewState>

    private val _toastMessage = SingleLiveEvent<ToastMessageHolder>()
    val toastMessage: LiveData<ToastMessageHolder> = _toastMessage

    private val _dialogAction = SingleLiveEvent<DialogHolder>()
    val dialogAction: LiveData<DialogHolder> = _dialogAction

    private val _conflictResolutionAction = SingleLiveEvent<PostResolutionOverlayActionEvent.ShowDialogAction>()
    val conflictResolutionAction: LiveData<PostResolutionOverlayActionEvent.ShowDialogAction> =
        _conflictResolutionAction

    private val _postUploadAction = SingleLiveEvent<PostUploadAction>()
    val postUploadAction: LiveData<PostUploadAction> = _postUploadAction

    private val _isSearchExpanded = MutableLiveData<Boolean>()
    val isSearchExpanded: LiveData<Boolean> = _isSearchExpanded

    private val _searchQuery = MutableLiveData<String?>()
    val searchQuery: LiveData<String?> = _searchQuery

    private val uploadStatusTracker = PostModelUploadStatusTracker(
        uploadStore = uploadStore,
        uploadActionUseCase = uploadActionUseCase
    )
    private val featuredImageTracker = PostListFeaturedImageTracker(dispatcher = dispatcher, mediaStore = mediaStore)

    private val postFetcher by lazy {
        PostFetcher(lifecycleOwner.lifecycle, dispatcher)
    }

    private val postListDialogHelper: PostListDialogHelper by lazy {
        PostListDialogHelper(
            showDialog = { _dialogAction.postValue(it) },
            showConflictResolutionOverlay = { _conflictResolutionAction.postValue(it) },
            checkNetworkConnection = this::checkNetworkConnection,
            analyticsTracker = analyticsTracker,
            isPostConflictResolutionEnabled = postConflictResolutionFeatureUtils.isPostConflictResolutionEnabled()
        )
    }

    private val postConflictResolver: PostConflictResolver by lazy {
        PostConflictResolver(
            dispatcher = dispatcher,
            site = site,
            getPostByLocalPostId = postStore::getPostByLocalPostId,
            invalidateList = this::invalidateAllLists,
            checkNetworkConnection = this::checkNetworkConnection,
            showSnackBar = { _snackBarMessage.postValue(it) },
            uploadStore = uploadStore,
            postStore = postStore
        )
    }

    private val postActionHandler: PostActionHandler by lazy {
        PostActionHandler(
            dispatcher = dispatcher,
            site = site,
            postStore = postStore,
            postListDialogHelper = postListDialogHelper,
            doesPostHaveUnhandledConflict = postConflictDetector::hasUnhandledConflict,
            hasUnhandledAutoSave = postConflictDetector::hasUnhandledAutoSave,
            triggerPostListAction = { _postListAction.postValue(it) },
            triggerPostUploadAction = { _postUploadAction.postValue(it) },
            triggerPublishAction = this::showPrepublishingBottomSheet,
            invalidateList = this::invalidateAllLists,
            checkNetworkConnection = this::checkNetworkConnection,
            showSnackbar = { _snackBarMessage.postValue(it) },
            showToast = { _toastMessage.postValue(it) },
            triggerPreviewStateUpdate = this::updatePreviewAndDialogState,
            copyPost = this::copyPost,
            postConflictResolutionFeatureUtils = postConflictResolutionFeatureUtils
        )
    }

    fun copyPost(site: SiteModel, postToCopy: PostModel, performChecks: Boolean = false) {
        if (performChecks && (postConflictDetector.hasUnhandledConflict(postToCopy) ||
                    postConflictDetector.hasUnhandledAutoSave(postToCopy))
        ) {
            postListDialogHelper.showCopyConflictDialog(postToCopy)
            return
        }
        val post = postStore.instantiatePostModel(
            site,
            false,
            postToCopy.title,
            postToCopy.content,
            PostStatus.DRAFT.toString(),
            postToCopy.categoryIdList,
            postToCopy.postFormat,
            true
        )
        _postListAction.postValue(PostListAction.EditPost(site, post, loadAutoSaveRevision = false))
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
        site.isUsingWpComRestApi && site.hasCapabilityEditOthersPosts
                && (site.isSingleUserSite != null && !site.isSingleUserSite)
    }

    init {
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    @Suppress("LongMethod")
    fun start(
        site: SiteModel,
        initPreviewState: PostListRemotePreviewState,
        currentBottomSheetPostId: LocalId,
        editPostRepository: EditPostRepository
    ) {
        if (isStarted) return
        this.site = site
        this.editPostRepository = editPostRepository

        val authorFilterSelection: AuthorFilterSelection = if (isFilteringByAuthorSupported) {
            prefs.postListAuthorSelection
        } else {
            AuthorFilterSelection.EVERYONE
        }

        postListEventListenerFactory.createAndStartListening(
            lifecycle = lifecycleOwner.lifecycle,
            dispatcher = dispatcher,
            bgDispatcher = bgDispatcher,
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
            },
            triggerPreviewStateUpdate = this::updatePreviewAndDialogState,
            isRemotePreviewingFromPostsList = this::isRemotePreviewingFromPostsList,
            hasRemoteAutoSavePreviewError = this::hasRemoteAutoSavePreviewError
        )

        _authorSelectionUpdated.value = authorFilterSelection
        _viewState.value = PostListMainViewState(
            isFabVisible = FAB_VISIBLE_POST_LIST_PAGES.contains(POST_LIST_PAGES.first()) &&
                    isSearchExpanded.value != true,
            isAuthorFilterVisible = isFilteringByAuthorSupported,
            authorFilterSelection = authorFilterSelection,
            authorFilterItems = getAuthorFilterItems(authorFilterSelection, accountStore.account?.avatarUrl)
        )
        _previewState.value = _previewState.value ?: initPreviewState

        currentBottomSheetPostId.let { postId ->
            if (postId.value != 0) {
                editPostRepository.loadPostByLocalPostId(postId.value)
            }
        }

        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.STARTED

        uploadStarter.queueUploadFromSite(site)

        editPostRepository.run {
            postChanged.observe(lifecycleOwner, Observer {
                savePostToDbUseCase.savePostToDb(editPostRepository, site)
            })
        }

        isStarted = true
    }

    override fun onCleared() {
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
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
            uploadStatusTracker = uploadStatusTracker,
            doesPostHaveUnhandledConflict = postConflictDetector::hasUnhandledConflict,
            hasAutoSave = postConflictDetector::hasUnhandledAutoSave,
            postFetcher = postFetcher,
            getFeaturedImageUrl = featuredImageTracker::getFeaturedImageUrl
        )
    }

    fun onSearchExpanded(restorePreviousSearch: Boolean) {
        if (isSearchExpanded.value != true) {
            AnalyticsUtils.trackWithSiteDetails(POST_LIST_SEARCH_ACCESSED, site)

            if (!restorePreviousSearch) {
                clearSearch()
            }

            _isSearchExpanded.value = true
            _viewState.value = _viewState.value?.copy(isFabVisible = false, isAuthorFilterVisible = false)
        }
    }

    fun onSearchCollapsed(delay: Long = SEARCH_COLLAPSE_DELAY) {
        _isSearchExpanded.value = false
        clearSearch()

        launch {
            delay(delay)
            withContext(mainDispatcher) {
                _viewState.value =
                    _viewState.value?.copy(isFabVisible = true, isAuthorFilterVisible = isFilteringByAuthorSupported)
            }
        }
    }

    fun onSearch(searchQuery: String) {
        _searchQuery.value = searchQuery
    }

    private fun clearSearch() {
        _searchQuery.value = null
    }

    fun fabClicked() {
        analyticsTracker.track(POST_LIST_CREATE_POST_TAPPED,
            mapOf(TRACKS_ACTION to TRACKS_CREATE_NEW_POST )
        )

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
            _snackBarMessage.value = SnackbarMessageHolder(UiStringRes(R.string.error_post_does_not_exist))
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
        switchToDraftTabIfNeeded(data)
        postActionHandler.handleEditPostResult(data)
    }

    private fun switchToDraftTabIfNeeded(data: Intent?) {
        if (data != null && data.getBooleanExtra(EditPostActivityConstants.EXTRA_IS_NEW_POST, false) &&
            data.getBooleanExtra(EditPostActivityConstants.EXTRA_HAS_CHANGES, false)
        ) {
            _selectTab.value = POST_LIST_PAGES.indexOf(DRAFTS)
        }
    }

    private fun editRestoredAutoSavePost(localPostId: Int) {
        val post = postStore.getPostByLocalPostId(localPostId)
        if (post != null) {
            _postListAction.postValue(PostListAction.EditPost(site, post, loadAutoSaveRevision = true))
        } else {
            _snackBarMessage.value = SnackbarMessageHolder(UiStringRes((R.string.error_post_does_not_exist)))
        }
    }

    private fun editLocalPost(localPostId: Int) {
        val post = postStore.getPostByLocalPostId(localPostId)
        if (post != null) {
            _postListAction.postValue(PostListAction.EditPost(site, post, loadAutoSaveRevision = false))
        } else {
            _snackBarMessage.value = SnackbarMessageHolder(UiStringRes(R.string.error_post_does_not_exist))
        }
    }

    private fun copyLocalPost(localPostId: Int) {
        val post = postStore.getPostByLocalPostId(localPostId)
        if (post != null) {
            copyPost(site, post)
        } else {
            _snackBarMessage.value = SnackbarMessageHolder(UiStringRes(R.string.error_post_does_not_exist))
        }
    }

    // BasicFragmentDialog Events

    fun onPositiveClickedForBasicDialog(instanceTag: String) {
        postListDialogHelper.onPositiveClickedForBasicDialog(
            instanceTag = instanceTag,
            trashPostWithLocalChanges = postActionHandler::trashPostWithLocalChanges,
            trashPostWithUnsavedChanges = postActionHandler::trashPostWithUnsavedChanges,
            deletePost = postActionHandler::deletePost,
            publishPost = postActionHandler::publishPost,
            updateConflictedPostWithRemoteVersion = postConflictResolver::updateConflictedPostWithRemoteVersion,
            editRestoredAutoSavePost = this::editRestoredAutoSavePost,
            moveTrashedPostToDraft = postActionHandler::moveTrashedPostToDraft,
            resolveConflictsAndEditPost = postActionHandler::resolveConflictsAndEditPost
        )
    }

    fun onNegativeClickedForBasicDialog(instanceTag: String) {
        postListDialogHelper.onNegativeClickedForBasicDialog(
            instanceTag = instanceTag,
            updateConflictedPostWithLocalVersion = postConflictResolver::updateConflictedPostWithLocalVersion,
            editLocalPost = this::editLocalPost,
            copyLocalPost = this::copyLocalPost
        )
    }

    fun onDismissByOutsideTouchForBasicDialog(instanceTag: String) {
        postListDialogHelper.onDismissByOutsideTouchForBasicDialog(
            instanceTag = instanceTag,
            updateConflictedPostWithLocalVersion = postConflictResolver::updateConflictedPostWithLocalVersion,
            editLocalPost = this::editLocalPost,
            copyLocalPost = this::copyLocalPost
        )
    }

    // Post Resolution Overlay Actions
    fun onPostResolutionConfirmed(event: PostResolutionOverlayActionEvent.PostResolutionConfirmationEvent) {
        postListDialogHelper.onPostResolutionConfirmed(
            event = event,
            updateConflictedPostWithRemoteVersion = postConflictResolver::updateConflictedPostWithRemoteVersion,
            editRestoredAutoSavePost = this::editRestoredAutoSavePost,
            editLocalPost = this::editLocalPost,
            updateConflictedPostWithLocalVersion = postConflictResolver::updateConflictedPostWithLocalVersion
        )
    }

    private fun showPrepublishingBottomSheet(post: PostModel) {
        currentBottomSheetPostId = LocalId(post.id)
        editPostRepository.loadPostByLocalPostId(post.id)
        _openPrepublishingBottomSheet.postValue(Event(Unit))
    }

    /**
     * Only the non-null variables will be changed in the current state
     */
    @SuppressLint("NullSafeMutableLiveData")
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

    private fun isRemotePreviewingFromPostsList() = _previewState.value != null &&
            _previewState.value != PostListRemotePreviewState.NONE

    private fun hasRemoteAutoSavePreviewError() = _previewState.value != null &&
            _previewState.value == PostListRemotePreviewState.REMOTE_AUTO_SAVE_PREVIEW_ERROR

    private fun checkNetworkConnection(): Boolean =
        if (networkUtilsWrapper.isNetworkAvailable()) {
            true
        } else {
            _toastMessage.postValue(ToastMessageHolder(R.string.no_network_message, Duration.SHORT))
            false
        }

    fun handleRemotePreviewClosing() {
        updatePreviewAndDialogState(PostListRemotePreviewState.NONE, PostInfoType.PostNoInfo)
    }

    private fun updatePreviewAndDialogState(newState: PostListRemotePreviewState, postInfo: PostInfoType) {
        // We need only transitions, so...
        if (_previewState.value == newState) return

        AppLog.d(
            AppLog.T.POSTS,
            "Posts list preview state machine: transition from ${_previewState.value} to $newState"
        )

        // update the state
        val prevState = _previewState.value
        _previewState.postValue(newState)

        // take care of exit actions on state transition
        previewStateHelper.managePreviewStateTransitions(
            newState,
            prevState,
            postInfo,
            postActionHandler::handleRemotePreview
        )
    }

    fun onBottomSheetPublishButtonClicked() {
        editPostRepository.getEditablePost()?.let {
            postActionHandler.publishPost(it)
        }
    }

    fun refreshUiStateForAuthorFilter() {
        _viewState.value = _viewState.value?.copy()
    }
}
