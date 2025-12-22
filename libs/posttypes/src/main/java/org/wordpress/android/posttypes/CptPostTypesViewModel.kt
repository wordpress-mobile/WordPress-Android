package org.wordpress.android.posttypes

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.posttypes.bridge.BridgeConstants
import org.wordpress.android.posttypes.bridge.SiteReference
import javax.inject.Inject

data class CptPostTypeItem(
    val slug: String,
    val label: String,
    val hierarchical: Boolean = false
)

data class CptPostTypesUiState(
    val postTypes: List<CptPostTypeItem> = emptyList()
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val site: SiteReference? = savedStateHandle.get<SiteReference>(BridgeConstants.EXTRA_SITE)

    private val _uiState = MutableStateFlow(
        CptPostTypesUiState(
            postTypes = listOf(
                CptPostTypeItem(slug = "post", label = "Posts", hierarchical = false),
                CptPostTypeItem(slug = "page", label = "Pages", hierarchical = true)
            )
        )
    )
    val uiState: StateFlow<CptPostTypesUiState> = _uiState.asStateFlow()

    private val _navigation = MutableSharedFlow<CptNavigationAction>(extraBufferCapacity = 1)
    val navigation: SharedFlow<CptNavigationAction> = _navigation.asSharedFlow()

    fun onPostTypeClick(postType: CptPostTypeItem) {
        if (site == null) {
            Log.e(TAG, "Site is null, cannot navigate to post type list")
            return
        }
        _navigation.tryEmit(
            CptNavigationAction.OpenPostTypeList(
                site = site,
                postTypeSlug = postType.slug,
                postTypeLabel = postType.label,
                hierarchical = postType.hierarchical,
                endpointTypeId = resolveEndpointTypeId(postType.slug)
            )
        )
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
