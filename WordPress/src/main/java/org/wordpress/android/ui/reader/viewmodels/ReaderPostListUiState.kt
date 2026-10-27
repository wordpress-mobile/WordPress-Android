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
    val canPullToRefresh: Boolean = false,
    val newPostsBarVisible: Boolean = false,
    val emptyState: ReaderPostListEmptyState? = null,
    val scrollToPosition: Int? = null
)

sealed class ReaderPostListItem {
    /**
     * Unique within the list - the LazyColumn crashes on duplicate keys
     */
    abstract val key: String

    data class SiteHeader(val info: ReaderSiteHeaderUiState) : ReaderPostListItem() {
        override val key = KEY_HEADER
    }

    data class TagHeader(
        val label: String,
        val isFollowed: Boolean,
        val isFollowActionRunning: Boolean = false
    ) : ReaderPostListItem() {
        override val key = KEY_HEADER
    }

    data class Post(val uiState: ReaderPostUiState, override val key: String) : ReaderPostListItem()

    data class XPost(val post: ReaderPost) : ReaderPostListItem() {
        override val key = postKey(post)
    }

    data class RemovedPost(val post: ReaderPost) : ReaderPostListItem() {
        override val key = postKey(post)
    }

    data class GapMarker(val isFilling: Boolean) : ReaderPostListItem() {
        override val key = KEY_GAP_MARKER
    }

    companion object {
        private const val KEY_HEADER = "header"
        private const val KEY_GAP_MARKER = "gap"

        // pseudo_id is part of the posts table's primary key, and every list query fixes the rest of it, so it's
        // unique within a list - the ids are only a fallback for a post without one
        fun postKey(post: ReaderPost) = post.pseudoId.ifEmpty { "${post.blogId}:${post.feedId}:${post.postId}" }
    }
}

data class ReaderSiteHeaderUiState(
    val name: String?,
    val host: String?,
    val description: String?,
    val imageUrl: String?,
    val isP2orA8C: Boolean,
    val subscriberCount: Int,
    val isFollowed: Boolean,
    val isFollowInProgress: Boolean,
    val showSettings: Boolean
)

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
    data object BlogInfoFailed : ReaderPostListUiEvent()
    data class ShowSubscriptionSettings(val blogId: Long, val blogName: String, val blogHost: String) :
        ReaderPostListUiEvent()
}
