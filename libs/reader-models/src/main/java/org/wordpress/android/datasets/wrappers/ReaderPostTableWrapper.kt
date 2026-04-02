package org.wordpress.android.datasets.wrappers

import dagger.Reusable
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId
import javax.inject.Inject

@Reusable
class ReaderPostTableWrapper @Inject constructor() {
    fun getBlogPost(blogId: Long, postId: Long, excludeTextColumn: Boolean): ReaderPost? =
        ReaderPostTable.getBlogPost(blogId, postId, excludeTextColumn)

    fun getFeedPost(blogId: Long, postId: Long, excludeTextColumn: Boolean): ReaderPost? = ReaderPostTable
        .getFeedPost(blogId, postId, excludeTextColumn)

    fun isPostFollowed(post: ReaderPost): Boolean = ReaderPostTable.isPostFollowed(post)

    fun isPostSeen(post: ReaderPost): Boolean = ReaderPostTable.isPostSeen(post)

    fun setPostSeenStatusInDb(post: ReaderPost, isSeen: Boolean) = ReaderPostTable.setPostSeenStatus(post, isSeen)

    fun getPostsWithTag(
        readerTag: ReaderTag,
        maxRows: Int,
        excludeTextColumn: Boolean
    ): ReaderPostList =
        ReaderPostTable.getPostsWithTag(readerTag, maxRows, excludeTextColumn)

    fun getNumPostsWithTag(readerTag: ReaderTag): Int = ReaderPostTable.getNumPostsWithTag(readerTag)

    fun addOrUpdatePosts(readerTag: ReaderTag?, posts: ReaderPostList) =
        ReaderPostTable.addOrUpdatePosts(readerTag, posts)

    fun deletePostsWithTag(tag: ReaderTag) = ReaderPostTable.deletePostsWithTag(tag)

    fun comparePosts(posts: ReaderPostList): UpdateResult = ReaderPostTable.comparePosts(posts)

    fun updateBookmarkedPostPseudoId(posts: ReaderPostList) = ReaderPostTable.updateBookmarkedPostPseudoId(posts)

    fun setGapMarkerForTag(blogId: Long, postId: Long, tag: ReaderTag) =
        ReaderPostTable.setGapMarkerForTag(blogId, postId, tag)

    fun removeGapMarkerForTag(tag: ReaderTag) = ReaderPostTable.removeGapMarkerForTag(tag)

    fun deletePostsBeforeGapMarkerForTag(tag: ReaderTag) = ReaderPostTable.deletePostsBeforeGapMarkerForTag(tag)

    fun hasOverlap(posts: ReaderPostList?, tag: ReaderTag): Boolean = ReaderPostTable.hasOverlap(posts, tag)

    fun getGapMarkerIdsForTag(tag: ReaderTag): ReaderBlogIdPostId? = ReaderPostTable.getGapMarkerIdsForTag(tag)
}
