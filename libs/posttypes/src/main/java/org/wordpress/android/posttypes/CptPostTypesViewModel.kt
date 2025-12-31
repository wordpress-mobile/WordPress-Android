package org.wordpress.android.posttypes

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.posttypes.bridge.BridgeConstants
import org.wordpress.android.posttypes.bridge.SiteReference
import org.wordpress.android.posttypes.bridge.WpSelfHostedServiceFactory
import rs.wordpress.cache.kotlin.ObservableCollection
import rs.wordpress.cache.kotlin.getObservablePostTypeCollectionWithEditContext
import uniffi.wp_mobile.FullEntityPostTypeDetailsWithEditContext
import uniffi.wp_mobile.PostTypeCollectionWithEditContext
import javax.inject.Inject

data class CptPostTypeItem(
    val slug: String,
    val name: String,
    val description: String?,
    val hierarchical: Boolean = false
) {
    companion object {
        fun fromEntity(entity: uniffi.wp_mobile.FullEntityPostTypeDetailsWithEditContext): CptPostTypeItem {
            val postType = entity.data
            return CptPostTypeItem(
                slug = postType.slug,
                name = postType.name,
                description = postType.description,
                hierarchical = postType.hierarchical ?: false
            )
        }
    }
}

data class CptPostTypesUiState(
    val postTypes: List<CptPostTypeItem> = emptyList(),
    val isFetching: Boolean = false,
    val lastError: String? = null, // TODO: Consider better error type
    val hasFetchedOnce: Boolean = false
)

/**
 * String identifiers for PostEndpointType that can be passed through Intent extras.
 *
 * These map to wordpress-rs PostEndpointType variants:
 * - [ENDPOINT_TYPE_POSTS] -> PostEndpointType.Posts
 * - [ENDPOINT_TYPE_PAGES] -> PostEndpointType.Pages
 * - Any other value -> PostEndpointType.Custom(value)
 */
object PostEndpointTypeId {
    const val POSTS = "Posts"
    const val PAGES = "Pages"
}

sealed class CptNavigationAction {
    data class OpenPostTypeList(
        val site: SiteReference,
        val postTypeSlug: String,
        val postTypeLabel: String,
        val hierarchical: Boolean,
        val endpointTypeId: String
    ) : CptNavigationAction()
}

@HiltViewModel
class CptPostTypesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    serviceFactory: WpSelfHostedServiceFactory
) : ViewModel() {
    private val site: SiteReference? = savedStateHandle.get<SiteReference>(BridgeConstants.EXTRA_SITE)

    private val _uiState = MutableStateFlow(CptPostTypesUiState())
    val uiState: StateFlow<CptPostTypesUiState> = _uiState.asStateFlow()

    private val _navigation = MutableSharedFlow<CptNavigationAction>(extraBufferCapacity = 1)
    val navigation: SharedFlow<CptNavigationAction> = _navigation.asSharedFlow()

    private var observableCollection: ObservableCollection<FullEntityPostTypeDetailsWithEditContext>? = null
    private var postTypeCollection: PostTypeCollectionWithEditContext? = null

    init {
        site?.let {
            val selfHostedService = serviceFactory.create(it.id, it.url)
            val postTypeService = selfHostedService.postTypes()
            createObservableCollection(postTypeService)
            loadPostTypesFromCache()
            fetch()
        } ?: Log.e(TAG, "Site is null, cannot initialize post types")
    }

    /**
     * Fetch all post types from the network
     */
    fun fetch() {
        if (_uiState.value.isFetching) {
            return // Already fetching, ignore
        }

        _uiState.value = _uiState.value.copy(
            isFetching = true,
            lastError = null
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val collection = postTypeCollection
                if (collection == null) {
                    _uiState.value = _uiState.value.copy(isFetching = false)
                    return@launch
                }

                // Fetch all post types (no pagination)
                collection.fetch()

                // Update state with successful result
                _uiState.value = _uiState.value.copy(
                    isFetching = false,
                    lastError = null,
                    hasFetchedOnce = true
                )

                // Post types will auto-reload via ObservableCollection after database update
            } catch (error: Exception) {
                // Update state with error
                _uiState.value = _uiState.value.copy(
                    lastError = error.message ?: "Unknown error",
                    isFetching = false
                )
            }
        }
    }

    /**
     * Create the observable collection
     */
    private fun createObservableCollection(postTypeService: uniffi.wp_mobile.PostTypeService) {
        // Create the underlying PostTypeCollection (for fetch)
        // Uses default filter (viewable = true)
        val underlyingCollection = postTypeService.createPostTypeCollectionWithEditContext()
        postTypeCollection = underlyingCollection

        // Create observable wrapper (for auto-reload on DB changes)
        val observable = postTypeService.getObservablePostTypeCollectionWithEditContext()

        // Set up observer to reload post types when database changes
        observable.addObserver {
            loadPostTypesFromCache()
        }

        observableCollection = observable
    }

    /**
     * Load post types from cache and update the state flow
     */
    private fun loadPostTypesFromCache() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val collection = observableCollection ?: return@launch
                val allPostTypes = collection.loadData()

                val postTypeDataList = allPostTypes.map { fullEntity ->
                    CptPostTypeItem.fromEntity(fullEntity)
                }

                _uiState.value = _uiState.value.copy(postTypes = postTypeDataList)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading post types from cache: ${e.message}")
                _uiState.value = _uiState.value.copy(postTypes = emptyList())
            }
        }
    }

    fun onPostTypeClick(postType: CptPostTypeItem) {
        if (site == null) {
            Log.e(TAG, "Site is null, cannot navigate to post type list")
            return
        }
        _navigation.tryEmit(
            CptNavigationAction.OpenPostTypeList(
                site = site,
                postTypeSlug = postType.slug,
                postTypeLabel = postType.name,
                hierarchical = postType.hierarchical,
                endpointTypeId = resolveEndpointTypeId(postType.slug)
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        observableCollection?.close()
        observableCollection = null
    }

    companion object {
        private const val TAG = "CptPostTypesViewModel"
    }

    /**
     * Resolve the PostEndpointType identifier from a post type slug.
     *
     * Standard WordPress post types ("post", "page") map to their respective endpoint types.
     * Custom post types use the slug as the identifier for PostEndpointType.Custom.
     */
    private fun resolveEndpointTypeId(slug: String): String {
        return when (slug) {
            "post" -> PostEndpointTypeId.POSTS
            "page" -> PostEndpointTypeId.PAGES
            else -> slug // Custom post type slug
        }
    }
}
