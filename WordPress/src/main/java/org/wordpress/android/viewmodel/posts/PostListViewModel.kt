package org.wordpress.android.viewmodel.posts

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.Dispatcher
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
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.modules.DEFAULT_SCOPE
import org.wordpress.android.modules.UI_SCOPE
import org.wordpress.android.ui.posts.PostListDataSource
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.viewmodel.SingleLiveEvent
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
    @Named(UI_SCOPE) private val uiScope: CoroutineScope,
    @Named(DEFAULT_SCOPE) private val defaultScope: CoroutineScope
) : ViewModel() {
    private var isStarted: Boolean = false
    private var listDescriptor: PostListDescriptor? = null
    private var site: SiteModel? = null

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

    private val _displayPublishConfirmationDialog = SingleLiveEvent<Pair<SiteModel, PostModel>>()
    val displayPublishConfirmationDialog: LiveData<Pair<SiteModel, PostModel>> = _displayPublishConfirmationDialog

    init {
//        EventBus.getDefault().register(this)
        dispatcher.register(this)
    }

    override fun onCleared() {
//        EventBus.getDefault().unregister(this)
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
        // Site shouldn't be null at this point, but this simplifies our approach
        site?.let { site ->
            when (buttonType) {
                PostListButton.BUTTON_EDIT -> editPost(site, post)
                PostListButton.BUTTON_RETRY -> _retryPost.postValue(post)
                PostListButton.BUTTON_SUBMIT, PostListButton.BUTTON_SYNC, PostListButton.BUTTON_PUBLISH -> {
                    _displayPublishConfirmationDialog.postValue(Pair(site, post))
                }
                PostListButton.BUTTON_VIEW -> _viewPost.postValue(Pair(site, post))
                PostListButton.BUTTON_PREVIEW -> _previewPost.postValue(Pair(site, post))
                PostListButton.BUTTON_STATS -> _viewStats.postValue(Pair(site, post))
                PostListButton.BUTTON_TRASH, PostListButton.BUTTON_DELETE -> {
                    _displayTrashConfirmationDialog.postValue(post)
                }
            }
        }
    }

    fun newPost() {
        site?.let {
            _newPost.postValue(it)
        }
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

    fun addUploadedPostRemoteId(remotePostId: Long) {
        uploadedPostRemoteIds.add(remotePostId)
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

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onListItemsChanged(event: OnListItemsChanged) {
        if (listDescriptor?.typeIdentifier != event.type) {
            return
        }
        refreshListManagerFromStore()
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
