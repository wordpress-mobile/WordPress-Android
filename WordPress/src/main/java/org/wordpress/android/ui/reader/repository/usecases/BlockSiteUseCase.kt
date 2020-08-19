package org.wordpress.android.ui.reader.repository.usecases

import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_BLOG_BLOCKED
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener
import org.wordpress.android.ui.reader.actions.ReaderBlogActions.BlockedBlogResult
import org.wordpress.android.ui.reader.actions.ReaderBlogActionsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BlockSiteUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    private val readerBlogActionsWrapper: ReaderBlogActionsWrapper
) {
    private var continuation: Continuation<Boolean>? = null

    suspend fun blockSite(blogId: Long): SnackbarMessageHolder? {
        if (continuation != null) {
            // Blocking multiple sites in parallel isn't supported as the user would lose the ability to undo the action
            return null
        }
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return SnackbarMessageHolder(R.string.reader_toast_err_block_blog)
        }

        // We want to track the action no matter the result
        analyticsUtilsWrapper.trackWithSiteId(READER_BLOG_BLOCKED, blogId)

        val actionListener = ActionListener { succeeded ->
            continuation?.resume(succeeded)
            continuation = null
        }
        var blockedBlogResult: BlockedBlogResult? = null

        val succeeded = suspendCoroutine<Boolean> { cont ->
            continuation = cont
            blockedBlogResult = readerBlogActionsWrapper.blockBlogFromReader(blogId, actionListener)
            // wait for the result
        }

        return if (succeeded) {
            // show the undo snackbar enabling the user to undo the block
            SnackbarMessageHolder(R.string.reader_toast_blog_blocked, R.string.undo, {
                readerBlogActionsWrapper.undoBlockBlogFromReader(blockedBlogResult!!)
            })
        } else {
            readerBlogActionsWrapper.undoBlockBlogFromReader(blockedBlogResult!!)
            SnackbarMessageHolder(R.string.reader_toast_err_block_blog)
        }
    }
}
