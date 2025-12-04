package org.wordpress.android.ui.posttypes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

data class CptPostTypeItem(
    val slug: String,
    val label: String
)

data class CptPostTypesUiState(
    val postTypes: List<CptPostTypeItem> = emptyList()
)

sealed class CptNavigationAction {
    data class OpenPostTypeList(
        val site: SiteModel,
        val postTypeSlug: String,
        val postTypeLabel: String
    ) : CptNavigationAction()
}

@HiltViewModel
class CptPostTypesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val site: SiteModel? = savedStateHandle.get<SiteModel>(WordPress.SITE)

    private val _uiState = MutableStateFlow(
        CptPostTypesUiState(
            postTypes = listOf(
                CptPostTypeItem(slug = "post", label = "Posts"),
                CptPostTypeItem(slug = "page", label = "Pages")
            )
        )
    )
    val uiState: StateFlow<CptPostTypesUiState> = _uiState.asStateFlow()

    private val _navigation = MutableSharedFlow<CptNavigationAction>()
    val navigation: SharedFlow<CptNavigationAction> = _navigation.asSharedFlow()

    fun onPostTypeClick(postType: CptPostTypeItem) {
        site?.let {
            _navigation.tryEmit(
                CptNavigationAction.OpenPostTypeList(
                    site = it,
                    postTypeSlug = postType.slug,
                    postTypeLabel = postType.label
                )
            )
        }
    }
}
