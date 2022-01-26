package org.wordpress.android.ui.reader.actions

import org.wordpress.android.models.ReaderComment
import javax.inject.Inject

class ReaderCommentActionsWrapper @Inject constructor() {
    fun updateComment(comment: ReaderComment, listener: ReaderActions.CommentActionListener) =
            ReaderCommentActions.updateComment(comment, listener)
}
