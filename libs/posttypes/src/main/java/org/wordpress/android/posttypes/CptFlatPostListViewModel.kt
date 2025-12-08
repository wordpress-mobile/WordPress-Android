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
    @Suppress("unused") // Will be used to fetch posts from wordpress-rs
    private val site: SiteReference? = savedStateHandle.get<SiteReference>(BridgeConstants.EXTRA_SITE)

    @Suppress("unused") // Will be used to fetch posts from wordpress-rs
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

    @Suppress("MagicNumber") // Mock data - will be replaced with real data from wordpress-rs
    private fun generateMockPosts(): List<CptPostListItem> {
        val statuses = listOf("Published", "Draft", "Scheduled")
        return (1..5).map { id ->
            CptPostListItem(
                id = id.toLong(),
                title = "Post $id",
                excerpt = "This is the post $id excerpt...",
                status = statuses[id % statuses.size]
            )
        }
    }

    @Suppress("UnusedParameter") // Will navigate to post editor with wordpress-rs integration
    fun onPostClick(post: CptPostListItem) {
        // No-op: navigation will be implemented with wordpress-rs integration
    }
}
