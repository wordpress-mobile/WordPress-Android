package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener
import org.wordpress.android.ui.reader.actions.ReaderBlogActionsWrapper
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.AlreadyRunning
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
    private val readerUtilsWrapper: ReaderUtilsWrapper,
    private val readerTracker: ReaderTracker
) {
    private val continuations: MutableMap<Param, Continuation<Boolean>?> = mutableMapOf()

    suspend fun toggleFollow(param: Param) = flow {
        val (blogId, feedId) = param
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            emit(NoNetwork)
        } else {
            // There is already an action running for this request
            if (continuations[param] != null) {
                emit(AlreadyRunning)
                return@flow
            }
            val isAskingToFollow = !readerBlogTableWrapper.isSiteFollowed(blogId, feedId)
            val showEnableNotification = !readerUtilsWrapper.isExternalFeed(blogId, feedId) && isAskingToFollow

            emit(FollowStatusChanged(blogId, isAskingToFollow, showEnableNotification))
            performAction(param, isAskingToFollow)
        }
    }

    private suspend fun FlowCollector<FollowSiteState>.performAction(param: Param, isAskingToFollow: Boolean) {
        val succeeded = followSiteAndWaitForResult(param, isAskingToFollow)
        if (!succeeded) {
            emit(FollowStatusChanged(param.blogId, !isAskingToFollow))
            emit(RequestFailed)
        } else {
            val deleteNotificationSubscription = !readerUtilsWrapper.isExternalFeed(param.blogId, param.feedId) &&
                    !isAskingToFollow
            emit(
                    FollowStatusChanged(
                            param.blogId,
                            isAskingToFollow,
                            deleteNotificationSubscription = deleteNotificationSubscription
                    )
            )
            emit(Success)
        }
    }

    private suspend fun followSiteAndWaitForResult(param: Param, isAskingToFollow: Boolean): Boolean {
        val actionListener = ActionListener { succeeded ->
            continuations[param]?.resume(succeeded)
            continuations[param] = null
        }

        return suspendCoroutine { cont ->
            continuations[param] = cont
            readerBlogActionsWrapper.followBlog(
                    param.blogId,
                    param.feedId,
                    isAskingToFollow,
                    actionListener,
                    readerTracker
            )
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
