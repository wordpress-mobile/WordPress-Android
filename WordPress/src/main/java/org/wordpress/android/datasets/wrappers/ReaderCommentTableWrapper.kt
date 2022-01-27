package org.wordpress.android.datasets.wrappers

import dagger.Reusable
import org.wordpress.android.datasets.ReaderCommentTable
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
}
