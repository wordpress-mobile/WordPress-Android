package org.wordpress.android.viewmodel.posts

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.paging.DataSource
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import android.content.Intent
import de.greenrobot.event.EventBus
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.apache.commons.text.StringEscapeUtils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListItemDataSource
import org.wordpress.android.fluxc.model.list.ListManager
import org.wordpress.android.fluxc.model.list.ListState
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForXmlRpcSite
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.ListStore.ListErrorType.PERMISSION_ERROR
import org.wordpress.android.fluxc.store.ListStore.OnListChanged
import org.wordpress.android.fluxc.store.ListStore.OnListChanged.CauseOfListChange.FIRST_PAGE_FETCHED
import org.wordpress.android.fluxc.store.ListStore.OnListItemsChanged
import org.wordpress.android.fluxc.store.ListStore.OnListStateChanged
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.fluxc.store.UploadStore.UploadError
import org.wordpress.android.modules.DEFAULT_SCOPE
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.EditPostActivity
import org.wordpress.android.ui.posts.PostAdapterItemPostData
import org.wordpress.android.ui.posts.PostAdapterItemType
import org.wordpress.android.ui.posts.PostAdapterItemType.PostAdapterItemEndListIndicator
import org.wordpress.android.ui.posts.PostAdapterItemType.PostAdapterItemLoading
import org.wordpress.android.ui.posts.PostAdapterItemType.PostAdapterItemPost
import org.wordpress.android.ui.posts.PostListData
import org.wordpress.android.ui.posts.PostListDataSource
import org.wordpress.android.ui.posts.PostListUserAction
import org.wordpress.android.ui.posts.PostPositionalDataSource
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
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.ToastUtils.Duration
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.DialogHolder
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder
import org.wordpress.android.viewmodel.posts.PostListEmptyViewState.EMPTY_LIST
import org.wordpress.android.viewmodel.posts.PostListEmptyViewState.HIDDEN_LIST
import org.wordpress.android.viewmodel.posts.PostListEmptyViewState.LOADING
import org.wordpress.android.widgets.PostListButton
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Named
import kotlin.system.measureTimeMillis

private const val INITIAL_LOAD_SIZE_HINT = 20
private const val PAGE_SIZE = 10

enum class PostListEmptyViewState {
    EMPTY_LIST,
    HIDDEN_LIST,
    LOADING,
    REFRESH_ERROR,
    PERMISSION_ERROR
}

class PostListViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val listStore: ListStore,
    private val uploadStore: UploadStore,
    private val mediaStore: MediaStore,
    private val postStore: PostStore,
    @Named(DEFAULT_SCOPE) private val defaultScope: CoroutineScope
) : ViewModel() {
    private val isAztecEditorEnabled = AppPrefs.isAztecEditorEnabled()
    private val isStatsSupported: Boolean by lazy {
        SiteUtils.isAccessedViaWPComRest(site) && site.hasCapabilityViewStats
    }
    private val isPhotonCapable: Boolean by lazy {
        SiteUtils.isPhotonCapable(site)
    }

    private val pagedListConfig = PagedList.Config.Builder()
            .setEnablePlaceholders(true)
            .setInitialLoadSizeHint(INITIAL_LOAD_SIZE_HINT)
            .setPageSize(PAGE_SIZE)
            .build()

    private var isStarted: Boolean = false
    private var listDescriptor: PostListDescriptor? = null
    private lateinit var site: SiteModel
    private val uploadedPostRemoteIds = ArrayList<Long>()

    private val _postListData = MutableLiveData<PostListData>()
    val postListData: LiveData<PostListData> = _postListData

    private val _emptyViewState = MutableLiveData<PostListEmptyViewState>()
    val emptyViewState: LiveData<PostListEmptyViewState> = _emptyViewState

    private val _userAction = SingleLiveEvent<PostListUserAction>()
    val userAction: LiveData<PostListUserAction> = _userAction

    private val _postUploadAction = SingleLiveEvent<PostUploadAction>()
    val postUploadAction: LiveData<PostUploadAction> = _postUploadAction

    private val _toastMessage = SingleLiveEvent<ToastMessageHolder>()
    val toastMessage: LiveData<ToastMessageHolder> = _toastMessage

    private val _dialogAction = SingleLiveEvent<DialogHolder>()
    val dialogAction: LiveData<DialogHolder> = _dialogAction

    private val _snackbarAction = SingleLiveEvent<SnackbarMessageHolder>()
    val snackbarAction: LiveData<SnackbarMessageHolder> = _snackbarAction

    val pagedListData: LiveData<PagedList<PostAdapterItemType>> by lazy {
        val dataSourceFactory = object : DataSource.Factory<Int, PostAdapterItemType>() {
            override fun create(): DataSource<Int, PostAdapterItemType> {
                // TODO: null cast !!
                return PostPositionalDataSource(postStore, site, listStore, listDescriptor!!) { remotePostId, post ->
                    if (post == null) {
                        PostAdapterItemLoading(remotePostId)
                    } else {
                        createPostAdapterItem(post)
                    }
                }
            }
        }
        LivePagedListBuilder<Int, PostAdapterItemType>(dataSourceFactory, pagedListConfig).build()
    }

    private var listManager: ListManager<PostModel>? = null
    private var listState: ListState? = null
    private var items: List<PostAdapterItemType>? = null

    init {
        EventBus.getDefault().register(this)
        dispatcher.register(this)
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        dispatcher.unregister(this)
        super.onCleared()
    }

    fun start(site: SiteModel) {
        if (isStarted) {
            return
        }
        this.site = site
        this.listDescriptor = if (site.isUsingWpComRestApi) {
            PostListDescriptorForRestSite(site)
        } else {
            PostListDescriptorForXmlRpcSite(site)
        }
        refreshListManagerFromStore(refreshFirstPageAfter = true)
        isStarted = true
    }

    fun refreshList() {
        listManager?.refresh()
    }

    fun handleEditPostResult(data: Intent?) {
        if (data == null) {
            return
        }
        val localPostId = data.getIntExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, 0)
        if (localPostId == 0) {
            return
        }
        val post = postStore.getPostByLocalPostId(localPostId)
        if (post == null) {
            if (!data.getBooleanExtra(EditPostActivity.EXTRA_IS_DISCARDABLE, false)) {
                // TODO: This really shouldn't happen, but shouldn't we refresh the post ourselves when it does?
                _toastMessage.postValue(ToastMessageHolder(R.string.post_not_found, Duration.LONG))
            }
        } else {
            _postUploadAction.postValue(EditPostResult(site, post, data) { publishPost(post) })
        }
    }

    fun newPost() {
        _userAction.postValue(PostListUserAction.NewPost(site))
    }

    private fun handlePostButton(buttonType: Int, post: PostModel) {
        when (buttonType) {
            PostListButton.BUTTON_EDIT -> editPost(site, post)
            PostListButton.BUTTON_RETRY -> _userAction.postValue(PostListUserAction.RetryUpload(post))
            PostListButton.BUTTON_SUBMIT, PostListButton.BUTTON_SYNC, PostListButton.BUTTON_PUBLISH -> {
                showPublishConfirmationDialog(post)
            }
            PostListButton.BUTTON_VIEW -> _userAction.postValue(PostListUserAction.ViewPost(site, post))
            PostListButton.BUTTON_PREVIEW -> _userAction.postValue(PostListUserAction.PreviewPost(site, post))
            PostListButton.BUTTON_STATS -> _userAction.postValue(PostListUserAction.ViewStats(site, post))
            PostListButton.BUTTON_TRASH, PostListButton.BUTTON_DELETE -> {
                showTrashConfirmationDialog(post)
            }
        }
    }

    private fun showTrashConfirmationDialog(post: PostModel) {
        val messageRes = if (!UploadService.isPostUploadingOrQueued(post)) {
            if (post.isLocalDraft) {
                R.string.dialog_confirm_delete_permanently_post
            } else R.string.dialog_confirm_delete_post
        } else {
            R.string.dialog_confirm_cancel_post_media_uploading
        }
        val dialogHolder = DialogHolder(
                titleRes = R.string.delete_post,
                messageRes = messageRes,
                positiveButtonTextRes = R.string.delete,
                negativeButtonTextRes = R.string.cancel,
                positiveButtonAction = { trashPost(post) }
        )
        _dialogAction.postValue(dialogHolder)
    }

    private fun showPublishConfirmationDialog(post: PostModel) {
        val dialogHolder = DialogHolder(
                titleRes = R.string.dialog_confirm_publish_title,
                messageRes = R.string.dialog_confirm_publish_message_post,
                positiveButtonTextRes = R.string.dialog_confirm_publish_yes,
                negativeButtonTextRes = R.string.cancel,
                positiveButtonAction = { publishPost(post) }
        )
        _dialogAction.postValue(dialogHolder)
    }

    private fun publishPost(post: PostModel) {
        _postUploadAction.postValue(PublishPost(dispatcher, site, post))
    }

    private fun editPost(site: SiteModel, post: PostModel) {
        // track event
        val properties = HashMap<String, Any>()
        properties["button"] = "edit"
        if (!post.isLocalDraft) {
            properties["post_id"] = post.remotePostId
        }
        properties[AnalyticsUtils.HAS_GUTENBERG_BLOCKS_KEY] = PostUtils.contentContainsGutenbergBlocks(post.content)
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.POST_LIST_BUTTON_PRESSED, site, properties)

        if (UploadService.isPostUploadingOrQueued(post)) {
            // If the post is uploading media, allow the media to continue uploading, but don't upload the
            // post itself when they finish (since we're about to edit it again)
            UploadService.cancelQueuedPostUpload(post)
        }
        _userAction.postValue(PostListUserAction.EditPost(site, post))
    }

    private fun trashPost(post: PostModel) {
        // TODO: Undo action
        // TODO: Remove the pending draft notification
        _postUploadAction.postValue(CancelPostAndMediaUpload(post))
        if (post.isLocalDraft) {
            dispatcher.dispatch(PostActionBuilder.newRemovePostAction(post))
        } else {
            dispatcher.dispatch(PostActionBuilder.newDeletePostAction(RemotePostPayload(post, site)))
        }

        // TODO: We should probably show this after we get the confirmation that the post is deleted/removed
        val messageRes = if (post.isLocalDraft) R.string.post_deleted else R.string.post_trashed
        val snackbarHolder = SnackbarMessageHolder(messageRes)
        _snackbarAction.postValue(snackbarHolder)
    }

    // FluxC Events

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onListChanged(event: OnListChanged) {
        listDescriptor?.let {
            if (!event.listDescriptors.contains(it)) {
                return
            }
            if (event.isError) {
                val emptyViewState = if (event.error.type == PERMISSION_ERROR) {
                    PostListEmptyViewState.PERMISSION_ERROR
                } else PostListEmptyViewState.REFRESH_ERROR
                _emptyViewState.postValue(emptyViewState)
            } else if (event.causeOfChange == FIRST_PAGE_FETCHED) {
                // `uploadedPostRemoteIds` is kept as a workaround when the local drafts are uploaded and the list
                // has not yet been updated yet. Since we just fetched the first page, we can safely clear it.
                // Please check out `onPostUploaded` for more context.
                uploadedPostRemoteIds.clear()
            }
            // We want to refresh the posts even if there is an error so we can get the state change
            refreshListManagerFromStore()
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onListItemsChanged(event: OnListItemsChanged) {
        if (listDescriptor?.typeIdentifier != event.type) {
            return
        }
        refreshListManagerFromStore()
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onListStateChanged(event: OnListStateChanged) {
        // There is no error to handle for `OnListStateChanged`
        listState = event.newState
        createAndDispatchPostListData()
    }

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
            updateFeaturedMediaAndDispatchChanges(event.mediaList)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostUploaded(event: OnPostUploaded) {
        if (event.post != null && event.post.localSiteId == site.id) {
            _postUploadAction.postValue(PostUploadedSnackbar(dispatcher, site, event.post, event.isError, null))
            // When a local draft is uploaded, it'll no longer be considered a local item by `ListManager` and it won't
            // be in the remote item list until the next refresh, which means it'll briefly disappear from the list.
            // This is not the behavior we want and to get around it, we'll keep the remote id of the post until the
            // next refresh and pass it to `ListStore` so it'll be included in the list.
            // Although the issue is related to local drafts, we can't check if uploaded post is local draft reliably
            // as the current `ListManager` might not have been updated yet since it's a bg action.
            uploadedPostRemoteIds.add(event.post.remotePostId)
            updateUploadStatus(event.post)
            // TODO: might not be the best way to start a refresh
            refreshList()
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
        updateFeaturedMediaAndDispatchChanges(listOf(event.media))
    }

    // EventBus

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
            updateUploadStatus(event.post)
            updatePostsAndDispatchChanges(setOf(event.post))
        }
    }

    /**
     * Upload cancelled (probably due to failed media), reload so correct status on uploading post appears
     */
    @Suppress("unused")
    fun onEventBackgroundThread(event: PostEvents.PostUploadCanceled) {
        if (site.id == event.post.localSiteId) {
            updateUploadStatus(event.post)
            updatePostsAndDispatchChanges(setOf(event.post))
        }
    }

    @Suppress("unused")
    fun onEventBackgroundThread(event: VideoOptimizer.ProgressEvent) {
        postStore.getPostByLocalPostId(event.media.localPostId)?.let { post ->
            updateUploadStatus(post)
            updatePostsAndDispatchChanges(setOf(post))
        }
    }

    @Suppress("unused")
    fun onEventBackgroundThread(event: UploadService.UploadMediaRetryEvent) {
        if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            // if there' a Post to which the retried media belongs, clear their status
            val postsToRefresh = PostUtils.getPostsThatIncludeAnyOfTheseMedia(postStore, event.mediaModelList)
            // now that we know which Posts to refresh, let's do it
            for (post in postsToRefresh) {
                updateUploadStatus(post)
            }
            updatePostsAndDispatchChanges(postsToRefresh)
        }
    }

    // ListManager

    /**
     * A helper function to load the current [ListManager] from [ListStore].
     *
     * @param refreshFirstPageAfter Whether the first page of the list should be fetched after its loaded
     */
    private fun refreshListManagerFromStore(refreshFirstPageAfter: Boolean = false) {
        listDescriptor?.let { listDescriptor ->
            defaultScope.launch {
                val timeTook = measureTimeMillis {
                    val listManager = getListManagerFromStore(listDescriptor)
                    this@PostListViewModel.listManager = listManager
                    updateAllItemsAndDispatchChanges(listManager)
                    if (refreshFirstPageAfter) {
                        listManager.refresh()
                    }
                }
                AppLog.e(T.POSTS, "Time took to updateUI: $timeTook ms!")
            }
        }
    }

    // TODO: Only dispatch changes if the data is updated
    // TODO: Don't use suspend functions all over, currently there is no queue or order, just a chaos
    private fun updateFeaturedMediaAndDispatchChanges(mediaList: List<MediaModel>) {
        defaultScope.launch {
            mediaList.forEach { featuredImageMap[it.mediaId] = it.url }

            items = items?.map { item ->
                if (item is PostAdapterItemPost) {
                    val mediaToUpdate = mediaList.find { it.mediaId == item.data.featuredImageId }
                    if (mediaToUpdate != null) {
                        PostAdapterItemPost(
                                data = item.data.new(mediaToUpdate.url),
                                onButtonClicked = item.onButtonClicked,
                                onSelected = item.onSelected
                        )
                    }
                }
                item
            }
            createAndDispatchPostListData()
        }
    }

    private fun updatePostsAndDispatchChanges(postSet: Set<PostModel>) {
        defaultScope.launch {
            items = items?.map { item ->
                if (item is PostAdapterItemPost) {
                    val postToUpdate = postSet.find { it.id == item.data.localPostId }
                    if (postToUpdate != null) {
                        return@map createPostAdapterItem(postToUpdate)
                    }
                }
                item
            }
            createAndDispatchPostListData()
        }
    }

    private fun updateAllItemsAndDispatchChanges(listManager: ListManager<PostModel>) {
        defaultScope.launch {
            items = createAdapterItems(listManager)
            createAndDispatchPostListData()
        }
    }

    private fun createAndDispatchPostListData() {
        defaultScope.launch {
            val data = PostListData(
                    items,
                    listManager,
                    listState,
                    isAztecEditorEnabled,
                    isPhotonCapable,
                    site.hasCapabilityPublishPosts
            )
            _postListData.postValue(data)
            updateEmptyViewState(data)
        }
    }

    private fun updateEmptyViewState(postListData: PostListData) {
        val state = if (postListData.size == 0) {
            if (postListData.isLoadingFirstPage) LOADING else EMPTY_LIST
        } else {
            HIDDEN_LIST
        }
        _emptyViewState.postValue(state)
    }

    /**
     * A helper function to load the [ListManager] for the given [ListDescriptor] from [ListStore].
     *
     * [ListStore] requires an instance of [ListItemDataSource] which is a way for us to tell [ListStore] and
     * [ListManager] how to take certain actions or how to access certain data.
     */
    private suspend fun getListManagerFromStore(listDescriptor: PostListDescriptor) = withContext(Dispatchers.Default) {
        listStore.getListManager(
                listDescriptor,
                PostListDataSource(dispatcher, postStore, site, null, uploadedPostRemoteIds)
        )
    }

    private val uploadStatusMap = ConcurrentHashMap<Int, PostAdapterItemUploadStatus>()
    private val featuredImageMap = ConcurrentHashMap<Long, String>()
    private val featuredImageFromContentMap = ConcurrentHashMap<String, String>()

    data class PostAdapterItemUploadStatus(
        val uploadError: UploadError?,
        val mediaUploadProgress: Int,
        val isUploading: Boolean,
        val isUploadingOrQueued: Boolean,
        val isQueued: Boolean,
        val isUploadFailed: Boolean,
        val hasInProgressMediaUpload: Boolean,
        val hasPendingMediaUpload: Boolean
    )

    // TODO: Make this cancelable
    private suspend fun createAdapterItems(listManager: ListManager<PostModel>?): List<PostAdapterItemType> =
            withContext(Dispatchers.Default) {
                if (listManager == null) return@withContext emptyList<PostAdapterItemType>()
                val items = (0..(listManager.size - 1)).map { index ->
                    val post = listManager.getItem(index, shouldFetchIfNull = false, shouldLoadMoreIfNecessary = false)
                    if (post == null) {
                        val remotePostId = requireNotNull(listManager.getRemoteItemId(index)) {
                            // TODO: Rework this in ListManager
                            "If the item is null, its remoteItemId has to be a valid id"
                        }
                        PostAdapterItemLoading(remotePostId)
                    } else {
                        createPostAdapterItem(post)
                    }
                }
                if (hasLoadedAllPosts()) {
                    items.plus(PostAdapterItemEndListIndicator)
                } else items
            }

    private fun createPostAdapterItem(post: PostModel): PostAdapterItemPost {
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
        val postData = PostAdapterItemPostData(
                localPostId = post.id,
                remotePostId = if (post.remotePostId != 0L) post.remotePostId else null,
                title = title,
                excerpt = excerpt,
                isLocalDraft = post.isLocalDraft,
                date = PostUtils.getFormattedDate(post),
                postStatus = postStatus,
                isLocallyChanged = post.isLocallyChanged,
                canShowStats = canShowStats,
                canPublishPost = canPublishPost,
                canRetryUpload = uploadStatus.uploadError != null && !uploadStatus.hasInProgressMediaUpload,
                featuredImageId = post.featuredImageId,
                featuredImageUrl = getFeaturedImageUrl(post.featuredImageId) ?: getFeaturedImageUrl(post.content),
                uploadStatus = uploadStatus
        )
        return PostAdapterItemPost(
                data = postData,
                onSelected = { handlePostButton(PostListButton.BUTTON_EDIT, post) },
                onButtonClicked = { handlePostButton(it, post) }
        )
    }

    private fun hasLoadedAllPosts(): Boolean {
        return if (items == null) {
            // If items is null, that means we haven't loaded the data yet, which means there is more data to be loaded
            false
        } else listState?.canLoadMore() == false
    }

    private fun getFeaturedImageUrl(postContent: String): String? {
        featuredImageFromContentMap[postContent]?.let { return it }
        val imageUrl = ReaderImageScanner(postContent, !SiteUtils.isPhotonCapable(site)).largestImage
        if (imageUrl != null) {
            featuredImageFromContentMap[postContent] = imageUrl
        }
        return imageUrl
    }

    private fun getFeaturedImageUrl(featuredImageId: Long): String? {
        if (featuredImageId == 0L) {
            return null
        }
        featuredImageMap[featuredImageId]?.let { return it }
        val media = mediaStore.getSiteMediaWithId(site, featuredImageId)
        media?.let {
            val mediaUrl = media.url
            featuredImageMap[featuredImageId] = mediaUrl
            return mediaUrl
        }
        // Media is not in the Store, we need to download it
        val mediaToDownload = MediaModel()
        mediaToDownload.mediaId = featuredImageId
        mediaToDownload.localSiteId = site.id
        val payload = MediaPayload(site, mediaToDownload)
        dispatcher.dispatch(MediaActionBuilder.newFetchMediaAction(payload))
        return null
    }

    private fun getUploadStatus(post: PostModel): PostAdapterItemUploadStatus {
        uploadStatusMap[post.id]?.let { return it }
        return updateUploadStatus(post)
    }

    private fun updateUploadStatus(post: PostModel): PostAdapterItemUploadStatus {
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
}
