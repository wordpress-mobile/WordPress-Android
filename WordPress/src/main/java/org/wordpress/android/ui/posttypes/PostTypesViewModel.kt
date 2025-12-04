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

data class PostTypeItem(
    val slug: String,
    val label: String
)

data class PostTypesUiState(
    val postTypes: List<PostTypeItem> = emptyList()
)

sealed class PostTypesNavigationAction {
    data class OpenPostTypeList(
        val site: SiteModel,
        val postTypeSlug: String,
        val postTypeLabel: String
    ) : PostTypesNavigationAction()
}

@HiltViewModel
class PostTypesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val site: SiteModel? = savedStateHandle.get<SiteModel>(WordPress.SITE)

    private val _uiState = MutableStateFlow(
        PostTypesUiState(
            postTypes = listOf(
                PostTypeItem(slug = "post", label = "Posts"),
                PostTypeItem(slug = "page", label = "Pages")
            )
        )
    )
    val uiState: StateFlow<PostTypesUiState> = _uiState.asStateFlow()

    private val _navigation = MutableSharedFlow<PostTypesNavigationAction>()
    val navigation: SharedFlow<PostTypesNavigationAction> = _navigation.asSharedFlow()

    fun onPostTypeClick(postType: PostTypeItem) {
        site?.let {
            _navigation.tryEmit(
                PostTypesNavigationAction.OpenPostTypeList(
                    site = it,
                    postTypeSlug = postType.slug,
                    postTypeLabel = postType.label
                )
            )
        }
    }
}
