package org.wordpress.android.posttypes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.posttypes.bridge.BridgeConstants
import org.wordpress.android.posttypes.bridge.SiteReference
import org.wordpress.android.posttypes.bridge.WpSelfHostedServiceFactory
import rs.wordpress.cache.kotlin.ObservableMetadataCollection
import rs.wordpress.cache.kotlin.getObservablePostMetadataCollectionWithEditContext
import rs.wordpress.cache.kotlin.hasMorePages
import rs.wordpress.cache.kotlin.isSyncing
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.parsePostStatus
import uniffi.wp_mobile.ListInfo
import uniffi.wp_mobile.PostItemState
import uniffi.wp_mobile.PostListFilter
import uniffi.wp_mobile.PostMetadataCollectionItem
import uniffi.wp_mobile.WpSelfHostedService
import javax.inject.Inject

/**
 * Convert a string identifier to PostEndpointType.
 *
 * @see PostEndpointTypeId for the standard identifiers.
 */
private fun String.toPostEndpointType(): PostEndpointType = when (this) {
    PostEndpointTypeId.POSTS -> PostEndpointType.Posts
    PostEndpointTypeId.PAGES -> PostEndpointType.Pages
    else -> PostEndpointType.Custom(this)
}

/**
 * Represents a filter option for the post list.
 */
enum class PostStatusFilter(val displayName: String, val apiValue: String?) {
    ALL("All", null),
    PUBLISHED("Published", "publish"),
    DRAFT("Draft", "draft")
}

/**
 * Display data for a post item with its fetch state.
 */
data class CptPostListItem(
    val id: Long,
    val title: String,
    val excerpt: String,
    val status: String,
    val itemState: PostItemState = PostItemState.Missing,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        fun fromCollectionItem(item: PostMetadataCollectionItem): CptPostListItem {
            val state = item.state
            return when (state) {
                is PostItemState.Missing -> CptPostListItem(
                    id = item.id,
                    itemState = state,
                    title = "Post ${item.id}",
                    excerpt = "",
                    status = ""
                )
                is PostItemState.Fetching -> CptPostListItem(
                    id = item.id,
                    itemState = state,
                    title = "Post ${item.id}",
                    excerpt = "",
                    status = "",
                    isLoading = true
                )
                is PostItemState.FetchingWithData -> CptPostListItem(
                    id = item.id,
                    itemState = state,
                    title = state.data.data.title.rendered,
                    excerpt = state.data.data.content.rendered.take(EXCERPT_MAX_LENGTH),
                    status = state.data.data.status.toString().replaceFirstChar { it.uppercase() },
                    isLoading = true
                )
                is PostItemState.Cached -> CptPostListItem(
                    id = item.id,
                    itemState = state,
                    title = state.data.data.title.rendered,
                    excerpt = state.data.data.content.rendered.take(EXCERPT_MAX_LENGTH),
                    status = state.data.data.status.toString().replaceFirstChar { it.uppercase() }
                )
                is PostItemState.Stale -> CptPostListItem(
                    id = item.id,
                    itemState = state,
                    title = state.data.data.title.rendered,
                    excerpt = state.data.data.content.rendered.take(EXCERPT_MAX_LENGTH),
                    status = state.data.data.status.toString().replaceFirstChar { it.uppercase() }
                )
                is PostItemState.Failed -> CptPostListItem(
                    id = item.id,
                    itemState = state,
                    title = "Post ${item.id}",
                    excerpt = "",
                    status = "",
                    errorMessage = state.error
                )
                is PostItemState.FailedWithData -> CptPostListItem(
                    id = item.id,
                    itemState = state,
                    title = state.data.data.title.rendered,
                    excerpt = state.data.data.content.rendered.take(EXCERPT_MAX_LENGTH),
                    status = state.data.data.status.toString().replaceFirstChar { it.uppercase() },
                    errorMessage = state.error
                )
            }
        }

        private const val EXCERPT_MAX_LENGTH = 100
    }
}

/**
 * UI state for the flat post list screen.
 */
data class CptFlatPostListUiState(
    val postTypeLabel: String = "",
    val posts: List<CptPostListItem> = emptyList(),
    val currentFilter: PostStatusFilter = PostStatusFilter.ALL,
    val listInfo: ListInfo? = null,
    val errorMessage: String? = null
) {
    /**
     * Whether a sync operation is in progress.
     * Derived from listInfo.state - the single source of truth from the database.
     */
    val isSyncing: Boolean
        get() = listInfo?.isSyncing ?: false

    val hasMorePages: Boolean
        get() = listInfo?.hasMorePages ?: true

    val currentPage: Long
        get() = listInfo?.currentPage?.toLong() ?: 0L
}

