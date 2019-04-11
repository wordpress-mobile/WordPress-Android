package org.wordpress.android.viewmodel.posts

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.paging.PagedList
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.AuthorFilter
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForXmlRpcSite
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.posts.AuthorFilterSelection.EVERYONE
import org.wordpress.android.ui.posts.AuthorFilterSelection.ME
import org.wordpress.android.ui.posts.PostListType
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.posts.trackPostListAction
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.posts.PostListEmptyUiState.RefreshError
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.LocalPostId
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState
import javax.inject.Inject

typealias PagedPostList = PagedList<PostListItemType>

class PostListViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val listStore: ListStore,
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

    private var scrollToLocalPostId: LocalPostId? = null

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
                    // TODO!!!
                    newPost = {}//postActionHandler::newPost
            )
        }
        result.addSource(pagedListWrapper.isEmpty) { result.value = update() }
        result.addSource(pagedListWrapper.isFetchingFirstPage) { result.value = update() }
        result.addSource(pagedListWrapper.listError) { result.value = update() }
        result
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle = lifecycleRegistry

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

        isStarted = true
        lifecycleRegistry.markState(Lifecycle.State.STARTED)
        fetchFirstPage()
    }

    override fun onCleared() {
        lifecycleRegistry.markState(Lifecycle.State.DESTROYED)
        super.onCleared()
    }

    // Public Methods

    fun fetchFirstPage() {
        pagedListWrapper.fetchFirstPage()
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

    // Utils

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

    private val dummyUploadStatus = PostListItemUploadStatus(
            uploadError = null,
            mediaUploadProgress = 0,
            isUploading = false,
            isUploadingOrQueued = false,
            isQueued = false,
            isUploadFailed = false,
            hasInProgressMediaUpload = false,
            hasPendingMediaUpload = false
    )

    private fun transformPostModelToPostListItemUiState(post: PostModel) =
    // TODO!!!
            listItemUiStateHelper.createPostListItemUiState(
                    post = post,
                    uploadStatus = dummyUploadStatus,//uploadStatesTracker.getUploadStatus(post),
                    unhandledConflicts = false, //postConflictResolver.doesPostHaveUnhandledConflict(post),
                    capabilitiesToPublish = site.hasCapabilityPublishPosts,
                    statsSupported = isStatsSupported,
                    featuredImageUrl = null, //featuredImageTracker.getFeaturedImageUrl(
//                            site = site,
//                            featuredImageId = post.featuredImageId,
//                            postContent = post.content
//                    ),
                    formattedDate = PostUtils.getFormattedDate(post),
                    performingCriticalAction = false //postActionHandler.isPerformingCriticalAction(LocalId(post.id))
            ) { postModel, buttonType, statEvent ->
                trackPostListAction(site, buttonType, postModel, statEvent)
//                postActionHandler.handlePostButton(buttonType, postModel)
            }

    private fun retryOnConnectionAvailableAfterRefreshError() {
        val connectionAvailableAfterRefreshError = networkUtilsWrapper.isNetworkAvailable() &&
                emptyViewState.value is RefreshError

        if (connectionAvailableAfterRefreshError) {
            fetchFirstPage()
        }
    }
}
