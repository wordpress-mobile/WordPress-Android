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
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.Param.FromPost
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.Param.RecommendedSite
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

    suspend fun toggleFollow(param: Param) = flow<FollowSiteState> {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            emit(NoNetwork)
        } else {
            val isAskingToFollow = when (param) {
                is FromPost -> !readerPostTableWrapper.isPostFollowed(param.post)
                is RecommendedSite -> param.isAskingToFollow
            }
            val showEnableNotification = when (param) {
                is FromPost -> !readerUtilsWrapper.isExternalFeed(param.blogId, param.feedId) &&
                        !param.post.isFollowedByCurrentUser
                is RecommendedSite -> false // todo: figure out logic for recommended site
            }
            emit(PostFollowStatusChanged(param.blogId, isAskingToFollow, showEnableNotification))
            performAction(param.blogId, param.feedId, isAskingToFollow)
        }
    }

    private suspend fun FlowCollector<FollowSiteState>.performAction(
        blogId: Long,
        feedId: Long,
        isAskingToFollow: Boolean
    ) {
        val succeeded = followSiteAndWaitForResult(blogId, feedId, isAskingToFollow)
        if (!succeeded) {
            emit(PostFollowStatusChanged(blogId, !isAskingToFollow))
            emit(RequestFailed)
        } else {
            val deleteNotificationSubscription = !readerUtilsWrapper.isExternalFeed(blogId, feedId) &&
                    !isAskingToFollow
            emit(
                    PostFollowStatusChanged(
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

    sealed class Param {
        abstract val blogId: Long
        abstract val feedId: Long
        data class FromPost(
            val post: ReaderPost
        ) : Param() {
            override val blogId: Long = post.blogId
            override val feedId: Long = post.feedId
        }

        data class RecommendedSite(
            override val blogId: Long,
            override val feedId: Long,
            val isAskingToFollow: Boolean
        ) : Param()
    }
}
