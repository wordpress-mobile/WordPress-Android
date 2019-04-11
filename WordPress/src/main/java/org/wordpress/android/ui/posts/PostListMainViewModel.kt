package org.wordpress.android.ui.posts

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Intent
import android.support.annotation.ColorRes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.AuthorFilterSelection.EVERYONE
import org.wordpress.android.ui.posts.AuthorFilterSelection.ME
import org.wordpress.android.ui.posts.PostListType.DRAFTS
import org.wordpress.android.ui.posts.PostListType.PUBLISHED
import org.wordpress.android.ui.posts.PostListType.SCHEDULED
import org.wordpress.android.ui.posts.PostListType.TRASHED
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.ToastUtils.Duration
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.DialogHolder
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.LocalPostId
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

private const val SCROLL_TO_DELAY = 50L
private val FAB_VISIBLE_POST_LIST_PAGES = listOf(PUBLISHED, DRAFTS)
val POST_LIST_PAGES = listOf(PUBLISHED, DRAFTS, SCHEDULED, TRASHED)

class PostListMainViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val postStore: PostStore,
    private val accountStore: AccountStore,
    uploadStore: UploadStore,
    mediaStore: MediaStore,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val prefs: AppPrefsWrapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
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

    private val _updatePostsPager = SingleLiveEvent<AuthorFilterSelection>()
    val updatePostsPager = _updatePostsPager

    private val _selectTab = SingleLiveEvent<Int>()
    val selectTab = _selectTab as LiveData<Int>

    private val _scrollToLocalPostId = SingleLiveEvent<LocalPostId>()
    val scrollToLocalPostId = _scrollToLocalPostId as LiveData<LocalPostId>

    private val _snackBarMessage = SingleLiveEvent<SnackbarMessageHolder>()
    val snackBarMessage = _snackBarMessage as LiveData<SnackbarMessageHolder>

    // TODO: Implement toast message in PostListActivity
    private val _toastMessage = SingleLiveEvent<ToastMessageHolder>()
    val toastMessage: LiveData<ToastMessageHolder> = _toastMessage

    private val _dialogAction = SingleLiveEvent<DialogHolder>()
    val dialogAction: LiveData<DialogHolder> = _dialogAction

    private val _postUploadAction = SingleLiveEvent<PostUploadAction>()
    val postUploadAction: LiveData<PostUploadAction> = _postUploadAction

    init {
        lifecycleRegistry.markState(Lifecycle.State.CREATED)
    }

    fun start(site: SiteModel) {
        this.site = site

        val authorFilterSelection: AuthorFilterSelection = prefs.postListAuthorSelection

        listenForPostListEvents(
                lifecycle = lifecycle,
                dispatcher = dispatcher,
                postStore = postStore,
                site = site,
                postConflictResolver = postConflictResolver,
                postActionHandler = postActionHandler,
                // TODO: Do we have to refresh all lists
                refreshList = this::invalidateAllLists,
                triggerPostUploadAction = { _postUploadAction.postValue(it) },
                invalidateUploadStatus = {
                    uploadStatesTracker.invalidateUploadStatus(it)
                    invalidateAllLists()
                },
                invalidateFeaturedMedia = {
                    featuredImageTracker.invalidateFeaturedMedia(it)
                    invalidateAllLists()
                }
        )
        _updatePostsPager.value = authorFilterSelection
        _viewState.value = PostListMainViewState(
                isFabVisible = FAB_VISIBLE_POST_LIST_PAGES.contains(POST_LIST_PAGES.first()),
                isAuthorFilterVisible = site.isUsingWpComRestApi,
                authorFilterSelection = authorFilterSelection,
                authorFilterItems = getAuthorFilterItems(authorFilterSelection)
        )
        lifecycleRegistry.markState(Lifecycle.State.STARTED)
    }

    override fun onCleared() {
        lifecycleRegistry.markState(Lifecycle.State.DESTROYED)
        scrollToTargetPostJob.cancel() // cancels all coroutines with the default coroutineContext
        super.onCleared()
    }

    fun newPost() {
        postActionHandler.newPost()
    }

    fun updateAuthorFilterSelection(selectionId: Long) {
        val selection = AuthorFilterSelection.fromId(selectionId)

        updateViewStateTriggerPagerChange(
                authorFilterSelection = selection,
                authorFilterItems = getAuthorFilterItems(selection)
        )
        prefs.postListAuthorSelection = selection
    }

    fun onTabChanged(position: Int) {
        val currentPage = POST_LIST_PAGES[position]
        updateViewStateTriggerPagerChange(isFabVisible = FAB_VISIBLE_POST_LIST_PAGES.contains(currentPage))
    }

    fun showTargetPost(targetPostId: Int) {
        val postModel = postStore.getPostByLocalPostId(targetPostId)
        if (postModel == null) {
            _snackBarMessage.value = SnackbarMessageHolder(string.error_post_does_not_exist)
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

    private fun getAuthorFilterItems(selection: AuthorFilterSelection): List<AuthorFilterListItemUIState> {
        return AuthorFilterSelection.values().map { value ->
            @ColorRes val backgroundColorRes: Int =
                    if (selection == value) R.color.grey_lighten_30_translucent_50
                    else R.color.transparent

            when (value) {
                ME -> AuthorFilterListItemUIState.Me(accountStore.account?.avatarUrl, backgroundColorRes)
                EVERYONE -> AuthorFilterListItemUIState.Everyone(backgroundColorRes)
            }
        }
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
            _updatePostsPager.value = authorFilterSelection
        }
    }

    /**
     */

    private val uploadStatesTracker = PostListUploadStatusTracker(uploadStore = uploadStore)
    private val featuredImageTracker = PostListFeaturedImageTracker(dispatcher = dispatcher, mediaStore = mediaStore)

    private val postListDialogHelper: PostListDialogHelper by lazy {
        PostListDialogHelper(
                showDialog = { _dialogAction.postValue(it) },
                checkNetworkConnection = this::checkNetworkConnection
        )
    }

    private val postConflictResolver: PostConflictResolver by lazy {
        PostConflictResolver(
                dispatcher = dispatcher,
                postStore = postStore,
                site = site,
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
                postConflictResolver = postConflictResolver,
                triggerPostListAction = { _postListAction.postValue(it) },
                triggerPostUploadAction = { _postUploadAction.postValue(it) },
                invalidateList = this::invalidateAllLists,
                checkNetworkConnection = this::checkNetworkConnection,
                showSnackbar = { _snackBarMessage.postValue(it) },
                showToast = { _toastMessage.postValue(it) }
        )
    }

    private fun invalidateAllLists() {
        TODO()
    }

    private fun checkNetworkConnection(): Boolean =
            if (networkUtilsWrapper.isNetworkAvailable()) {
                true
            } else {
                _toastMessage.postValue(ToastMessageHolder(string.no_network_message, Duration.SHORT))
                false
            }

    fun handleEditPostResult(data: Intent?) {
        postActionHandler.handleEditPostResult(data)
    }

    // BasicFragmentDialog Events

    fun onPositiveClickedForBasicDialog(instanceTag: String) {
        postListDialogHelper.onPositiveClickedForBasicDialog(
                instanceTag = instanceTag,
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
}
