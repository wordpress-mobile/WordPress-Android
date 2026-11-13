package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.ui.reader.repository.ReaderBlogRepository
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.AlreadyRunning
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.Failed.NoNetwork
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.Failed.RequestFailed
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.FollowStatusChanged
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.Success
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

/**
 * This class handles reader blog follow click events.
 */
class ReaderSiteFollowUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val readerBlogRepository: ReaderBlogRepository,
    private val readerBlogTableWrapper: ReaderBlogTableWrapper,
    private val readerUtilsWrapper: ReaderUtilsWrapper
) {
    private val runningRequests = mutableSetOf<Param>()

    suspend fun toggleFollow(
        param: Param,
        source: String
    ) = flow {
        val (blogId, feedId) = param
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            emit(NoNetwork)
        } else {
            // There is already an action running for this request
            if (param in runningRequests) {
                emit(AlreadyRunning)
                return@flow
            }
            val isAskingToFollow = !readerBlogTableWrapper.isSiteFollowed(blogId, feedId)
            val showEnableNotification = !readerUtilsWrapper.isExternalFeed(blogId, feedId) && isAskingToFollow

            emit(FollowStatusChanged(blogId, feedId, isAskingToFollow, showEnableNotification, isChangeFinal = false))
            performAction(param, isAskingToFollow, source)
        }
    }

    private suspend fun FlowCollector<FollowSiteState>.performAction(
        param: Param,
        isAskingToFollow: Boolean,
        source: String
    ) {
        val succeeded = followSiteAndWaitForResult(param, isAskingToFollow, source)
        if (!succeeded) {
            emit(FollowStatusChanged(param.blogId, param.feedId, !isAskingToFollow, isChangeFinal = true))
            emit(RequestFailed)
        } else {
            val deleteNotificationSubscription = !readerUtilsWrapper.isExternalFeed(param.blogId, param.feedId) &&
                    !isAskingToFollow
            emit(
                FollowStatusChanged(
                    param.blogId,
                    param.feedId,
                    isAskingToFollow,
                    deleteNotificationSubscription = deleteNotificationSubscription,
                    isChangeFinal = true,
                )
            )
            emit(Success)
        }
    }

    private suspend fun followSiteAndWaitForResult(
        param: Param,
        isAskingToFollow: Boolean,
        source: String
    ): Boolean {
        runningRequests += param
        return try {
            readerBlogRepository.followSite(param.blogId, param.feedId, isAskingToFollow, source)
        } finally {
            runningRequests -= param
        }
    }

    sealed class FollowSiteState {
        data class FollowStatusChanged(
            val blogId: Long,
            val feedId: Long,
            val following: Boolean,
            val showEnableNotification: Boolean = false,
            val deleteNotificationSubscription: Boolean = false,
            val isChangeFinal: Boolean = true,
        ) : FollowSiteState()

        object Success : FollowSiteState()
        object AlreadyRunning : FollowSiteState()
        sealed class Failed : FollowSiteState() {
            object NoNetwork : Failed()
            object RequestFailed : Failed()
        }
    }

    data class Param(
        val blogId: Long,
        val feedId: Long,
        val blogName: String
    )
}
