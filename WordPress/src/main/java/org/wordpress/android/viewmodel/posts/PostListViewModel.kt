package org.wordpress.android.viewmodel.posts

import android.text.TextUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.posts.AuthorFilterSelection.EVERYONE
import org.wordpress.android.ui.posts.AuthorFilterSelection.ME
import org.wordpress.android.ui.posts.PostListType.SEARCH
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.posts.trackPostListAction
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.uploads.UploadStarter
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.ThrottleLiveData
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.posts.PostListEmptyUiState.RefreshError
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.LocalPostId
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState
import javax.inject.Inject
import javax.inject.Named
import kotlin.properties.Delegates

typealias PagedPostList = PagedList<PostListItemType>

private const val SEARCH_DELAY_MS = 500L
private const val SEARCH_PROGRESS_INDICATOR_DELAY_MS = 500L
private const val EMPTY_VIEW_THROTTLE = 250L

class PostListViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val listStore: ListStore,
    private val postStore: PostStore,
    private val accountStore: AccountStore,
    private val listItemUiStateHelper: PostListItemUiStateHelper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val uploadStarter: UploadStarter,
    private val readerUtilsWrapper: ReaderUtilsWrapper,
    private val uploadUtilsWrapper: UploadUtilsWrapper,
    @Named(UI_THREAD) private val uiDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    connectionStatus: LiveData<ConnectionStatus>
) : ScopedViewModel(uiDispatcher), LifecycleOwner {
    private val isStatsSupported: Boolean by lazy {
        SiteUtils.isAccessedViaWPComRest(connector.site) && connector.site.hasCapabilityViewStats
    }
    private var isStarted: Boolean = false
    private lateinit var connector: PostListViewModelConnector

    private var photonWidth by Delegates.notNull<Int>()
    private var photonHeight by Delegates.notNull<Int>()

    private var scrollToLocalPostId: LocalPostId? = null

    private val _scrollToPosition = SingleLiveEvent<Int>()
    val scrollToPosition: LiveData<Int> = _scrollToPosition

    private val dataSource: PostListItemDataSource by lazy {
        PostListItemDataSource(
                dispatcher = dispatcher,
                postStore = postStore,
                postFetcher = connector.postFetcher,
                transform = this::transformPostModelToPostListItemUiState,
                postListType = connector.postListType
        )
    }

    private var pagedListWrapper: PagedListWrapper<PostListItemType>? = null

    private val _pagedListData = MediatorLiveData<PagedPostList>()
    val pagedListData: LiveData<PagedPostList> = _pagedListData

    private val _emptyViewState = ThrottleLiveData<PostListEmptyUiState>(
            offset = EMPTY_VIEW_THROTTLE,
            coroutineScope = this,
            mainDispatcher = uiDispatcher,
            backgroundDispatcher = bgDispatcher
    )
    val emptyViewState: LiveData<PostListEmptyUiState> = _emptyViewState

    private val _isLoadingMore = MediatorLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _isFetchingFirstPage = MediatorLiveData<Boolean>()
    val isFetchingFirstPage: LiveData<Boolean> = _isFetchingFirstPage

    private var searchQuery: String? = null
    private var searchJob: Job? = null
    private var searchProgressJob: Job? = null
    private lateinit var authorFilterSelection: AuthorFilterSelection

    private val lifecycleRegistry = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    fun start(
        postListViewModelConnector: PostListViewModelConnector,
        value: AuthorFilterSelection,
        photonWidth: Int,
        photonHeight: Int
    ) {
        if (isStarted) {
            return
        }
        this.photonHeight = photonHeight
        this.photonWidth = photonWidth
        connector = postListViewModelConnector

        isStarted = true
        lifecycleRegistry.markState(Lifecycle.State.STARTED)

        if (connector.postListType == SEARCH) {
            this.authorFilterSelection = EVERYONE
        } else {
            this.authorFilterSelection = value
            /*
             * We don't want to initialize the list with empty search query in search mode as it'd send an unnecessary
             * request to fetch ids of all posts on the site.
             */
            initList(dataSource, lifecycle)
        }
    }

    private fun initList(dataSource: PostListItemDataSource, lifecycle: Lifecycle) {
        val listDescriptor: PostListDescriptor = initListDescriptor(searchQuery)

        clearLiveDataSources()

        val pagedListWrapper = listStore.getList(listDescriptor, dataSource, lifecycle)

        listenToEmptyViewStateLiveData(pagedListWrapper)

        _pagedListData.addSource(pagedListWrapper.data) { pagedPostList ->
            pagedPostList?.let {
                if (isSearchResultDeliverable()) {
                    onDataUpdated(it)
                    _pagedListData.value = it
                }
            }
        }
        _isFetchingFirstPage.addSource(pagedListWrapper.isFetchingFirstPage) {
            searchProgressJob?.cancel()
            if (it == true) {
                val delay = if (connector.postListType != SEARCH) {
                    0
                } else {
                    SEARCH_PROGRESS_INDICATOR_DELAY_MS
                }
                searchProgressJob = launch {
                    delay(delay)
                    searchProgressJob = null
                    if (isActive) {
                        _isFetchingFirstPage.value = true
                    }
                }
            } else {
                _isFetchingFirstPage.value = false
            }
        }
        _isLoadingMore.addSource(pagedListWrapper.isLoadingMore) {
            _isLoadingMore.value = it
        }

        this.pagedListWrapper = pagedListWrapper
        fetchFirstPage()
    }

    private fun clearLiveDataSources() {
        pagedListWrapper?.let {
            _pagedListData.removeSource(it.data)
            _emptyViewState.removeSource(pagedListData)
            _emptyViewState.removeSource(it.isEmpty)
            _emptyViewState.removeSource(it.isFetchingFirstPage)
            _emptyViewState.removeSource(it.listError)
            _isFetchingFirstPage.removeSource(it.isFetchingFirstPage)
            _isLoadingMore.removeSource(it.isLoadingMore)
        }
    }

    private fun initListDescriptor(searchQuery: String?): PostListDescriptor {
        return if (connector.site.isUsingWpComRestApi) {
            val author: AuthorFilter = when (authorFilterSelection) {
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

    private fun listenToEmptyViewStateLiveData(pagedListWrapper: PagedListWrapper<PostListItemType>) {
        val update = {
            createEmptyUiState(
                    postListType = connector.postListType,
                    isNetworkAvailable = networkUtilsWrapper.isNetworkAvailable(),
                    isLoadingData = pagedListWrapper.isFetchingFirstPage.value ?: false ||
                            pagedListWrapper.data.value == null,
                    isListEmpty = pagedListWrapper.isEmpty.value ?: true,
                    isSearchPromptRequired = isEmptySearch(),
                    error = pagedListWrapper.listError.value,
                    fetchFirstPage = this::fetchFirstPage,
                    newPost = connector.postActionHandler::newPost
            )
        }

        _emptyViewState.addSource(pagedListWrapper.isEmpty) { _emptyViewState.postValue(update()) }
        _emptyViewState.addSource(pagedListWrapper.isFetchingFirstPage) { _emptyViewState.postValue(update()) }
        _emptyViewState.addSource(pagedListWrapper.listError) { _emptyViewState.postValue(update()) }
    }

    // used to filter out dataset changes that might trigger empty view when performing search
    private fun isSearchResultDeliverable(): Boolean {
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

    fun search(query: String?, delay: Long = SEARCH_DELAY_MS) {
        if (searchQuery == query) {
            return
        }
        searchJob?.cancel()
        searchProgressJob?.cancel()
        searchQuery = query
        if (TextUtils.isEmpty(query)) {
            clearLiveDataSources()
            pagedListWrapper = null
            _isFetchingFirstPage.value = false
            showEmptySearchPrompt()
        } else {
            searchJob = launch {
                delay(delay)
                searchJob = null
                if (isActive) {
                    initList(dataSource, lifecycle)
                }
            }
        }
    }

    private fun showEmptySearchPrompt() {
        _emptyViewState.value = createEmptyUiState(
                postListType = SEARCH,
                isNetworkAvailable = networkUtilsWrapper.isNetworkAvailable(),
                isLoadingData = false,
                isListEmpty = true,
                isSearchPromptRequired = true,
                error = null,
                fetchFirstPage = this@PostListViewModel::fetchFirstPage,
                newPost = connector.postActionHandler::newPost
        )
    }

    override fun onCleared() {
        lifecycleRegistry.markState(Lifecycle.State.DESTROYED)
        super.onCleared()
    }

    // Public Methods

    fun swipeToRefresh() {
        uploadStarter.queueUploadFromSite(connector.site)
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

    private fun transformPostModelToPostListItemUiState(post: PostModel): PostListItemUiState {
        val hasAutoSave = connector.hasAutoSave(post)
        return listItemUiStateHelper.createPostListItemUiState(
                authorFilterSelection,
                post = post,
                site = connector.site,
                unhandledConflicts = connector.doesPostHaveUnhandledConflict(post),
                hasAutoSave = hasAutoSave,
                capabilitiesToPublish = uploadUtilsWrapper.userCanPublish(connector.site),
                statsSupported = isStatsSupported,
                featuredImageUrl =
                convertToPhotonUrlIfPossible(connector.getFeaturedImageUrl(post.featuredImageId)),
                formattedDate = PostUtils.getFormattedDate(post),
                performingCriticalAction = connector.postActionHandler.isPerformingCriticalAction(LocalId(post.id)),
                onAction = { postModel, buttonType, statEvent ->
                    trackPostListAction(connector.site, buttonType, postModel, statEvent)
                    connector.postActionHandler.handlePostButton(buttonType, postModel, hasAutoSave)
                },
                uploadStatusTracker = connector.uploadStatusTracker,
                isSearch = connector.postListType == SEARCH
        )
    }

    private fun retryOnConnectionAvailableAfterRefreshError() {
        val connectionAvailableAfterRefreshError = networkUtilsWrapper.isNetworkAvailable() &&
                emptyViewState.value is RefreshError

        if (connectionAvailableAfterRefreshError) {
            fetchFirstPage()
        }
    }

    private fun convertToPhotonUrlIfPossible(featuredImageUrl: String?): String? =
            readerUtilsWrapper.getResizedImageUrl(
                    featuredImageUrl,
                    photonWidth,
                    photonHeight,
                    !SiteUtils.isPhotonCapable(connector.site),
                    connector.site.isPrivateWPComAtomic
            )

    fun updateAuthorFilterIfNotSearch(authorFilterSelection: AuthorFilterSelection): Boolean {
        if (connector.postListType != SEARCH && this.authorFilterSelection != authorFilterSelection) {
            this.authorFilterSelection = authorFilterSelection
            initList(dataSource, lifecycle)
            return true
        }
        return false
    }
}
