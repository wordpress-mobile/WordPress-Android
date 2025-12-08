package org.wordpress.android.posttypes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.posttypes.bridge.BridgeConstants
import org.wordpress.android.posttypes.bridge.SiteReference
import javax.inject.Inject

data class CptPostListItem(
    val id: Long,
    val title: String,
    val excerpt: String,
    val status: String
)

data class CptFlatPostListUiState(
    val postTypeLabel: String = "",
    val posts: List<CptPostListItem> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class CptFlatPostListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    @Suppress("unused") // TODO: Will be used to fetch posts from wordpress-rs
    private val site: SiteReference? = savedStateHandle.get<SiteReference>(BridgeConstants.EXTRA_SITE)

    @Suppress("unused") // TODO: Will be used to fetch posts from wordpress-rs
    private val postTypeSlug: String = savedStateHandle.get<String>(
        CptFlatPostListActivity.EXTRA_POST_TYPE_SLUG
    ) ?: ""

    private val postTypeLabel: String = savedStateHandle.get<String>(
        CptFlatPostListActivity.EXTRA_POST_TYPE_LABEL
    ) ?: ""

    private val _uiState = MutableStateFlow(
        CptFlatPostListUiState(
            postTypeLabel = postTypeLabel,
            posts = generateMockPosts()
        )
    )
    val uiState: StateFlow<CptFlatPostListUiState> = _uiState.asStateFlow()

    private fun generateMockPosts(): List<CptPostListItem> {
        return listOf(
            CptPostListItem(1, "First Post", "This is the first post excerpt...", "Published"),
            CptPostListItem(2, "Second Post", "This is the second post excerpt...", "Draft"),
            CptPostListItem(3, "Third Post", "This is the third post excerpt...", "Published"),
            CptPostListItem(4, "Fourth Post", "This is the fourth post excerpt...", "Scheduled"),
            CptPostListItem(5, "Fifth Post", "This is the fifth post excerpt...", "Published")
        )
    }

    fun onPostClick(post: CptPostListItem) {
        // TODO: Navigate to post editor or detail screen
    }
}
