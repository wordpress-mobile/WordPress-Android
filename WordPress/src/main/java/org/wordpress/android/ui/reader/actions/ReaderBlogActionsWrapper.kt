package org.wordpress.android.ui.reader.actions

import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateBlogInfoListener
import org.wordpress.android.ui.reader.actions.ReaderBlogActions.BlockedBlogResult
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import javax.inject.Inject

class ReaderBlogActionsWrapper @Inject constructor(
    private val readerUtilsWrapper: ReaderUtilsWrapper
) {
    fun blockBlogFromReaderLocal(blogId: Long): BlockedBlogResult = ReaderBlogActions.blockBlogFromReaderLocal(blogId)

    fun blockBlogFromReaderRemote(blockedBlogResult: BlockedBlogResult, actionListener: ActionListener?): Unit =
            ReaderBlogActions.blockBlogFromReaderRemote(blockedBlogResult, actionListener)

    fun undoBlockBlogFromReader(blocked: BlockedBlogResult) = ReaderBlogActions.undoBlockBlogFromReader(blocked)

    fun followBlog(blogId: Long, feedId: Long, isAskingToFollow: Boolean, actionListener: ActionListener) =
            ReaderBlogActions.followBlog(blogId, feedId, isAskingToFollow, actionListener)

    fun updateBlogInfo(blogId: Long, feedId: Long, blogUrl: String?, infoListener: UpdateBlogInfoListener) =
            if (readerUtilsWrapper.isExternalFeed(blogId, feedId)) {
                ReaderBlogActions.updateFeedInfo(blogId, blogUrl, infoListener)
            } else {
                ReaderBlogActions.updateBlogInfo(blogId, blogUrl, infoListener)
            }
}
