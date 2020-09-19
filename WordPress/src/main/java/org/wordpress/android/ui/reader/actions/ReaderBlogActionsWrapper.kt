package org.wordpress.android.ui.reader.actions

import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener
import org.wordpress.android.ui.reader.actions.ReaderBlogActions.BlockedBlogResult
import javax.inject.Inject

class ReaderBlogActionsWrapper @Inject constructor() {
    fun blockBlogFromReaderLocal(blogId: Long): BlockedBlogResult = ReaderBlogActions.blockBlogFromReaderLocal(blogId)

    fun blockBlogFromReaderRemote(blockedBlogResult: BlockedBlogResult, actionListener: ActionListener?): Unit =
            ReaderBlogActions.blockBlogFromReaderRemote(blockedBlogResult, actionListener)

    fun undoBlockBlogFromReader(blocked: BlockedBlogResult) = ReaderBlogActions.undoBlockBlogFromReader(blocked)

    fun followBlogForPost(post: ReaderPost, isAskingToFollow: Boolean, actionListener: ActionListener) =
            ReaderBlogActions.followBlogForPost(post, isAskingToFollow, actionListener)
}
