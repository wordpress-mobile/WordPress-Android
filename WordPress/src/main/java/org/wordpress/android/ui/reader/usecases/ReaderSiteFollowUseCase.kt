package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener
import org.wordpress.android.ui.reader.actions.ReaderBlogActionsWrapper
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.Failed.NoNetwork
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.Failed.RequestFailed
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.FollowStatusChanged
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.Success
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * This class handles reader blog follow click events.
 */
class ReaderSiteFollowUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val readerBlogActionsWrapper: ReaderBlogActionsWrapper,
    private val readerBlogTableWrapper: ReaderBlogTableWrapper,
    private val readerUtilsWrapper: ReaderUtilsWrapper
) {
    private var continuation: Continuation<Boolean>? = null

    suspend fun toggleFollow(param: Param) = flow<FollowSiteState> {
        val (blogId, feedId) = param
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            emit(NoNetwork)
        } else {
            val isAskingToFollow = !readerBlogTableWrapper.isSiteFollowed(blogId, feedId)
            val showEnableNotification = !readerUtilsWrapper.isExternalFeed(blogId, feedId) && isAskingToFollow

            emit(FollowStatusChanged(blogId, isAskingToFollow, showEnableNotification))
            performAction(blogId, feedId, isAskingToFollow)
        }
    }

    private suspend fun FlowCollector<FollowSiteState>.performAction(
        blogId: Long,
        feedId: Long,
        isAskingToFollow: Boolean
    ) {
        val succeeded = followSiteAndWaitForResult(blogId, feedId, isAskingToFollow)
        if (!succeeded) {
            emit(FollowStatusChanged(blogId, !isAskingToFollow))
            emit(RequestFailed)
        } else {
            val deleteNotificationSubscription = !readerUtilsWrapper.isExternalFeed(blogId, feedId) &&
                    !isAskingToFollow
            emit(
                    FollowStatusChanged(
                            blogId,
                            isAskingToFollow,
                            deleteNotificationSubscription = deleteNotificationSubscription
                    )
            )
            emit(Success)
        }
    }

    private suspend fun followSiteAndWaitForResult(blogId: Long, feedId: Long, isAskingToFollow: Boolean): Boolean {
        val actionListener = ActionListener { succeeded ->
            continuation?.resume(succeeded)
            continuation = null
        }

        return suspendCoroutine { cont ->
            continuation = cont
            readerBlogActionsWrapper.followBlog(blogId, feedId, isAskingToFollow, actionListener)
        }
    }

    sealed class FollowSiteState {
        data class FollowStatusChanged(
            val blogId: Long,
            val following: Boolean,
            val showEnableNotification: Boolean = false,
            val deleteNotificationSubscription: Boolean = false
        ) : FollowSiteState()

        object Success : FollowSiteState()
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
