package org.wordpress.android.datasets.wrappers

import dagger.Reusable
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.models.ReaderPost
import javax.inject.Inject

@Reusable
class ReaderPostTableWrapper @Inject constructor() {
    fun getBlogPost(blogId: Long, postId: Long, excludeTextColumn: Boolean): ReaderPost? =
            ReaderPostTable.getBlogPost(blogId, postId, excludeTextColumn)

    fun isPostFollowed(post: ReaderPost): Boolean = ReaderPostTable.isPostFollowed(post)
}
