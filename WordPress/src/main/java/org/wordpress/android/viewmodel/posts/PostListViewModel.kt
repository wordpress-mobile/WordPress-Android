package org.wordpress.android.viewmodel.posts

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Intent
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListItemDataSource
import org.wordpress.android.fluxc.model.list.ListManager
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForXmlRpcSite
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.ListStore.ListErrorType.PERMISSION_ERROR
import org.wordpress.android.fluxc.store.ListStore.OnListChanged
import org.wordpress.android.fluxc.store.ListStore.OnListChanged.CauseOfListChange.FIRST_PAGE_FETCHED
import org.wordpress.android.fluxc.store.ListStore.OnListItemsChanged
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.modules.DEFAULT_SCOPE
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.EditPostActivity
import org.wordpress.android.ui.posts.PostListDataSource
import org.wordpress.android.ui.posts.PostUploadAction
import org.wordpress.android.ui.posts.PostUploadAction.CancelPostAndMediaUpload
import org.wordpress.android.ui.posts.PostUploadAction.EditPostResult
import org.wordpress.android.ui.posts.PostUploadAction.MediaUploadedSnackbar
import org.wordpress.android.ui.posts.PostUploadAction.PostUploadedSnackbar
import org.wordpress.android.ui.posts.PostUploadAction.PublishPost
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.uploads.PostEvents
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.VideoOptimizer
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.ToastUtils.Duration
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder
import org.wordpress.android.viewmodel.posts.PostListEmptyViewState.EMPTY_LIST
import org.wordpress.android.viewmodel.posts.PostListEmptyViewState.HIDDEN_LIST
import org.wordpress.android.viewmodel.posts.PostListEmptyViewState.LOADING
import org.wordpress.android.widgets.PostListButton
import javax.inject.Inject
import javax.inject.Named

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
    private val postStore: PostStore,
    @Named(DEFAULT_SCOPE) private val defaultScope: CoroutineScope
) : ViewModel() {
    private var isStarted: Boolean = false
    private var listDescriptor: PostListDescriptor? = null
    private lateinit var site: SiteModel

    private val uploadedPostRemoteIds = ArrayList<Long>()
    private val trashedPostIds = ArrayList<Pair<Int, Long>>()

    private val _listManager = MutableLiveData<ListManager<PostModel>>()
    val listManagerLiveData: LiveData<ListManager<PostModel>> = _listManager

    private val _emptyViewState = MutableLiveData<PostListEmptyViewState>()
    val emptyViewState: LiveData<PostListEmptyViewState> = _emptyViewState

    private val _editPost = SingleLiveEvent<Pair<SiteModel, PostModel>>()
    val editPost: LiveData<Pair<SiteModel, PostModel>> = _editPost

    private val _retryPost = SingleLiveEvent<PostModel>()
    val retryPost: LiveData<PostModel> = _retryPost

    private val _viewStats = SingleLiveEvent<Pair<SiteModel, PostModel>>()
    val viewStats: LiveData<Pair<SiteModel, PostModel>> = _viewStats

    private val _previewPost = SingleLiveEvent<Pair<SiteModel, PostModel>>()
    val previewPost: LiveData<Pair<SiteModel, PostModel>> = _previewPost

    private val _viewPost = SingleLiveEvent<Pair<SiteModel, PostModel>>()
    val viewPost: LiveData<Pair<SiteModel, PostModel>> = _viewPost

    private val _newPost = SingleLiveEvent<SiteModel>()
    val newPost: LiveData<SiteModel> = _newPost

    private val _displayTrashConfirmationDialog = SingleLiveEvent<PostModel>()
    val displayTrashConfirmationDialog: LiveData<PostModel> = _displayTrashConfirmationDialog

    private val _displayPublishConfirmationDialog = SingleLiveEvent<PostModel>()
    val displayPublishConfirmationDialog: LiveData<PostModel> = _displayPublishConfirmationDialog

    private val _postUploadAction = SingleLiveEvent<PostUploadAction>()
    val postUploadAction: LiveData<PostUploadAction> = _postUploadAction

    private val _toastMessage = SingleLiveEvent<ToastMessageHolder>()
    val toastMessage: LiveData<ToastMessageHolder> = _toastMessage

    private val _postDetailsUpdated = SingleLiveEvent<PostModel>()
    val postDetailsUpdated: LiveData<PostModel> = _postDetailsUpdated

    private val _mediaChanged = SingleLiveEvent<List<MediaModel>>()
    val mediaChanged: LiveData<List<MediaModel>> = _mediaChanged

    private val _snackbarAction = SingleLiveEvent<SnackbarMessageHolder>()
    val snackbarAction: LiveData<SnackbarMessageHolder> = _snackbarAction

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
        listManagerLiveData.value?.refresh()
    }

    fun handlePostButton(buttonType: Int, post: PostModel) {
        when (buttonType) {
            PostListButton.BUTTON_EDIT -> editPost(site, post)
            PostListButton.BUTTON_RETRY -> _retryPost.postValue(post)
            PostListButton.BUTTON_SUBMIT, PostListButton.BUTTON_SYNC, PostListButton.BUTTON_PUBLISH -> {
                _displayPublishConfirmationDialog.postValue(post)
            }
            PostListButton.BUTTON_VIEW -> _viewPost.postValue(Pair(site, post))
            PostListButton.BUTTON_PREVIEW -> _previewPost.postValue(Pair(site, post))
            PostListButton.BUTTON_STATS -> _viewStats.postValue(Pair(site, post))
            PostListButton.BUTTON_TRASH, PostListButton.BUTTON_DELETE -> {
                _displayTrashConfirmationDialog.postValue(post)
            }
        }
    }

    fun publishPost(post: PostModel) {
        _postUploadAction.postValue(PublishPost(dispatcher, site, post))
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
        _newPost.postValue(site)
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
        _editPost.postValue(Pair(site, post))
    }

    fun trashPost(post: PostModel) {
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

    private fun addUploadedPostRemoteId(remotePostId: Long) {
        uploadedPostRemoteIds.add(remotePostId)
    }

    fun onEventMainThread(event: UploadService.UploadErrorEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        if (event.post != null) {
            _postUploadAction.postValue(PostUploadedSnackbar(dispatcher, site, event.post, true, event.errorMessage))
        } else if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            _postUploadAction.postValue(MediaUploadedSnackbar(site, event.mediaModelList, true, event.errorMessage))
        }
    }

    fun onEventMainThread(event: UploadService.UploadMediaSuccessEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            _postUploadAction.postValue(MediaUploadedSnackbar(site, event.mediaModelList, false, event.successMessage))
        }
    }

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
            addUploadedPostRemoteId(event.post.remotePostId)
            // TODO: might not be the best way to start a refresh
            refreshList()
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
    fun onMediaUploaded(event: OnMediaUploaded) {
        if (event.isError || event.canceled) {
            return
        }
        if (event.media == null || event.media.localPostId == 0 || site.id != event.media.localSiteId) {
            // Not interested in media not attached to posts or not belonging to the current site
            return
        }
        postStore.getPostByLocalPostId(event.media.localPostId)?.let { post ->
            _postDetailsUpdated.postValue(post)
        }
    }

    /**
     * Upload started, reload so correct status on uploading post appears
     */
    fun onEventMainThread(event: PostEvents.PostUploadStarted) {
        if (site.id == event.post.localSiteId) {
            _postDetailsUpdated.postValue(event.post)
        }
    }

    /**
     * Upload cancelled (probably due to failed media), reload so correct status on uploading post appears
     */
    fun onEventMainThread(event: PostEvents.PostUploadCanceled) {
        if (site.id == event.post.localSiteId) {
            _postDetailsUpdated.postValue(event.post)
        }
    }

    fun onEventMainThread(event: VideoOptimizer.ProgressEvent) {
        postStore.getPostByLocalPostId(event.media.localPostId)?.let { post ->
            _postDetailsUpdated.postValue(post)
        }
    }

    fun onEventMainThread(event: UploadService.UploadMediaRetryEvent) {
        if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            // if there' a Post to which the retried media belongs, clear their status
            val postsToRefresh = PostUtils.getPostsThatIncludeAnyOfTheseMedia(postStore, event.mediaModelList)
            // now that we know which Posts to refresh, let's do it
            for (post in postsToRefresh) {
                _postDetailsUpdated.postValue(post)
            }
        }
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

    /**
     * Media info for a post's featured image has been downloaded, tell
     * the adapter so it can show the featured image now that we have its URL
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMediaChanged(event: OnMediaChanged) {
        if (!event.isError && event.mediaList != null) {
            _mediaChanged.postValue(event.mediaList)
        }
    }

    /**
     * A helper function to load the current [ListManager] from [ListStore].
     *
     * @param refreshFirstPageAfter Whether the first page of the list should be fetched after its loaded
     */
    private fun refreshListManagerFromStore(refreshFirstPageAfter: Boolean = false) {
        listDescriptor?.let {
            defaultScope.launch {
                val listManager = getListManagerFromStore(it)
                _listManager.postValue(listManager)
                updateEmptyViewState(listManager)
                if (refreshFirstPageAfter) {
                    listManager.refresh()
                }
            }
        }
    }

    private fun updateEmptyViewState(listManager: ListManager<PostModel>) {
        val state = if (listManager.size == 0) {
            if (listManager.isFetchingFirstPage) LOADING else EMPTY_LIST
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
                PostListDataSource(dispatcher, postStore, site, trashedPostIds, uploadedPostRemoteIds)
        )
    }
}
