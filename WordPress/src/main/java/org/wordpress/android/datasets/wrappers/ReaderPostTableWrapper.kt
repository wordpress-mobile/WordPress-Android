package org.wordpress.android.datasets.wrappers

import dagger.Reusable
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import javax.inject.Inject

@Reusable
class ReaderPostTableWrapper @Inject constructor() {
    fun getBlogPost(blogId: Long, postId: Long, excludeTextColumn: Boolean): ReaderPost? =
            ReaderPostTable.getBlogPost(blogId, postId, excludeTextColumn)

    fun isPostFollowed(post: ReaderPost): Boolean = ReaderPostTable.isPostFollowed(post)

    fun getPostsWithTag(
        readerTag: ReaderTag,
        maxRows: Int,
        excludeTextColumn: Boolean
    ): ReaderPostList =
            ReaderPostTable.getPostsWithTag(readerTag, maxRows, excludeTextColumn)

    fun getNumPostsWithTag(readerTag: ReaderTag): Int = ReaderPostTable.getNumPostsWithTag(readerTag)

}
