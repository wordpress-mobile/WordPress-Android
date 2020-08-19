package org.wordpress.android.ui.reader.actions

import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener
import org.wordpress.android.ui.reader.actions.ReaderBlogActions.BlockedBlogResult
import javax.inject.Inject

class ReaderBlogActionsWrapper @Inject constructor() {
    fun blockBlogFromReader(blogId: Long, actionListener: ActionListener?): BlockedBlogResult =
            ReaderBlogActions.blockBlogFromReader(blogId, actionListener)

    fun undoBlockBlogFromReader(blocked: BlockedBlogResult) = ReaderBlogActions.undoBlockBlogFromReader(blocked)
}