@HiltViewModel
class CptFlatPostListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    serviceFactory: WpSelfHostedServiceFactory
) : ViewModel() {
    private val site: SiteReference? = savedStateHandle.get<SiteReference>(BridgeConstants.EXTRA_SITE)

    @Suppress("unused") // Will be used for post type specific queries
    private val postTypeSlug: String = savedStateHandle.get<String>(
        CptFlatPostListActivity.EXTRA_POST_TYPE_SLUG
    ).orEmpty()

    private val postTypeLabel: String = savedStateHandle.get<String>(
        CptFlatPostListActivity.EXTRA_POST_TYPE_LABEL
    ).orEmpty()

    private val endpointType: PostEndpointType = savedStateHandle.get<String>(
        CptFlatPostListActivity.EXTRA_ENDPOINT_TYPE_ID
    )?.toPostEndpointType() ?: PostEndpointType.Posts

    private val selfHostedService: WpSelfHostedService? = site?.let {
        serviceFactory.create(it.id, it.url)
    }

    private val _uiState = MutableStateFlow(
        CptFlatPostListUiState(postTypeLabel = postTypeLabel)
    )
    val uiState: StateFlow<CptFlatPostListUiState> = _uiState.asStateFlow()

    private var observableCollection: ObservableMetadataCollection? = null

    init {
        if (selfHostedService != null) {
            createObservableCollection(PostStatusFilter.ALL)
            loadItemsFromCollection()
        }
    }

    /**
     * Change the filter and load persisted state from database.
     */
    fun setFilter(filter: PostStatusFilter) {
        if (_uiState.value.currentFilter == filter) return

        observableCollection?.close()
        createObservableCollection(filter)

        // Read persisted state from database (single query)
        _uiState.value = _uiState.value.copy(
            currentFilter = filter,
            listInfo = observableCollection?.listInfo(),
            errorMessage = null
        )

        // Load items (async)
        loadItemsFromCollection()
    }

    /**
     * Refresh the collection (fetch first page, sync missing/stale items).
     *
     * Note: syncState is managed by the database and observed via listInfo observer.
     * We don't manually toggle isSyncing - it's derived from listInfo.state.
     */
    @Suppress("TooGenericExceptionCaught") // FFI exceptions from wordpress-rs
    fun refresh() {
        if (_uiState.value.isSyncing) return

        _uiState.value = _uiState.value.copy(errorMessage = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val collection = observableCollection ?: return@launch
                collection.refresh()

                _uiState.value = _uiState.value.copy(
                    listInfo = collection.listInfo(),
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Unknown error"
                )
            }
        }
    }

    /**
     * Load the next page of items.
     *
     * Note: syncState is managed by the database and observed via listInfo observer.
     * We don't manually toggle isSyncing - it's derived from listInfo.state.
     */
    @Suppress("TooGenericExceptionCaught") // FFI exceptions from wordpress-rs
    fun loadNextPage() {
        val state = _uiState.value
        if (state.isSyncing || !state.hasMorePages) return

        // If no pages have been loaded yet, do a refresh instead
        if (state.currentPage == 0L) {
            refresh()
            return
        }

        _uiState.value = state.copy(errorMessage = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val collection = observableCollection ?: return@launch
                collection.loadNextPage()

                _uiState.value = _uiState.value.copy(
                    listInfo = collection.listInfo(),
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Unknown error"
                )
            }
        }
    }

    @Suppress("UnusedParameter") // Navigation will be implemented later
    fun onPostClick(post: CptPostListItem) {
        // No-op
    }

    override fun onCleared() {
        super.onCleared()
        observableCollection?.close()
        observableCollection = null
    }

    private fun createObservableCollection(filter: PostStatusFilter) {
        val service = selfHostedService ?: return
        val postService = service.posts()

        val postStatus = filter.apiValue?.let { parsePostStatus(it) }
        val listFilter = PostListFilter(
            status = if (postStatus != null) listOf(postStatus) else emptyList()
        )

        val observable = postService.getObservablePostMetadataCollectionWithEditContext(
            endpointType,
            listFilter
        )

        // Data observer: refresh list contents when data changes
        observable.addDataObserver {
            viewModelScope.launch(Dispatchers.Default) {
                loadItemsFromCollectionInternal()
            }
        }

        // ListInfo observer: update pagination and sync state when they change
        observable.addListInfoObserver {
            viewModelScope.launch(Dispatchers.Default) {
                updateListInfo()
            }
        }

        observableCollection = observable
    }

    private fun updateListInfo() {
        val newListInfo = observableCollection?.listInfo()
        _uiState.value = _uiState.value.copy(listInfo = newListInfo)
    }

    @Suppress("TooGenericExceptionCaught") // FFI exceptions from wordpress-rs
    private suspend fun loadItemsFromCollectionInternal() {
        try {
            val collection = observableCollection ?: return
            val rawItems = collection.loadItems()
            val posts = rawItems.map { CptPostListItem.fromCollectionItem(it) }
            _uiState.value = _uiState.value.copy(posts = posts)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                posts = emptyList(),
                errorMessage = e.message
            )
        }
    }

    private fun loadItemsFromCollection() {
        viewModelScope.launch(Dispatchers.Default) {
            loadItemsFromCollectionInternal()
        }
    }
}
