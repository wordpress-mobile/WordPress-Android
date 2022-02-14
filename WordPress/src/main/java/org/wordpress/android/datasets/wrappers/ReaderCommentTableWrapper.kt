package org.wordpress.android.datasets.wrappers

import dagger.Reusable
import org.wordpress.android.datasets.ReaderCommentTable
import org.wordpress.android.models.ReaderComment
import org.wordpress.android.models.ReaderCommentList
import org.wordpress.android.models.ReaderPost
import javax.inject.Inject

@Reusable
class ReaderCommentTableWrapper @Inject constructor() {
    fun getCommentsForPostSnippet(post: ReaderPost?, limit: Int): ReaderCommentList? =
            ReaderCommentTable.getCommentsForPostSnippet(
                    post,
                    limit
            )

    fun getComment(blogId: Long, postId: Long, commentId: Long): ReaderComment? =
            ReaderCommentTable.getComment(
                    blogId,
                    postId,
                    commentId
            )

    fun addOrUpdateComment(readerComment: ReaderComment) = ReaderCommentTable.addOrUpdateComment(readerComment)
}
