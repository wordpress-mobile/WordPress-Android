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

data class CptHierarchicalPostItem(
    val id: Long,
    val title: String,
    val status: String,
    val indent: Int = 0
)

data class CptHierarchicalPostListUiState(
    val postTypeLabel: String = "",
    val posts: List<CptHierarchicalPostItem> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class CptHierarchicalPostListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    @Suppress("unused") // Will be used to fetch posts from wordpress-rs
    private val site: SiteReference? = savedStateHandle.get<SiteReference>(BridgeConstants.EXTRA_SITE)

    @Suppress("unused") // Will be used to fetch posts from wordpress-rs
    private val postTypeSlug: String = savedStateHandle.get<String>(
        CptHierarchicalPostListActivity.EXTRA_POST_TYPE_SLUG
    ) ?: ""

    private val postTypeLabel: String = savedStateHandle.get<String>(
        CptHierarchicalPostListActivity.EXTRA_POST_TYPE_LABEL
    ) ?: ""

    private val _uiState = MutableStateFlow(
        CptHierarchicalPostListUiState(
            postTypeLabel = postTypeLabel,
            posts = generateMockHierarchy()
        )
    )
    val uiState: StateFlow<CptHierarchicalPostListUiState> = _uiState.asStateFlow()

    @Suppress("MagicNumber") // Mock data - will be replaced with real data from wordpress-rs
    private fun generateMockHierarchy(): List<CptHierarchicalPostItem> {
        // Simulates a page hierarchy: Home > About > Team, Contact
        return listOf(
            CptHierarchicalPostItem(1, "Home", "Published", indent = 0),
            CptHierarchicalPostItem(2, "About", "Published", indent = 1),
            CptHierarchicalPostItem(3, "Team", "Published", indent = 2),
            CptHierarchicalPostItem(4, "Contact", "Published", indent = 1),
            CptHierarchicalPostItem(5, "Blog", "Draft", indent = 0)
        )
    }

    @Suppress("UnusedParameter") // Will navigate to post editor with wordpress-rs integration
    fun onPostClick(post: CptHierarchicalPostItem) {
        // No-op: navigation will be implemented with wordpress-rs integration
    }
}
