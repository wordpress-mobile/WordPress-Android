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
import uniffi.wp_mobile.ListInfo
import uniffi.wp_mobile.PostListFilter
import uniffi.wp_mobile.PostItemState
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
 * Display data for a hierarchical post item with its parent relationship.
 */
data class CptHierarchicalPostItem(
    val id: Long,
    val parentId: Long?,
    val title: String,
    val status: String,
    val itemState: PostItemState = PostItemState.Missing,
    val indent: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        fun fromCollectionItem(item: PostMetadataCollectionItem): CptHierarchicalPostItem {
            // Parent is always available from metadata, regardless of post data fetch state
            val parentId = item.parent

            val state = item.state
            return when (state) {
                is PostItemState.Missing -> CptHierarchicalPostItem(
                    id = item.id,
                    parentId = parentId,
                    itemState = state,
                    title = "Page ${item.id}",
                    status = ""
                )
                is PostItemState.Fetching -> CptHierarchicalPostItem(
                    id = item.id,
                    parentId = parentId,
                    itemState = state,
                    title = "Page ${item.id}",
                    status = "",
                    isLoading = true
                )
                is PostItemState.FetchingWithData -> CptHierarchicalPostItem(
                    id = item.id,
                    parentId = parentId,
                    itemState = state,
                    title = state.data.data.title.rendered,
                    status = state.data.data.status.toString().replaceFirstChar { it.uppercase() },
                    isLoading = true
                )
                is PostItemState.Cached -> CptHierarchicalPostItem(
                    id = item.id,
                    parentId = parentId,
                    itemState = state,
                    title = state.data.data.title.rendered,
                    status = state.data.data.status.toString().replaceFirstChar { it.uppercase() }
                )
                is PostItemState.Stale -> CptHierarchicalPostItem(
                    id = item.id,
                    parentId = parentId,
                    itemState = state,
                    title = state.data.data.title.rendered,
                    status = state.data.data.status.toString().replaceFirstChar { it.uppercase() }
                )
                is PostItemState.Failed -> CptHierarchicalPostItem(
                    id = item.id,
                    parentId = parentId,
                    itemState = state,
                    title = "Page ${item.id}",
                    status = "",
                    errorMessage = state.error
                )
                is PostItemState.FailedWithData -> CptHierarchicalPostItem(
                    id = item.id,
                    parentId = parentId,
                    itemState = state,
                    title = state.data.data.title.rendered,
                    status = state.data.data.status.toString().replaceFirstChar { it.uppercase() },
                    errorMessage = state.error
                )
            }
        }
    }
}

/**
 * UI state for the hierarchical post list screen.
 */
data class CptHierarchicalPostListUiState(
    val postTypeLabel: String = "",
    val posts: List<CptHierarchicalPostItem> = emptyList(),
    val listInfo: ListInfo? = null,
    val errorMessage: String? = null
) {
    val isSyncing: Boolean
        get() = listInfo?.isSyncing ?: false

    val hasMorePages: Boolean
        get() = listInfo?.hasMorePages ?: true

    val currentPage: Long
        get() = listInfo?.currentPage?.toLong() ?: 0L
}

@HiltViewModel
class CptHierarchicalPostListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    serviceFactory: WpSelfHostedServiceFactory
) : ViewModel() {
    private val site: SiteReference? = savedStateHandle.get<SiteReference>(BridgeConstants.EXTRA_SITE)

    @Suppress("unused") // Reserved for future use
    private val postTypeSlug: String = savedStateHandle.get<String>(
        CptHierarchicalPostListActivity.EXTRA_POST_TYPE_SLUG
    ).orEmpty()

    private val postTypeLabel: String = savedStateHandle.get<String>(
        CptHierarchicalPostListActivity.EXTRA_POST_TYPE_LABEL
    ).orEmpty()

    private val endpointType: PostEndpointType = savedStateHandle.get<String>(
        CptHierarchicalPostListActivity.EXTRA_ENDPOINT_TYPE_ID
    )?.toPostEndpointType() ?: PostEndpointType.Pages

    private val selfHostedService: WpSelfHostedService? = site?.let {
        serviceFactory.create(it.id, it.url)
    }

    private val _uiState = MutableStateFlow(
        CptHierarchicalPostListUiState(postTypeLabel = postTypeLabel)
    )
    val uiState: StateFlow<CptHierarchicalPostListUiState> = _uiState.asStateFlow()

    private var observableCollection: ObservableMetadataCollection? = null

    init {
        if (selfHostedService != null) {
            createObservableCollection()
            loadItemsFromCollection()
        }
    }

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

    @Suppress("TooGenericExceptionCaught") // FFI exceptions from wordpress-rs
    fun loadNextPage() {
        val state = _uiState.value
        if (state.isSyncing || !state.hasMorePages) return

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
    fun onPostClick(post: CptHierarchicalPostItem) {
        // No-op
    }

    override fun onCleared() {
        super.onCleared()
        observableCollection?.close()
        observableCollection = null
    }

    private fun createObservableCollection() {
        val service = selfHostedService ?: return
        val postService = service.posts()

        val listFilter = PostListFilter()

        val observable = postService.getObservablePostMetadataCollectionWithEditContext(
            endpointType,
            listFilter
        )

        observable.addDataObserver {
            viewModelScope.launch(Dispatchers.Default) {
                loadItemsFromCollectionInternal()
            }
        }

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
            val flatItems = rawItems.map { CptHierarchicalPostItem.fromCollectionItem(it) }
            val hierarchicalItems = buildHierarchy(flatItems)
            _uiState.value = _uiState.value.copy(posts = hierarchicalItems)
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

    /**
     * Build a hierarchical list with proper indentation from flat items with parent references.
     *
     * Items are ordered depth-first: parent followed by its children (recursively).
     * Indent level is calculated based on parent depth.
     */
    private fun buildHierarchy(items: List<CptHierarchicalPostItem>): List<CptHierarchicalPostItem> {
        if (items.isEmpty()) return emptyList()

        val itemsById = items.associateBy { it.id }
        val childrenByParent = items.groupBy { it.parentId ?: 0L }
        val result = mutableListOf<CptHierarchicalPostItem>()

        fun addWithChildren(parentId: Long, indent: Int) {
            val children = childrenByParent[parentId] ?: return
            for (child in children) {
                result.add(child.copy(indent = indent))
                addWithChildren(child.id, indent + 1)
            }
        }

        // Start with root items (parentId = 0 or null, or parent not in current list)
        val rootItems = items.filter { item ->
            item.parentId == null || item.parentId == 0L || !itemsById.containsKey(item.parentId)
        }

        for (root in rootItems) {
            result.add(root.copy(indent = 0))
            addWithChildren(root.id, 1)
        }

        return result
    }
}
