package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener
import org.wordpress.android.ui.reader.actions.ReaderBlogActionsWrapper
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.Failed.NoNetwork
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.Failed.RequestFailed
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.PostFollowStatusChanged
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.Success
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * This class handles reader post follow click events.
 */
class ReaderSiteFollowUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val readerBlogActionsWrapper: ReaderBlogActionsWrapper,
    private val readerPostTableWrapper: ReaderPostTableWrapper,
    private val readerUtilsWrapper: ReaderUtilsWrapper
) {
    private var continuation: Continuation<Boolean>? = null

    suspend fun toggleFollow(post: ReaderPost) = flow<FollowSiteState> {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            emit(NoNetwork)
        } else {
            val isAskingToFollow = !readerPostTableWrapper.isPostFollowed(post)
            val showEnableNotification = !readerUtilsWrapper.isExternalFeed(post.blogId, post.feedId) &&
                    !post.isFollowedByCurrentUser
            emit(PostFollowStatusChanged(post.blogId, isAskingToFollow, showEnableNotification))

            performAction(post, isAskingToFollow)
        }
    }

    private suspend fun FlowCollector<FollowSiteState>.performAction(post: ReaderPost, isAskingToFollow: Boolean) {
        val succeeded = followSiteAndWaitForResult(post, isAskingToFollow)
        if (!succeeded) {
            emit(PostFollowStatusChanged(post.blogId, !isAskingToFollow))
            emit(RequestFailed)
        } else {
            val deleteNotificationSubscription = !readerUtilsWrapper.isExternalFeed(post.blogId, post.feedId) &&
                    !isAskingToFollow
            emit(PostFollowStatusChanged(
                    post.blogId,
                    isAskingToFollow,
                    deleteNotificationSubscription = deleteNotificationSubscription))
            emit(Success)
        }
    }

    private suspend fun followSiteAndWaitForResult(post: ReaderPost, isAskingToFollow: Boolean): Boolean {
        val actionListener = ActionListener { succeeded ->
            continuation?.resume(succeeded)
            continuation = null
        }

        return suspendCoroutine { cont ->
            continuation = cont
            readerBlogActionsWrapper.followBlogForPost(post, isAskingToFollow, actionListener)
        }
    }

    sealed class FollowSiteState {
        data class PostFollowStatusChanged(
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
}
