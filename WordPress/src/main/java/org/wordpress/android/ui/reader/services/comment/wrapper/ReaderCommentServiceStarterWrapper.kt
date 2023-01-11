package org.wordpress.android.ui.reader.services.comment.wrapper

import android.content.Context
import dagger.Reusable
import org.wordpress.android.ui.reader.services.comment.ReaderCommentService
import javax.inject.Inject

@Reusable
class ReaderCommentServiceStarterWrapper @Inject constructor() {
    fun startServiceForCommentSnippet(context: Context?, blogId: Long, postId: Long) =
        ReaderCommentService.startServiceForCommentSnippet(
            context,
            blogId,
            postId
        )
}
