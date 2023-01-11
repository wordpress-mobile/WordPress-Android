package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener
import org.wordpress.android.ui.reader.actions.ReaderBlogActions.BlockedBlogResult
import org.wordpress.android.ui.reader.actions.ReaderBlogActionsWrapper
import org.wordpress.android.ui.reader.repository.usecases.BlockSiteState.Failed.AlreadyRunning
import org.wordpress.android.ui.reader.repository.usecases.BlockSiteState.Failed.NoNetwork
import org.wordpress.android.ui.reader.repository.usecases.BlockSiteState.Failed.RequestFailed
import org.wordpress.android.ui.reader.repository.usecases.BlockSiteState.SiteBlockedInLocalDb
import org.wordpress.android.ui.reader.repository.usecases.BlockSiteState.Success
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BlockBlogUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val readerTracker: ReaderTracker,
    private val readerBlogActionsWrapper: ReaderBlogActionsWrapper
) {
    private var continuation: Continuation<Boolean>? = null

    // TODO use .flowOn(ioDispatcher) when the experimental flag is removed
    suspend fun blockBlog(
        blogId: Long,
        feedId: Long
    ) = flow {
        // Blocking multiple sites in parallel isn't supported as the user would lose the ability to undo the action
        if (continuation == null) {
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                emit(NoNetwork)
            } else {
                performAction(blogId, feedId)
            }
        } else {
            emit(AlreadyRunning)
        }
    }

    private suspend fun FlowCollector<BlockSiteState>.performAction(
        blogId: Long,
        feedId: Long
    ) {
        // We want to track the action no matter the result
        readerTracker.trackBlog(
            AnalyticsTracker.Stat.READER_BLOG_BLOCKED,
            blogId,
            feedId
        )
        val blockedBlogData = readerBlogActionsWrapper.blockBlogFromReaderLocal(blogId, feedId)
        emit(SiteBlockedInLocalDb(blockedBlogData))

        val succeeded = blockBlogAndWaitForResult(blockedBlogData)

        if (succeeded) {
            emit(Success)
        } else {
            emit(RequestFailed)
        }
    }

    private suspend fun blockBlogAndWaitForResult(blockedBlogResult: BlockedBlogResult): Boolean {
        val actionListener = ActionListener { succeeded ->
            continuation?.resume(succeeded)
            continuation = null
        }

        return suspendCoroutine { cont ->
            continuation = cont
            readerBlogActionsWrapper.blockBlogFromReaderRemote(blockedBlogResult, actionListener)
        }
    }
}

sealed class BlockSiteState {
    data class SiteBlockedInLocalDb(val blockedBlogData: BlockedBlogResult) : BlockSiteState()
    object Success : BlockSiteState()
    sealed class Failed : BlockSiteState() {
        object NoNetwork : Failed()
        object RequestFailed : Failed()
        object AlreadyRunning : Failed()
    }
}
