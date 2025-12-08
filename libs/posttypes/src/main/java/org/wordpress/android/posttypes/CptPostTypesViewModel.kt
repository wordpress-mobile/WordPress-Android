package org.wordpress.android.posttypes

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

sealed class CptNavigationAction {
    data class OpenPostTypeList(
        val site: SiteReference,
        val postTypeSlug: String,
        val postTypeLabel: String,
        val hierarchical: Boolean
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
        site?.let {
            _navigation.tryEmit(
                CptNavigationAction.OpenPostTypeList(
                    site = it,
                    postTypeSlug = postType.slug,
                    postTypeLabel = postType.label,
                    hierarchical = postType.hierarchical
                )
            )
        }
    }
}
