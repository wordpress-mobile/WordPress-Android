package org.wordpress.android.viewmodel.posts

import android.annotation.SuppressLint
import android.text.TextUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.list.AuthorFilter
import org.wordpress.android.fluxc.model.list.AuthorFilter.Everyone
import org.wordpress.android.fluxc.model.list.AuthorFilter.SpecificAuthor
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForXmlRpcSite
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.posts.AuthorFilterSelection.EVERYONE
import org.wordpress.android.ui.posts.AuthorFilterSelection.ME
import org.wordpress.android.ui.posts.PostListType.SEARCH
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.posts.trackPostListAction
import org.wordpress.android.ui.uploads.LocalDraftUploadStarter
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

@SuppressLint("UseSparseArrays")
class PostListViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val listStore: ListStore,
    private val postStore: PostStore,
    private val accountStore: AccountStore,
    private val listItemUiStateHelper: PostListItemUiStateHelper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val localDraftUploadStarter: LocalDraftUploadStarter,
    connectionStatus: LiveData<ConnectionStatus>
) : ViewModel(), LifecycleOwner {
    private val isStatsSupported: Boolean by lazy {
        SiteUtils.isAccessedViaWPComRest(connector.site) && connector.site.hasCapabilityViewStats
    }
    private var isStarted: Boolean = false
    private lateinit var connector: PostListViewModelConnector

    private var scrollToLocalPostId: LocalPostId? = null
    private var consumeEmptySearchListEvent: Boolean = true

    private val _scrollToPosition = SingleLiveEvent<Int>()
    val scrollToPosition: LiveData<Int> = _scrollToPosition

    private val dataSource: PostListItemDataSource by lazy {
        PostListItemDataSource(
                dispatcher = dispatcher,
                postStore = postStore,
                postFetcher = connector.postFetcher,
                transform = this::transformPostModelToPostListItemUiState
        )
    }

    val pagedListData: LiveData<PagedPostList> = MediatorLiveData<PagedPostList>()
    val emptyViewState: LiveData<PostListEmptyUiState> = MediatorLiveData<PostListEmptyUiState>()
    val isLoadingMore: LiveData<Boolean> = MediatorLiveData<Boolean>()
    val isFetchingFirstPage: LiveData<Boolean> = MediatorLiveData<Boolean>()

    private var searchQuery: String? = null

    private var pagedListWrapper: PagedListWrapper<PostListItemType>? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    fun start(postListViewModelConnector: PostListViewModelConnector) {
        if (isStarted) {
            return
        }
        connector = postListViewModelConnector

        initList(null, dataSource, lifecycle)

        isStarted = true
        lifecycleRegistry.markState(Lifecycle.State.STARTED)
        fetchFirstPage()
    }

    private fun initList(query: String?, dataSource: PostListItemDataSource, lifecycle: Lifecycle) {
        searchQuery = query
        val listDescriptor: PostListDescriptor = initListDescriptor(searchQuery)

        pagedListWrapper?.let {
            clearPagedListData(it)
            clearEmptyViewStateLiveData(it)
            clearFetchingFirstPage(it)
            clearLoadingMore(it)
        }
        val pagedListWrapper = listStore.getList(listDescriptor, dataSource, lifecycle)
        listenToEmptyViewStateLiveData(pagedListWrapper)
        if (!isEmptySearch()) {
            listenToPagedListDate(pagedListWrapper)
            listenToFetchingFirstPage(pagedListWrapper)
            listenToIsLoadingMore(pagedListWrapper)
        }

        this.pagedListWrapper = pagedListWrapper
    }

    private fun initListDescriptor(searchQuery: String?): PostListDescriptor {
        return if (connector.site.isUsingWpComRestApi) {
            val author: AuthorFilter = when (connector.authorFilter) {
                ME -> SpecificAuthor(accountStore.account.userId)
                EVERYONE -> Everyone
            }

            PostListDescriptorForRestSite(
                    site = connector.site,
                    statusList = connector.postListType.postStatuses,
                    author = author,
                    searchQuery = searchQuery
            )
        } else {
            PostListDescriptorForXmlRpcSite(site = connector.site, statusList = connector.postListType.postStatuses)
        }
    }

    private fun listenToIsLoadingMore(pagedListWrapper: PagedListWrapper<PostListItemType>) {
        val isLoadingMore: MediatorLiveData<Boolean> = isLoadingMore as MediatorLiveData<Boolean>
        isLoadingMore.addSource(pagedListWrapper.isLoadingMore) {
            this.isLoadingMore.value = it
        }
    }

    private fun clearLoadingMore(pagedListWrapper: PagedListWrapper<PostListItemType>) {
        val isLoadingMore: MediatorLiveData<Boolean> = isLoadingMore as MediatorLiveData<Boolean>
        isLoadingMore.removeSource(pagedListWrapper.isLoadingMore)
    }

    private fun listenToFetchingFirstPage(pagedListWrapper: PagedListWrapper<PostListItemType>) {
        val isFetchingFirstPage: MediatorLiveData<Boolean> = isFetchingFirstPage as MediatorLiveData<Boolean>
        isFetchingFirstPage.addSource(pagedListWrapper.isFetchingFirstPage) {
            this.isFetchingFirstPage.value = it
        }
    }

    private fun clearFetchingFirstPage(pagedListWrapper: PagedListWrapper<PostListItemType>) {
        val isFetchingFirstPage: MediatorLiveData<Boolean> = isFetchingFirstPage as MediatorLiveData<Boolean>
        isFetchingFirstPage.removeSource(pagedListWrapper.isFetchingFirstPage)
    }

    private fun listenToPagedListDate(pagedListWrapper: PagedListWrapper<PostListItemType>) {
        val pagedListData: MediatorLiveData<PagedPostList> = pagedListData as MediatorLiveData<PagedPostList>
        pagedListData.addSource(pagedListWrapper.data) { pagedPostList ->
            pagedPostList?.let {
                if (isResultDeliverable()) {
                    consumeEmptySearchListEvent = false
                    onDataUpdated(it)
                    pagedListData.value = it
                }
            }
        }
    }

    private fun clearPagedListData(pagedListWrapper: PagedListWrapper<PostListItemType>) {
        val pagedListData: MediatorLiveData<PagedPostList> = pagedListData as MediatorLiveData<PagedPostList>
        pagedListData.removeSource(pagedListWrapper.data)
    }

    private fun listenToEmptyViewStateLiveData(pagedListWrapper: PagedListWrapper<PostListItemType>) {
        val emptyViewState: MediatorLiveData<PostListEmptyUiState> = emptyViewState as MediatorLiveData<PostListEmptyUiState>
        val update = {
            val isListEmpty = when {
                consumeEmptySearchListEvent && connector.postListType == SEARCH && !isEmptySearch() -> false
                isEmptySearch() -> true
                else -> pagedListWrapper.isEmpty.value ?: true
            }
            val isLoadingData = !isEmptySearch() &&
                    (pagedListWrapper.isFetchingFirstPage.value ?: false || pagedListWrapper.data.value == null)

            createEmptyUiState(
                    postListType = connector.postListType,
                    isNetworkAvailable = networkUtilsWrapper.isNetworkAvailable(),
                    isLoadingData = isLoadingData,
                    isListEmpty = isListEmpty,
                    isSearchPromptRequired = isEmptySearch(),
                    error = pagedListWrapper.listError.value,
                    fetchFirstPage = this::fetchFirstPage,
                    newPost = connector.postActionHandler::newPost
            )
        }

        if (connector.postListType == SEARCH) {
            if (!isEmptySearch()) {
                emptyViewState.addSource(pagedListData) { emptyViewState.value = update() }
                emptyViewState.addSource(pagedListWrapper.isEmpty) { emptyViewState.value = update() }
            } else {
                emptyViewState.value = update()
            }
        } else {
            emptyViewState.addSource(pagedListWrapper.isEmpty) { emptyViewState.value = update() }
            emptyViewState.addSource(pagedListWrapper.isFetchingFirstPage) { emptyViewState.value = update() }
            emptyViewState.addSource(pagedListWrapper.listError) { emptyViewState.value = update() }
        }
    }

    private fun clearEmptyViewStateLiveData(pagedListWrapper: PagedListWrapper<PostListItemType>) {
        val emptyViewState: MediatorLiveData<PostListEmptyUiState> = emptyViewState as MediatorLiveData<PostListEmptyUiState>
        emptyViewState.removeSource(pagedListData)
        emptyViewState.removeSource(pagedListWrapper.isEmpty)
        emptyViewState.removeSource(pagedListWrapper.isFetchingFirstPage)
        emptyViewState.removeSource(pagedListWrapper.listError)
    }

    private fun isResultDeliverable(): Boolean {
        return connector.postListType != SEARCH ||
                (connector.postListType == SEARCH &&
                        pagedListWrapper?.isFetchingFirstPage?.value != null &&
                        isFetchingFirstPage.value == false)
    }

    private fun isEmptySearch(): Boolean {
        return TextUtils.isEmpty(searchQuery) && connector.postListType == SEARCH
    }

    init {
        connectionStatus.observe(this, Observer {
            retryOnConnectionAvailableAfterRefreshError()
        })
        lifecycleRegistry.markState(Lifecycle.State.CREATED)
    }

    fun search(query: String?) {
        consumeEmptySearchListEvent = true
        initList(query, dataSource, lifecycle)
        fetchFirstPage()
    }

    override fun onCleared() {
        lifecycleRegistry.markState(Lifecycle.State.DESTROYED)
        super.onCleared()
    }

    // Public Methods

    fun swipeToRefresh() {
        localDraftUploadStarter.queueUploadFromSite(connector.site)
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
        pagedListWrapper?.fetchFirstPage()
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
