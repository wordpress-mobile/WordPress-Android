package org.wordpress.android.viewmodel.posts

import android.annotation.SuppressLint
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Observer
import android.arch.paging.PagedList
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.list.AuthorFilter
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForXmlRpcSite
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.posts.AuthorFilterSelection.EVERYONE
import org.wordpress.android.ui.posts.AuthorFilterSelection.ME
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.posts.trackPostListAction
import org.wordpress.android.ui.uploads.LocalDraftUploadStarter
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.posts.PostListEmptyUiState.RefreshError
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.LocalPostId
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState
import javax.inject.Inject
import javax.inject.Named

typealias PagedPostList = PagedList<PostListItemType>

@SuppressLint("UseSparseArrays")
class PostListViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val listStore: ListStore,
    private val postStore: PostStore,
    private val accountStore: AccountStore,
    private val listItemUiStateHelper: PostListItemUiStateHelper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val localDraftUploadStarter: LocalDraftUploadStarter,
    connectionStatus: LiveData<ConnectionStatus>,
    @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher), LifecycleOwner {
    private val isStatsSupported: Boolean by lazy {
        SiteUtils.isAccessedViaWPComRest(connector.site) && connector.site.hasCapabilityViewStats
    }
    private var isStarted: Boolean = false
    private var listDescriptor: PostListDescriptor? = null
    private lateinit var connector: PostListViewModelConnector

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
                postFetcher = connector.postFetcher,
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
                    postListType = connector.postListType,
                    isNetworkAvailable = networkUtilsWrapper.isNetworkAvailable(),
                    isLoadingData = pagedListWrapper.isFetchingFirstPage.value ?: false ||
                            pagedListWrapper.data.value == null,
                    isListEmpty = pagedListWrapper.isEmpty.value ?: true,
                    error = pagedListWrapper.listError.value,
                    fetchFirstPage = this::fetchFirstPage,
                    newPost = connector.postActionHandler::newPost
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

    fun start(postListViewModelConnector: PostListViewModelConnector) {
        if (isStarted) {
            return
        }
        connector = postListViewModelConnector

        this.listDescriptor = if (connector.site.isUsingWpComRestApi) {
            val author: AuthorFilter = when (postListViewModelConnector.authorFilter) {
                ME -> AuthorFilter.SpecificAuthor(accountStore.account.userId)
                EVERYONE -> AuthorFilter.Everyone
            }

            PostListDescriptorForRestSite(
                    site = connector.site,
                    statusList = connector.postListType.postStatuses,
                    author = author
            )
        } else {
            PostListDescriptorForXmlRpcSite(site = connector.site, statusList = connector.postListType.postStatuses)
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

    fun swipeToRefresh() {
        localDraftUploadStarter.queueUpload(site = connector.site)
        fetchFirstPage()
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

    private fun fetchFirstPage() {
        pagedListWrapper.fetchFirstPage()
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
                    uploadStatus = connector.getUploadStatus(post),
                    unhandledConflicts = connector.doesPostHaveUnhandledConflict(post),
                    capabilitiesToPublish = connector.site.hasCapabilityPublishPosts,
                    statsSupported = isStatsSupported,
                    featuredImageUrl = connector.getFeaturedImageUrl(
                            post.featuredImageId,
                            post.content
                    ),
                    formattedDate = PostUtils.getFormattedDate(post),
                    performingCriticalAction = connector.postActionHandler.isPerformingCriticalAction(LocalId(post.id)),
                    onAction = { postModel, buttonType, statEvent ->
                        trackPostListAction(connector.site, buttonType, postModel, statEvent)
                        connector.postActionHandler.handlePostButton(buttonType, postModel)
                    }
            )

    private fun retryOnConnectionAvailableAfterRefreshError() {
        val connectionAvailableAfterRefreshError = networkUtilsWrapper.isNetworkAvailable() &&
                emptyViewState.value is RefreshError

        if (connectionAvailableAfterRefreshError) {
            fetchFirstPage()
        }
    }
}
