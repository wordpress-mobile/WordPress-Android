package org.wordpress.android.ui.reader.viewmodels

import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState.Bookmarks
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState.FollowedSitesNoRecentPosts
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState.NoConnection
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState.NoFollowedSites
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState.NoLikedPosts
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState.NoPostsInBlog
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState.NoPostsInList
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState.NoPostsWithTag
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState.RequestFailed
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState.SearchExplainer
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState.SearchNoResults
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState.SearchUpdating
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState.SelfHostedCta
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState.Updating
import javax.inject.Inject

/**
 * Picks which empty state the post list shows. The checks run in the same order the fragment used.
 */
class ReaderPostListEmptyStateBuilder @Inject constructor() {
    data class Input(
        val listType: ReaderPostListType,
        val tag: ReaderTag?,
        val isSelfHostedCta: Boolean,
        val isNetworkAvailable: Boolean,
        val requestFailed: Boolean,
        val isUpdating: Boolean,
        val hasFollowedBlogs: Boolean,
        val searchQuery: String?,
        val isSearchViewEmpty: Boolean = false
    )

    fun build(input: Input): ReaderPostListEmptyState {
        val isSearch = input.listType == ReaderPostListType.SEARCH_RESULTS
        return when {
            input.isSelfHostedCta -> SelfHostedCta
            input.listType == ReaderPostListType.TAG_FOLLOWED && input.tag?.isBookmarked == true -> Bookmarks
            !input.isNetworkAvailable -> NoConnection
            input.requestFailed -> RequestFailed(isSearch)
            input.isUpdating && !isSearch -> Updating
            else -> buildForListType(input)
        }
    }

    private fun buildForListType(input: Input): ReaderPostListEmptyState = when (input.listType) {
        ReaderPostListType.TAG_FOLLOWED -> buildForFollowedTag(input)
        ReaderPostListType.BLOG_PREVIEW -> NoPostsInBlog
        ReaderPostListType.SEARCH_RESULTS -> when {
            input.isSearchViewEmpty || input.searchQuery.isNullOrEmpty() -> SearchExplainer
            input.isUpdating -> SearchUpdating
            else -> SearchNoResults(input.searchQuery)
        }
        ReaderPostListType.TAG_PREVIEW,
        ReaderPostListType.TAGS_FEED -> NoPostsWithTag
    }

    private fun buildForFollowedTag(input: Input): ReaderPostListEmptyState {
        val tag = input.tag
        return when {
            tag == null -> NoPostsWithTag
            tag.isFollowedSites || tag.isDefaultInMemoryTag ->
                if (input.hasFollowedBlogs) FollowedSitesNoRecentPosts else NoFollowedSites
            tag.isPostsILike -> NoLikedPosts
            tag.isListTopic -> NoPostsInList
            else -> NoPostsWithTag
        }
    }
}
