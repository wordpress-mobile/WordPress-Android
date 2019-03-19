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
import org.apache.commons.text.StringEscapeUtils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.PagedListItemType
import org.wordpress.android.fluxc.model.list.PagedListItemType.ReadyItem
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForXmlRpcSite
import org.wordpress.android.fluxc.model.list.datastore.PostListDataStore
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.ListStore.ListErrorType
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.notifications.utils.PendingDraftsNotificationsUtils
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.EditPostActivity
import org.wordpress.android.ui.posts.PostAdapterItem
import org.wordpress.android.ui.posts.PostAdapterItemData
import org.wordpress.android.ui.posts.PostAdapterItemUploadStatus
import org.wordpress.android.ui.posts.PostListAction
import org.wordpress.android.ui.posts.PostListAction.DismissPendingNotification
import org.wordpress.android.ui.posts.PostUploadAction
import org.wordpress.android.ui.posts.PostUploadAction.CancelPostAndMediaUpload
import org.wordpress.android.ui.posts.PostUploadAction.EditPostResult
import org.wordpress.android.ui.posts.PostUploadAction.MediaUploadedSnackbar
import org.wordpress.android.ui.posts.PostUploadAction.PostUploadedSnackbar
import org.wordpress.android.ui.posts.PostUploadAction.PublishPost
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.reader.utils.ReaderImageScanner
import org.wordpress.android.ui.uploads.PostEvents
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.VideoOptimizer
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.ToastUtils.Duration
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.helpers.DialogHolder
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder
import org.wordpress.android.widgets.PostListButton
import javax.inject.Inject

const val CONFIRM_DELETE_POST_DIALOG_TAG = "CONFIRM_DELETE_POST_DIALOG_TAG"
const val CONFIRM_PUBLISH_POST_DIALOG_TAG = "CONFIRM_PUBLISH_POST_DIALOG_TAG"
const val CONFIRM_ON_CONFLICT_LOAD_REMOTE_POST_DIALOG_TAG = "CONFIRM_ON_CONFLICT_LOAD_REMOTE_POST_DIALOG_TAG"

enum class PostListEmptyViewState {
    EMPTY_LIST,
    HIDDEN_LIST,
    LOADING,
    REFRESH_ERROR,
    PERMISSION_ERROR
}

typealias PagedPostList = PagedList<PagedListItemType<PostAdapterItem>>

class PostListViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val listStore: ListStore,
    private val uploadStore: UploadStore,
    private val mediaStore: MediaStore,
    private val postStore: PostStore,
    connectionStatus: LiveData<ConnectionStatus>
) : ViewModel(), LifecycleOwner {
    private val isStatsSupported: Boolean by lazy {
        SiteUtils.isAccessedViaWPComRest(site) && site.hasCapabilityViewStats
    }
    private var isStarted: Boolean = false
    private var listDescriptor: PostListDescriptor? = null
    private lateinit var site: SiteModel

    // Cache upload statuses and featured images for posts for quicker access
    private val uploadStatusMap = HashMap<Int, PostAdapterItemUploadStatus>()
    private val featuredImageMap = HashMap<Long, String>()

    // Keep a reference to the currently being trashed post, so we can hide it during Undo Snackbar
    private var postIdToTrash: Pair<Int, Long>? = null
    // Since we are using DialogFragments we need to hold onto which post will be published or trashed / resolved
    private var localPostIdForPublishDialog: Int? = null
    private var localPostIdForTrashDialog: Int? = null
    private var localPostIdForConflictResolutionDialog: Int? = null
    private var originalPostCopyForConflictUndo: PostModel? = null
    private var localPostIdForFetchingRemoteVersionOfConflictedPost: Int? = null
    // Initial target post to scroll to
    private var targetLocalPostId: Int? = null

    private val _postListAction = SingleLiveEvent<PostListAction>()
    val postListAction: LiveData<PostListAction> = _postListAction

    private val _postUploadAction = SingleLiveEvent<PostUploadAction>()
    val postUploadAction: LiveData<PostUploadAction> = _postUploadAction

    private val _toastMessage = SingleLiveEvent<ToastMessageHolder>()
    val toastMessage: LiveData<ToastMessageHolder> = _toastMessage

    private val _dialogAction = SingleLiveEvent<DialogHolder>()
    val dialogAction: LiveData<DialogHolder> = _dialogAction

    private val _snackbarAction = SingleLiveEvent<SnackbarMessageHolder>()
    val snackbarAction: LiveData<SnackbarMessageHolder> = _snackbarAction

    private val pagedListWrapper: PagedListWrapper<PostAdapterItem> by lazy {
        val listDescriptor = requireNotNull(listDescriptor) {
            "ListDescriptor needs to be initialized before this is observed!"
        }
        val dataStore = PostListDataStore(dispatcher, postStore, site) { descriptor ->
            if (descriptor is PostListDescriptor && !descriptor.statusList.contains(PostStatus.TRASHED)) {
                postIdToTrash?.let { listOf(it) } ?: emptyList()
            } else emptyList()
        }
        listStore.getList(listDescriptor, dataStore, lifecycle) { post ->
            createPostAdapterItem(post)
        }
    }

    val isFetchingFirstPage: LiveData<Boolean> by lazy { pagedListWrapper.isFetchingFirstPage }
    val isLoadingMore: LiveData<Boolean> by lazy { pagedListWrapper.isLoadingMore }
    // Since we can only scroll to a post when the data is loaded, we are keeping the information together
    val pagedListDataAndScrollPosition: LiveData<Pair<PagedPostList, Int?>> by lazy {
        val result = MediatorLiveData<Pair<PagedPostList, Int?>>()
        result.addSource(pagedListWrapper.data) { pagedListData ->
            pagedListData?.let { list ->
                if (targetLocalPostId == null) {
                    result.value = Pair(list, null)
                    return@let
                }
                val scrollIndex = list.listIterator().withIndex().asSequence().find { listItem ->
                    if (listItem.value is ReadyItem<PostAdapterItem>) {
                        val readyItem = listItem.value as ReadyItem<PostAdapterItem>
                        readyItem.item.data.localPostId == targetLocalPostId
                    } else {
                        false
                    }
                }?.let {
                    targetLocalPostId = null
                    it.index
                }
                result.value = Pair(list, scrollIndex)
            }
        }
        result
    }
    val emptyViewState: LiveData<PostListEmptyViewState> by lazy {
        val result = MediatorLiveData<PostListEmptyViewState>()
        val update = {
            val error = pagedListWrapper.listError.value
            if (pagedListWrapper.isEmpty.value != false) {
                if (error != null) {
                    if (error.type == ListErrorType.PERMISSION_ERROR) {
                        PostListEmptyViewState.PERMISSION_ERROR
                    } else PostListEmptyViewState.REFRESH_ERROR
                } else if (pagedListWrapper.isFetchingFirstPage.value == true) {
                    PostListEmptyViewState.LOADING
                } else {
                    PostListEmptyViewState.EMPTY_LIST
                }
            } else {
                PostListEmptyViewState.HIDDEN_LIST
            }
        }
        result.addSource(pagedListWrapper.isEmpty) { result.value = update() }
        result.addSource(pagedListWrapper.isFetchingFirstPage) { result.value = update() }
        result.addSource(pagedListWrapper.listError) { result.value = update() }
        result
    }

    private var isNetworkAvailable: Boolean = true
    private val lifecycleRegistry = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    // Lifecycle

    init {
        connectionStatus.observe(this, Observer {
            isNetworkAvailable = it?.isConnected == true
        })
        lifecycleRegistry.markState(Lifecycle.State.CREATED)
    }

    fun start(site: SiteModel, targetLocalPostId: Int?) {
        if (isStarted) {
            return
        }
        this.site = site
        this.listDescriptor = if (site.isUsingWpComRestApi) {
            PostListDescriptorForRestSite(site)
        } else {
            PostListDescriptorForXmlRpcSite(site)
        }
        // We want to update the target post only for the first time ViewModel is started
        this.targetLocalPostId = targetLocalPostId

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
        val localPostId = data?.getIntExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, 0)
        if (localPostId == null || localPostId == 0) {
            return
        }
        val post = postStore.getPostByLocalPostId(localPostId)
        if (post != null) {
            _postUploadAction.postValue(EditPostResult(site, post, data) { publishPost(localPostId) })
        }
    }

    fun newPost() {
        _postListAction.postValue(PostListAction.NewPost(site))
    }

    // Private List Actions

    private fun handlePostButton(buttonType: Int, post: PostModel) {
        when (buttonType) {
            PostListButton.BUTTON_EDIT -> editPostButtonAction(site, post)
            PostListButton.BUTTON_RETRY -> _postListAction.postValue(PostListAction.RetryUpload(post))
            PostListButton.BUTTON_SUBMIT, PostListButton.BUTTON_SYNC, PostListButton.BUTTON_PUBLISH -> {
                showPublishConfirmationDialog(post)
            }
            PostListButton.BUTTON_VIEW -> _postListAction.postValue(PostListAction.ViewPost(site, post))
            PostListButton.BUTTON_PREVIEW -> _postListAction.postValue(PostListAction.PreviewPost(site, post))
            PostListButton.BUTTON_STATS -> _postListAction.postValue(PostListAction.ViewStats(site, post))
            PostListButton.BUTTON_TRASH, PostListButton.BUTTON_DELETE -> {
                showTrashConfirmationDialog(post)
            }
        }
    }

    private fun showTrashConfirmationDialog(post: PostModel) {
        if (postIdToTrash != null) {
            // We can only handle one trash action at once due to be able to undo
            return
        }
        // We need network connection to delete a remote post, but not a local draft
        if (!post.isLocalDraft && !checkNetworkConnection()) {
            return
        }
        val messageRes = if (!UploadService.isPostUploadingOrQueued(post)) {
            if (post.isLocalDraft) {
                R.string.dialog_confirm_delete_permanently_post
            } else R.string.dialog_confirm_delete_post
        } else {
            R.string.dialog_confirm_cancel_post_media_uploading
        }
        val dialogHolder = DialogHolder(
                tag = CONFIRM_DELETE_POST_DIALOG_TAG,
                title = UiStringRes(R.string.delete_post),
                message = UiStringRes(messageRes),
                positiveButton = UiStringRes(R.string.delete),
                negativeButton = UiStringRes(R.string.cancel)
        )
        localPostIdForTrashDialog = post.id
        _dialogAction.postValue(dialogHolder)
    }

    private fun showPublishConfirmationDialog(post: PostModel) {
        if (localPostIdForPublishDialog != null) {
            // We can only handle one publish dialog at once
            return
        }
        if (!checkNetworkConnection()) {
            return
        }
        val dialogHolder = DialogHolder(
                tag = CONFIRM_PUBLISH_POST_DIALOG_TAG,
                title = UiStringRes(R.string.dialog_confirm_publish_title),
                message = UiStringRes(string.dialog_confirm_publish_message_post),
                positiveButton = UiStringRes(R.string.dialog_confirm_publish_yes),
                negativeButton = UiStringRes(R.string.cancel)
        )
        localPostIdForPublishDialog = post.id
        _dialogAction.postValue(dialogHolder)
    }

    private fun showConflictedPostResolutionDialog(post: PostModel) {
        val dialogHolder = DialogHolder(
                tag = CONFIRM_ON_CONFLICT_LOAD_REMOTE_POST_DIALOG_TAG,
                title = UiStringRes(R.string.dialog_confirm_load_remote_post_title),
                message = UiStringText(PostUtils.getConflictedPostCustomStringForDialog(post)),
                positiveButton = UiStringRes(R.string.dialog_confirm_load_remote_post_discard_local),
                negativeButton = UiStringRes(R.string.dialog_confirm_load_remote_post_discard_web)
        )
        localPostIdForConflictResolutionDialog = post.id
        _dialogAction.postValue(dialogHolder)
    }

    private fun publishPost(localPostId: Int) {
        val post = postStore.getPostByLocalPostId(localPostId)
        if (post != null) {
            _postUploadAction.postValue(PublishPost(dispatcher, site, post))
        }
        localPostIdForPublishDialog = null
    }

    private fun isGutenbergEnabled(): Boolean {
        return BuildConfig.OFFER_GUTENBERG && AppPrefs.isGutenbergEditorEnabled()
    }

    private fun editPostButtonAction(site: SiteModel, post: PostModel) {
        // first of all, check whether this post is in Conflicted state.
        if (doesPostHaveUnhandledConflict(post)) {
            showConflictedPostResolutionDialog(post)
            return
        }

        editPost(site, post)
    }

    private fun editPost(site: SiteModel, post: PostModel) {
        if (UploadService.isPostUploadingOrQueued(post)) {
            // If the post is uploading media, allow the media to continue uploading, but don't upload the
            // post itself when they finish (since we're about to edit it again)
            UploadService.cancelQueuedPostUpload(post)
        }
        _postListAction.postValue(PostListAction.EditPost(site, post))
    }

    private fun trashPost(localPostId: Int) {
        // If post doesn't exist, nothing else to do
        val post = postStore.getPostByLocalPostId(localPostId) ?: return
        postIdToTrash = Pair(post.id, post.remotePostId)
        // Refresh the list so we can immediately hide the post
        pagedListWrapper.invalidateData()
        val undoAction = {
            postIdToTrash = null
            // Refresh the list, so we can re-show the post
            pagedListWrapper.invalidateData()
        }
        val onDismissAction = {
            // If postIdToTrash is set to `null`, user undid the action
            if (postIdToTrash != null) {
                postIdToTrash = null
                _postUploadAction.postValue(CancelPostAndMediaUpload(post))
                if (post.isLocalDraft) {
                    val pushId = PendingDraftsNotificationsUtils.makePendingDraftNotificationId(post.id)
                    _postListAction.postValue(DismissPendingNotification(pushId))
                    dispatcher.dispatch(PostActionBuilder.newRemovePostAction(post))
                } else {
                    dispatcher.dispatch(PostActionBuilder.newDeletePostAction(RemotePostPayload(post, site)))
                }
            }
        }
        val messageRes = if (post.isLocalDraft) R.string.post_deleted else R.string.post_trashed
        val snackbarHolder = SnackbarMessageHolder(messageRes, R.string.undo, undoAction, onDismissAction)
        _snackbarAction.postValue(snackbarHolder)
    }

    // FluxC Events

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
                    originalPostCopyForConflictUndo?.id?.let {
                        val updatedPost = postStore.getPostByLocalPostId(it)
                        // Conflicted post has been successfully updated with its remote version
                        if (!PostUtils.isPostInConflictWithRemote(updatedPost)) {
                            conflictedPostUpdatedWithItsRemoteVersion()
                        }
                    }
                }
            }
            is CauseOfOnPostChanged.DeletePost -> {
                if (event.isError) {
                    _toastMessage.postValue(ToastMessageHolder(R.string.error_deleting_post, Duration.SHORT))
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMediaChanged(event: OnMediaChanged) {
        if (!event.isError && event.mediaList != null) {
            invalidateFeaturedMediaAndPagedListData(*event.mediaList.map { it.mediaId }.toLongArray())
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
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

    // PostAdapterItem Management

    private fun createPostAdapterItem(post: PostModel): PostAdapterItem {
        val title = if (post.title.isNotBlank()) {
            StringEscapeUtils.unescapeHtml4(post.title)
        } else null
        val excerpt = PostUtils.getPostListExcerptFromPost(post).takeIf { !it.isNullOrBlank() }
                ?.let { StringEscapeUtils.unescapeHtml4(it) }.let { PostUtils.collapseShortcodes(it) }
        val postStatus = PostStatus.fromPost(post)
        val canShowStats = isStatsSupported && postStatus == PostStatus.PUBLISHED && !post.isLocalDraft &&
                !post.isLocallyChanged
        val uploadStatus = getUploadStatus(post)
        val canPublishPost = !uploadStatus.isUploadingOrQueued &&
                (post.isLocallyChanged || post.isLocalDraft || postStatus == PostStatus.DRAFT)
        val postData = PostAdapterItemData(
                localPostId = post.id,
                remotePostId = if (post.remotePostId != 0L) post.remotePostId else null,
                title = title,
                excerpt = excerpt,
                isLocalDraft = post.isLocalDraft,
                date = PostUtils.getFormattedDate(post),
                postStatus = postStatus,
                isLocallyChanged = post.isLocallyChanged,
                isConflicted = doesPostHaveUnhandledConflict(post),
                canShowStats = canShowStats,
                canPublishPost = canPublishPost,
                canRetryUpload = uploadStatus.uploadError != null && !uploadStatus.hasInProgressMediaUpload,
                featuredImageId = post.featuredImageId,
                featuredImageUrl = getFeaturedImageUrl(post.featuredImageId, post.content),
                uploadStatus = uploadStatus
        )

        return PostAdapterItem(
                data = postData,
                onSelected = {
                    trackAction(PostListButton.BUTTON_EDIT, post, AnalyticsTracker.Stat.POST_LIST_ITEM_SELECTED)
                    handlePostButton(PostListButton.BUTTON_EDIT, post)
                },
                onButtonClicked = {
                    trackAction(it, post, AnalyticsTracker.Stat.POST_LIST_BUTTON_PRESSED)
                    handlePostButton(it, post)
                }
        )
    }

    private fun trackAction(buttonType: Int, postData: PostModel, statsEvent: Stat) {
        val properties = HashMap<String, Any?>()
        if (!postData.isLocalDraft) {
            properties["post_id"] = postData.remotePostId
        }

        properties["action"] = when (buttonType) {
            PostListButton.BUTTON_EDIT -> {
                properties[AnalyticsUtils.HAS_GUTENBERG_BLOCKS_KEY] = PostUtils
                        .contentContainsGutenbergBlocks(postData.content)
                "edit"
            }
            PostListButton.BUTTON_RETRY -> "retry"
            PostListButton.BUTTON_SUBMIT -> "submit"
            PostListButton.BUTTON_VIEW -> "view"
            PostListButton.BUTTON_PREVIEW -> "preview"
            PostListButton.BUTTON_STATS -> "stats"
            PostListButton.BUTTON_TRASH -> "trash"
            PostListButton.BUTTON_DELETE -> "delete"
            PostListButton.BUTTON_PUBLISH -> "publish"
            PostListButton.BUTTON_SYNC -> "sync"
            PostListButton.BUTTON_MORE -> "more"
            PostListButton.BUTTON_BACK -> "back"
            else -> AppLog.e(AppLog.T.POSTS, "Unknown button type")
        }

        AnalyticsUtils.trackWithSiteDetails(statsEvent, site, properties)
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

    private fun getUploadStatus(post: PostModel): PostAdapterItemUploadStatus {
        uploadStatusMap[post.id]?.let { return it }
        val uploadError = uploadStore.getUploadErrorForPost(post)
        val isUploadingOrQueued = UploadService.isPostUploadingOrQueued(post)
        val hasInProgressMediaUpload = UploadService.hasInProgressMediaUploadsForPost(post)
        val newStatus = PostAdapterItemUploadStatus(
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
        when (instanceTag) {
            CONFIRM_DELETE_POST_DIALOG_TAG -> localPostIdForTrashDialog?.let {
                localPostIdForTrashDialog = null
                trashPost(it)
            }
            CONFIRM_PUBLISH_POST_DIALOG_TAG -> localPostIdForPublishDialog?.let {
                localPostIdForPublishDialog = null
                publishPost(it)
            }
            CONFIRM_ON_CONFLICT_LOAD_REMOTE_POST_DIALOG_TAG -> localPostIdForConflictResolutionDialog?.let {
                localPostIdForConflictResolutionDialog = null
                // here load version from remote
                updateConflictedPostWithItsRemoteVersion(it)
            }
            else -> throw IllegalArgumentException("Dialog's positive button click is not handled: $instanceTag")
        }
    }

    fun onNegativeClickedForBasicDialog(instanceTag: String) {
        when (instanceTag) {
            CONFIRM_DELETE_POST_DIALOG_TAG -> localPostIdForTrashDialog = null
            CONFIRM_PUBLISH_POST_DIALOG_TAG -> localPostIdForPublishDialog = null
            CONFIRM_ON_CONFLICT_LOAD_REMOTE_POST_DIALOG_TAG -> localPostIdForConflictResolutionDialog?.let {
                updateConflictedPostWithItsLocalVersion(it)
            }
            else -> throw IllegalArgumentException("Dialog's negative button click is not handled: $instanceTag")
        }
    }

    fun onDismissByOutsideTouchForBasicDialog(instanceTag: String) {
        // Cancel and outside touch dismiss works the same way for all, except for conflict resolution dialog,
        // for which tapping outside and actively tapping the "edit local" have different meanings
        if (instanceTag != CONFIRM_ON_CONFLICT_LOAD_REMOTE_POST_DIALOG_TAG) {
            onNegativeClickedForBasicDialog(instanceTag)
        }
    }

    // Post Conflict Resolution

    private fun updateConflictedPostWithItsRemoteVersion(localPostId: Int) {
        // We need network connection to load a remote post
        if (!checkNetworkConnection()) {
            return
        }

        val post = postStore.getPostByLocalPostId(localPostId)
        if (post != null) {
            originalPostCopyForConflictUndo = post.clone()
            dispatcher.dispatch(PostActionBuilder.newFetchPostAction(RemotePostPayload(post, site)))
            _toastMessage.postValue(ToastMessageHolder(R.string.toast_conflict_updating_post, Duration.SHORT))
        }
    }

    private fun conflictedPostUpdatedWithItsRemoteVersion() {
        val undoAction = {
            // here replace the post with whatever we had before, again
            if (originalPostCopyForConflictUndo != null) {
                dispatcher.dispatch(PostActionBuilder.newUpdatePostAction(originalPostCopyForConflictUndo))
            }
        }
        val onDismissAction = {
            originalPostCopyForConflictUndo = null
        }
        val snackbarHolder = SnackbarMessageHolder(
                R.string.snackbar_conflict_local_version_discarded,
                R.string.snackbar_conflict_undo, undoAction, onDismissAction
        )
        _snackbarAction.postValue(snackbarHolder)
    }

    private fun updateConflictedPostWithItsLocalVersion(localPostId: Int) {
        // We need network connection to push local version to remote
        if (!checkNetworkConnection()) {
            return
        }

        // Keep a reference to which post is being updated with the local version so we can avoid showing the conflicted
        // label during the undo snackbar.
        localPostIdForFetchingRemoteVersionOfConflictedPost = localPostId
        pagedListWrapper.invalidateData()

        val post = postStore.getPostByLocalPostId(localPostId) ?: return

        // and now show a snackbar, acting as if the Post was pushed, but effectively push it after the snackbar is gone
        var isUndoed = false
        val undoAction = {
            isUndoed = true

            // Remove the reference for the post being updated and re-show the conflicted label on undo
            localPostIdForFetchingRemoteVersionOfConflictedPost = null
            pagedListWrapper.invalidateData()
        }

        val onDismissAction = {
            if (!isUndoed) {
                localPostIdForFetchingRemoteVersionOfConflictedPost = null
                PostUtils.trackSavePostAnalytics(post, site)
                dispatcher.dispatch(PostActionBuilder.newPushPostAction(RemotePostPayload(post, site)))
            }
        }
        val snackbarHolder = SnackbarMessageHolder(
                R.string.snackbar_conflict_web_version_discarded,
                R.string.snackbar_conflict_undo, undoAction, onDismissAction
        )
        _snackbarAction.postValue(snackbarHolder)
    }

    // Utils

    private fun doesPostHaveUnhandledConflict(post: PostModel): Boolean {
        // If we are fetching the remote version of a conflicted post, it means it's already being handled
        val isFetchingConflictedPost = localPostIdForFetchingRemoteVersionOfConflictedPost != null &&
                localPostIdForFetchingRemoteVersionOfConflictedPost == post.id
        return !isFetchingConflictedPost && PostUtils.isPostInConflictWithRemote(post)
    }

    private fun checkNetworkConnection(): Boolean =
            if (isNetworkAvailable) {
                true
            } else {
                _toastMessage.postValue(ToastMessageHolder(R.string.no_network_message, Duration.SHORT))
                false
            }
}
