package org.wordpress.android.ui.reader.viewmodels

import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType

/**
 * Everything the post list needs to start, and everything it saves to survive process death
 */
data class ReaderPostListArgs(
    val listType: ReaderPostListType,
    val tag: ReaderTag? = null,
    val originalTag: ReaderTag? = null,
    val blogId: Long = 0,
    val feedId: Long = 0,
    val isTopLevel: Boolean = false,
    val isFilterable: Boolean = false,
    val searchQuery: String? = null,
    val tagHistory: List<String> = emptyList(),
    val hasRequestedPosts: Boolean = false,
    val hasUpdatedPosts: Boolean = false,
    val restorePosition: Int = 0
)

data class ReaderPostListUiState(
    val items: List<ReaderPostListItem> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isUpdating: Boolean = false,
    val newPostsBarVisible: Boolean = false,
    val emptyState: ReaderPostListEmptyState? = null,
    val scrollToPosition: Int? = null
)

sealed class ReaderPostListItem {
    abstract val stableId: Long

    /**
     * [postsVersion] changes whenever the loaded posts change, so the header rebinds (and reloads its
     * blog info) at the same times it did when the adapter called notifyDataSetChanged
     */
    data class SiteHeader(val blogId: Long, val feedId: Long, val postsVersion: Int) : ReaderPostListItem() {
        override val stableId = ITEM_ID_HEADER
    }

    data class TagHeader(
        val label: String,
        val isFollowed: Boolean,
        val isFollowActionRunning: Boolean = false
    ) : ReaderPostListItem() {
        override val stableId = ITEM_ID_HEADER
    }

    data class Post(val uiState: ReaderPostUiState, override val stableId: Long) : ReaderPostListItem()

    data class XPost(val post: ReaderPost) : ReaderPostListItem() {
        override val stableId = post.stableId
    }

    data class RemovedPost(val post: ReaderPost) : ReaderPostListItem() {
        override val stableId = post.stableId
    }

    data class GapMarker(val isFilling: Boolean) : ReaderPostListItem() {
        override val stableId = ITEM_ID_GAP_MARKER
    }

    companion object {
        const val ITEM_ID_HEADER = -1L
        const val ITEM_ID_GAP_MARKER = -2L
    }
}

sealed class ReaderPostListEmptyState {
    data object SelfHostedCta : ReaderPostListEmptyState()
    data object Bookmarks : ReaderPostListEmptyState()
    data object NoConnection : ReaderPostListEmptyState()
    data class RequestFailed(val isSearch: Boolean) : ReaderPostListEmptyState()
    data object Updating : ReaderPostListEmptyState()
    data object FollowedSitesNoRecentPosts : ReaderPostListEmptyState()
    data object NoFollowedSites : ReaderPostListEmptyState()
    data object NoLikedPosts : ReaderPostListEmptyState()
    data object NoPostsInList : ReaderPostListEmptyState()
    data object NoPostsWithTag : ReaderPostListEmptyState()
    data object NoPostsInBlog : ReaderPostListEmptyState()
    data object SearchExplainer : ReaderPostListEmptyState()
    data object SearchUpdating : ReaderPostListEmptyState()
    data class SearchNoResults(val query: String) : ReaderPostListEmptyState()
}

enum class ReaderPostListEmptyButton { DISCOVER, FOLLOWED }

sealed class ReaderPostListUiEvent {
    data class PostSelected(val post: ReaderPost) : ReaderPostListUiEvent()
    data class PostButtonClicked(val post: ReaderPost, val type: ReaderPostCardActionType) : ReaderPostListUiEvent()
    data class PostHeaderClicked(val post: ReaderPost) : ReaderPostListUiEvent()
    data class VideoOverlayClicked(val post: ReaderPost) : ReaderPostListUiEvent()
    data class TagFollowFailed(val isFollowRequest: Boolean) : ReaderPostListUiEvent()
    data class SetSubfilterFromTag(val tag: ReaderTag) : ReaderPostListUiEvent()
    data object SetDefaultSubfilter : ReaderPostListUiEvent()
    data object NoConnection : ReaderPostListUiEvent()
    data object DataLoaded : ReaderPostListUiEvent()
    data class SearchStarted(val hasPosts: Boolean) : ReaderPostListUiEvent()
    data class SearchEnded(val showResults: Boolean) : ReaderPostListUiEvent()
}
