package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.reader.repository.BlockedBlogResult
import org.wordpress.android.ui.reader.repository.ReaderBlogRepository
import org.wordpress.android.ui.reader.repository.usecases.BlockSiteState.Failed.AlreadyRunning
import org.wordpress.android.ui.reader.repository.usecases.BlockSiteState.Failed.NoNetwork
import org.wordpress.android.ui.reader.repository.usecases.BlockSiteState.Failed.RequestFailed
import org.wordpress.android.ui.reader.repository.usecases.BlockSiteState.SiteBlockedInLocalDb
import org.wordpress.android.ui.reader.repository.usecases.BlockSiteState.Success
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

class BlockBlogUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val readerTracker: ReaderTracker,
    private val readerBlogRepository: ReaderBlogRepository
) {
    private var isRunning = false

    // TODO use .flowOn(ioDispatcher) when the experimental flag is removed
    suspend fun blockBlog(
        blogId: Long,
        feedId: Long
    ) = flow {
        // Blocking multiple sites in parallel isn't supported as the user would lose the ability to undo the action
        if (!isRunning) {
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
        val blockedBlogData = readerBlogRepository.blockBlogLocal(blogId, feedId)
        emit(SiteBlockedInLocalDb(blockedBlogData))

        isRunning = true
        val succeeded = try {
            readerBlogRepository.blockBlogRemote(blockedBlogData)
        } finally {
            isRunning = false
        }

        if (succeeded) {
            emit(Success)
        } else {
            emit(RequestFailed)
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
