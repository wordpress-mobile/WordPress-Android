package org.wordpress.android.ui.posttypes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    fun onPostTypeClick(postType: PostTypeItem) {
        // TODO: Navigate to flat/hierarchical post list screen based on post type
    }
}
