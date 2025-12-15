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
import uniffi.wp_api.parsePostStatus
import uniffi.wp_mobile.AnyPostFilter
import uniffi.wp_mobile.EntityState
import uniffi.wp_mobile.PostMetadataCollectionItem
import uniffi.wp_mobile.WpSelfHostedService
import uniffi.wp_mobile_cache.ListState
import javax.inject.Inject

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
    val entityState: EntityState = EntityState.Missing,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        fun fromCollectionItem(item: PostMetadataCollectionItem): CptPostListItem {
            val data = item.data
            return CptPostListItem(
                id = item.id,
                entityState = item.state,
                title = data?.data?.title?.rendered ?: "Post ${item.id}",
                excerpt = data?.data?.content?.rendered?.take(EXCERPT_MAX_LENGTH) ?: "",
                status = data?.data?.status?.toString()?.replaceFirstChar { it.uppercase() } ?: "",
                isLoading = item.state is EntityState.Fetching,
                errorMessage = (item.state as? EntityState.Failed)?.error
            )
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
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val syncState: ListState = ListState.IDLE,
    val currentPage: UInt = 0u,
    val totalPages: UInt? = null,
    val errorMessage: String? = null
) {
    val hasMorePages: Boolean
        get() = totalPages?.let { currentPage < it } ?: true
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
    ) ?: ""

    private val postTypeLabel: String = savedStateHandle.get<String>(
        CptFlatPostListActivity.EXTRA_POST_TYPE_LABEL
    ) ?: ""

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
     * Change the filter and reload posts.
     */
    fun setFilter(filter: PostStatusFilter) {
        if (_uiState.value.currentFilter == filter) return

        observableCollection?.close()
        createObservableCollection(filter)

        val collection = observableCollection
        _uiState.value = _uiState.value.copy(
            currentFilter = filter,
            currentPage = collection?.currentPage() ?: 0u,
            totalPages = collection?.totalPages(),
            errorMessage = null,
            isSyncing = false,
            syncState = ListState.IDLE
        )

        viewModelScope.launch(Dispatchers.Default) {
            loadItemsFromCollectionInternal()
            updateSyncState()
        }
    }

    /**
     * Refresh the collection (fetch first page, sync missing/stale items).
     */
    fun refresh() {
        if (_uiState.value.isSyncing) return

        _uiState.value = _uiState.value.copy(isSyncing = true, errorMessage = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val collection = observableCollection ?: return@launch
                val result = collection.refresh()

                _uiState.value = _uiState.value.copy(
                    currentPage = collection.currentPage(),
                    totalPages = collection.totalPages(),
                    errorMessage = null,
                    isSyncing = false,
                    syncState = collection.syncState()
                )

                loadItemsFromCollection()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Unknown error",
                    isSyncing = false,
                    syncState = observableCollection?.syncState() ?: _uiState.value.syncState
                )
            }
        }
    }

    /**
     * Load the next page of items.
     */
    fun loadNextPage() {
        if (_uiState.value.isSyncing) return
        if (!_uiState.value.hasMorePages) return

        // If no pages have been loaded yet, do a refresh instead
        if (_uiState.value.currentPage == 0u) {
            refresh()
            return
        }

        _uiState.value = _uiState.value.copy(isSyncing = true, errorMessage = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val collection = observableCollection ?: return@launch
                val result = collection.loadNextPage()

                _uiState.value = _uiState.value.copy(
                    currentPage = collection.currentPage(),
                    totalPages = collection.totalPages(),
                    errorMessage = null,
                    isSyncing = false,
                    syncState = collection.syncState()
                )

                loadItemsFromCollection()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Unknown error",
                    isSyncing = false,
                    syncState = observableCollection?.syncState() ?: _uiState.value.syncState
                )
            }
        }
    }

    fun onPostClick(post: CptPostListItem) {
        // No-op: navigation will be implemented later
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
        val anyPostFilter = AnyPostFilter(status = postStatus)

        val observable = postService.getObservablePostMetadataCollectionWithEditContext(anyPostFilter)

        // Data observer: refresh list contents when data changes
        observable.addDataObserver {
            viewModelScope.launch(Dispatchers.Default) {
                loadItemsFromCollectionInternal()
            }
        }

        // State observer: update sync state indicator when state changes
        observable.addStateObserver {
            viewModelScope.launch(Dispatchers.Default) {
                updateSyncState()
            }
        }

        observableCollection = observable
    }

    private suspend fun updateSyncState() {
        val collection = observableCollection ?: return
        val newSyncState = collection.syncState()
        _uiState.value = _uiState.value.copy(syncState = newSyncState)
    }

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
