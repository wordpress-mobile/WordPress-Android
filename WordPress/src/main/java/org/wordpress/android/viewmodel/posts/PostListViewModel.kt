package org.wordpress.android.viewmodel.posts

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.paging.PagedList
import android.content.Intent
import de.greenrobot.event.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.DeletePost
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.RemovePost
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.RestorePost
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.AuthorFilter
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForXmlRpcSite
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.fluxc.store.PostStore.PostDeleteActionType.DELETE
import org.wordpress.android.fluxc.store.PostStore.PostDeleteActionType.TRASH
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.posts.AuthorFilterSelection.EVERYONE
import org.wordpress.android.ui.posts.AuthorFilterSelection.ME
import org.wordpress.android.ui.posts.PostActionHandler
import org.wordpress.android.ui.posts.PostConflictResolver
import org.wordpress.android.ui.posts.PostListAction
import org.wordpress.android.ui.posts.PostListDialogHelper
import org.wordpress.android.ui.posts.PostListType
import org.wordpress.android.ui.posts.PostUploadAction
import org.wordpress.android.ui.posts.PostUploadAction.MediaUploadedSnackbar
import org.wordpress.android.ui.posts.PostUploadAction.PostUploadedSnackbar
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.posts.trackPostListAction
import org.wordpress.android.ui.reader.utils.ReaderImageScanner
import org.wordpress.android.ui.uploads.PostEvents
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.VideoOptimizer
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.ToastUtils.Duration
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.helpers.DialogHolder
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder
import org.wordpress.android.viewmodel.posts.PostListEmptyUiState.RefreshError
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.LocalPostId
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState
import javax.inject.Inject

typealias PagedPostList = PagedList<PostListItemType>

class PostListViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val listStore: ListStore,
    private val uploadStore: UploadStore,
    private val mediaStore: MediaStore,
    private val postStore: PostStore,
    private val accountStore: AccountStore,
    private val listItemUiStateHelper: PostListItemUiStateHelper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    connectionStatus: LiveData<ConnectionStatus>
) : ViewModel(), LifecycleOwner {
    private val isStatsSupported: Boolean by lazy {
        SiteUtils.isAccessedViaWPComRest(site) && site.hasCapabilityViewStats
    }
    private var isStarted: Boolean = false
    private var listDescriptor: PostListDescriptor? = null
    private lateinit var site: SiteModel
    private lateinit var postListType: PostListType

    // Cache upload statuses and featured images for posts for quicker access
    private val uploadStatusMap = HashMap<Int, PostListItemUploadStatus>()
    private val featuredImageMap = HashMap<Long, String>()

    private var scrollToLocalPostId: LocalPostId? = null

    private val _postListAction = SingleLiveEvent<PostListAction>()
    val postListAction: LiveData<PostListAction> = _postListAction

    private val _postUploadAction = SingleLiveEvent<PostUploadAction>()
    val postUploadAction: LiveData<PostUploadAction> = _postUploadAction

    private val _toastMessage = SingleLiveEvent<ToastMessageHolder>()
    val toastMessage: LiveData<ToastMessageHolder> = _toastMessage

    private val _dialogAction = SingleLiveEvent<DialogHolder>()
    val dialogAction: LiveData<DialogHolder> = _dialogAction

    private val _snackBarAction = SingleLiveEvent<SnackbarMessageHolder>()
    val snackBarAction: LiveData<SnackbarMessageHolder> = _snackBarAction

    private val _scrollToPosition = SingleLiveEvent<Int>()
    val scrollToPosition: LiveData<Int> = _scrollToPosition

    private val pagedListWrapper: PagedListWrapper<PostListItemType> by lazy {
        val listDescriptor = requireNotNull(listDescriptor) {
            "ListDescriptor needs to be initialized before this is observed!"
        }
        val dataSource = PostListItemDataSource(
                dispatcher = dispatcher,
                postStore = postStore,
                transform = this::transformPostModelToPostListItemUiState
        )
        listStore.getList(listDescriptor, dataSource, lifecycle)
    }

    val isFetchingFirstPage: LiveData<Boolean> by lazy { pagedListWrapper.isFetchingFirstPage }
    val isLoadingMore: LiveData<Boolean> by lazy { pagedListWrapper.isLoadingMore }
    val pagedListData: LiveData<PagedPostList> by lazy {
        val result = MediatorLiveData<PagedPostList>()
        result.addSource(pagedListWrapper.data) { pagedPostList ->
            pagedPostList?.let {
                onDataUpdated(it)
                result.value = it
            }
        }
        result
    }

    val emptyViewState: LiveData<PostListEmptyUiState> by lazy {
        val result = MediatorLiveData<PostListEmptyUiState>()
        val update = {
            createEmptyUiState(
                    postListType = postListType,
                    isNetworkAvailable = networkUtilsWrapper.isNetworkAvailable(),
                    isFetchingFirstPage = pagedListWrapper.isFetchingFirstPage.value ?: false,
                    isListEmpty = pagedListWrapper.isEmpty.value ?: true,
                    error = pagedListWrapper.listError.value,
                    fetchFirstPage = this::fetchFirstPage,
                    newPost = postActionHandler::newPost
            )
        }
        result.addSource(pagedListWrapper.isEmpty) { result.value = update() }
        result.addSource(pagedListWrapper.isFetchingFirstPage) { result.value = update() }
        result.addSource(pagedListWrapper.listError) { result.value = update() }
        result
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
                postStore = postStore,
                site = site,
                invalidateList = { pagedListWrapper.invalidateData() },
                checkNetworkConnection = this::checkNetworkConnection,
                showSnackbar = { _snackBarAction.postValue(it) },
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
                invalidateList = { pagedListWrapper.invalidateData() },
                checkNetworkConnection = this::checkNetworkConnection,
                showSnackbar = { _snackBarAction.postValue(it) },
                showToast = { _toastMessage.postValue(it) }
        )
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    // Lifecycle

    init {
        connectionStatus.observe(this, Observer {
            retryOnConnectionAvailableAfterRefreshError()
        })
        lifecycleRegistry.markState(Lifecycle.State.CREATED)
    }

    fun start(site: SiteModel, authorFilter: AuthorFilterSelection, postListType: PostListType) {
        if (isStarted) {
            return
        }
        this.site = site
        this.postListType = postListType

        this.listDescriptor = if (site.isUsingWpComRestApi) {
            val author: AuthorFilter = when (authorFilter) {
                ME -> AuthorFilter.SpecificAuthor(accountStore.account.userId)
                EVERYONE -> AuthorFilter.Everyone
            }

            PostListDescriptorForRestSite(site = site, statusList = postListType.postStatuses, author = author)
        } else {
            PostListDescriptorForXmlRpcSite(site = site, statusList = postListType.postStatuses)
        }

        // We should register after we have the SiteModel and ListDescriptor set
        EventBus.getDefault().register(this)
        dispatcher.register(this)

        isStarted = true
        lifecycleRegistry.markState(Lifecycle.State.STARTED)
        fetchFirstPage()
    }

    override fun onCleared() {
        lifecycleRegistry.markState(Lifecycle.State.DESTROYED)
        EventBus.getDefault().unregister(this)
        dispatcher.unregister(this)
        super.onCleared()
    }

    // Public Methods

    fun fetchFirstPage() {
        pagedListWrapper.fetchFirstPage()
    }

    fun handleEditPostResult(data: Intent?) {
        postActionHandler.handleEditPostResult(data)
    }

    private fun retryOnConnectionAvailableAfterRefreshError() {
        val connectionAvailableAfterRefreshError = networkUtilsWrapper.isNetworkAvailable() &&
                emptyViewState.value is RefreshError

        if (connectionAvailableAfterRefreshError) {
            fetchFirstPage()
        }
    }

    // FluxC Events

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onPostChanged(event: OnPostChanged) {
        when (event.causeOfChange) {
            // Fetched post list event will be handled by OnListChanged
            is CauseOfOnPostChanged.UpdatePost -> {
                if (event.isError) {
                    AppLog.e(
                            T.POSTS,
                            "Error updating the post with type: ${event.error.type} and message: ${event.error.message}"
                    )
                } else {
                    postConflictResolver.onPostSuccessfullyUpdated()
                }
            }
            is CauseOfOnPostChanged.DeletePost -> {
                val deletePostCauseOfChange = event.causeOfChange as DeletePost
                val localPostId = LocalId(deletePostCauseOfChange.localPostId)
                when (deletePostCauseOfChange.postDeleteActionType) {
                    TRASH -> postActionHandler.handlePostTrashed(localPostId = localPostId, isError = event.isError)
                    DELETE -> postActionHandler.handlePostDeletedOrRemoved(
                            localPostId = localPostId,
                            isRemoved = false,
                            isError = event.isError
                    )
                }
            }
            is CauseOfOnPostChanged.RestorePost -> {
                val localPostId = LocalId((event.causeOfChange as RestorePost).localPostId)
                postActionHandler.handlePostRestored(localPostId = localPostId, isError = event.isError)
            }
            is CauseOfOnPostChanged.RemovePost -> {
                val localPostId = LocalId((event.causeOfChange as RemovePost).localPostId)
                postActionHandler.handlePostDeletedOrRemoved(
                        localPostId = localPostId,
                        isRemoved = true,
                        isError = event.isError
                )
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMediaChanged(event: OnMediaChanged) {
        if (!event.isError && event.mediaList != null) {
            invalidateFeaturedMediaAndPagedListData(*event.mediaList.map { it.mediaId }.toLongArray())
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onPostUploaded(event: OnPostUploaded) {
        if (event.post != null && event.post.localSiteId == site.id) {
            _postUploadAction.postValue(PostUploadedSnackbar(dispatcher, site, event.post, event.isError, null))
            invalidateUploadStatusAndPagedListData(event.post.id)
            // If a post is successfully uploaded, we need to fetch the list again so it's id is added to ListStore
            if (!event.isError) {
                fetchFirstPage()
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMediaUploaded(event: OnMediaUploaded) {
        if (event.isError || event.canceled) {
            return
        }
        if (event.media == null || event.media.localPostId == 0 || site.id != event.media.localSiteId) {
            // Not interested in media not attached to posts or not belonging to the current site
            return
        }
        invalidateFeaturedMediaAndPagedListData(event.media.mediaId)
    }

    // EventBus Events

    @Suppress("unused")
    fun onEventBackgroundThread(event: UploadService.UploadErrorEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        if (event.post != null) {
            _postUploadAction.postValue(PostUploadedSnackbar(dispatcher, site, event.post, true, event.errorMessage))
        } else if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            _postUploadAction.postValue(MediaUploadedSnackbar(site, event.mediaModelList, true, event.errorMessage))
        }
    }

    @Suppress("unused")
    fun onEventBackgroundThread(event: UploadService.UploadMediaSuccessEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            _postUploadAction.postValue(MediaUploadedSnackbar(site, event.mediaModelList, false, event.successMessage))
        }
    }

    /**
     * Upload started, reload so correct status on uploading post appears
     */
    @Suppress("unused")
    fun onEventBackgroundThread(event: PostEvents.PostUploadStarted) {
        if (site.id == event.post.localSiteId) {
            invalidateUploadStatusAndPagedListData(event.post.id)
        }
    }

    /**
     * Upload cancelled (probably due to failed media), reload so correct status on uploading post appears
     */
    @Suppress("unused")
    fun onEventBackgroundThread(event: PostEvents.PostUploadCanceled) {
        if (site.id == event.post.localSiteId) {
            invalidateUploadStatusAndPagedListData(event.post.id)
        }
    }

    @Suppress("unused")
    fun onEventBackgroundThread(event: VideoOptimizer.ProgressEvent) {
        invalidateUploadStatusAndPagedListData(event.media.id)
    }

    @Suppress("unused")
    fun onEventBackgroundThread(event: UploadService.UploadMediaRetryEvent) {
        if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            // if there' a Post to which the retried media belongs, clear their status
            val postsToRefresh = PostUtils.getPostsThatIncludeAnyOfTheseMedia(postStore, event.mediaModelList)
            // now that we know which Posts to refresh, let's do it
            invalidateUploadStatusAndPagedListData(*postsToRefresh.map { it.id }.toIntArray())
        }
    }

    private fun getFeaturedImageUrl(featuredImageId: Long, postContent: String): String? {
        if (featuredImageId == 0L) {
            return ReaderImageScanner(postContent, !SiteUtils.isPhotonCapable(site)).largestImage
        }
        featuredImageMap[featuredImageId]?.let { return it }
        mediaStore.getSiteMediaWithId(site, featuredImageId)?.let { media ->
            // This should be a pretty rare case, but some media seems to be missing url
            return if (media.url != null) {
                featuredImageMap[featuredImageId] = media.url
                media.url
            } else null
        }
        // Media is not in the Store, we need to download it
        val mediaToDownload = MediaModel()
        mediaToDownload.mediaId = featuredImageId
        mediaToDownload.localSiteId = site.id
        val payload = MediaPayload(site, mediaToDownload)
        dispatcher.dispatch(MediaActionBuilder.newFetchMediaAction(payload))
        return null
    }

    private fun invalidateFeaturedMediaAndPagedListData(vararg featuredImageIds: Long) {
        featuredImageIds.forEach { featuredImageMap.remove(it) }
        pagedListWrapper.invalidateData()
    }

    private fun getUploadStatus(post: PostModel): PostListItemUploadStatus {
        uploadStatusMap[post.id]?.let { return it }
        val uploadError = uploadStore.getUploadErrorForPost(post)
        val isUploadingOrQueued = UploadService.isPostUploadingOrQueued(post)
        val hasInProgressMediaUpload = UploadService.hasInProgressMediaUploadsForPost(post)
        val newStatus = PostListItemUploadStatus(
                uploadError = uploadError,
                mediaUploadProgress = Math.round(UploadService.getMediaUploadProgressForPost(post) * 100),
                isUploading = UploadService.isPostUploading(post),
                isUploadingOrQueued = isUploadingOrQueued,
                isQueued = UploadService.isPostQueued(post),
                isUploadFailed = uploadStore.isFailedPost(post),
                hasInProgressMediaUpload = hasInProgressMediaUpload,
                hasPendingMediaUpload = UploadService.hasPendingMediaUploadsForPost(post)
        )
        uploadStatusMap[post.id] = newStatus
        return newStatus
    }

    private fun invalidateUploadStatusAndPagedListData(vararg localPostIds: Int) {
        localPostIds.forEach { uploadStatusMap.remove(it) }
        pagedListWrapper.invalidateData()
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

    fun scrollToPost(localPostId: LocalPostId) {
        val data = pagedListData.value
        if (data != null) {
            updateScrollPosition(data, localPostId)
        } else {
            // store the target post id and scroll there when the data is loaded
            scrollToLocalPostId = localPostId
        }
    }

    private fun onDataUpdated(data: PagedPostList) {
        val localPostId = scrollToLocalPostId
        if (localPostId != null) {
            scrollToLocalPostId = null
            updateScrollPosition(data, localPostId)
        }
    }

    private fun updateScrollPosition(data: PagedPostList, localPostId: LocalPostId) {
        val position = findItemListPosition(data, localPostId)
        position?.let {
            _scrollToPosition.value = it
        } ?: AppLog.e(AppLog.T.POSTS, "ScrollToPost failed - the post not found.")
    }

    private fun findItemListPosition(data: PagedPostList, localPostId: LocalPostId): Int? {
        return data.listIterator().withIndex().asSequence().find { listItem ->
            if (listItem.value is PostListItemUiState) {
                (listItem.value as PostListItemUiState).data.localPostId == localPostId
            } else {
                false
            }
        }?.index
    }

    private fun transformPostModelToPostListItemUiState(post: PostModel) =
            listItemUiStateHelper.createPostListItemUiState(
                    post = post,
                    uploadStatus = getUploadStatus(post),
                    unhandledConflicts = postConflictResolver.doesPostHaveUnhandledConflict(post),
                    capabilitiesToPublish = site.hasCapabilityPublishPosts,
                    statsSupported = isStatsSupported,
                    featuredImageUrl = getFeaturedImageUrl(
                            featuredImageId = post.featuredImageId,
                            postContent = post.content
                    ),
                    formattedDate = PostUtils.getFormattedDate(post),
                    performingCriticalAction = postActionHandler.isPerformingCriticalAction(LocalId(post.id))
            ) { postModel, buttonType, statEvent ->
                trackPostListAction(site, buttonType, postModel, statEvent)
                postActionHandler.handlePostButton(buttonType, postModel)
            }

    // Utils

    private fun checkNetworkConnection(): Boolean =
            if (networkUtilsWrapper.isNetworkAvailable()) {
                true
            } else {
                _toastMessage.postValue(ToastMessageHolder(R.string.no_network_message, Duration.SHORT))
                false
            }
}
