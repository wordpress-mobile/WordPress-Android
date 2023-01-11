package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState.AlreadyRunning
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState.Failed
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState.Failed.RequestFailed
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState.Success
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState.Unchanged
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class PostLikeUseCase @Inject constructor(
    private val readerPostActionsWrapper: ReaderPostActionsWrapper,
    private val readerTracker: ReaderTracker,
    private val accountStore: AccountStore,
    private val networkUtilsWrapper: NetworkUtilsWrapper
) {
    private val continuations:
            MutableMap<PostLikeRequest, Continuation<PostLikeState>?> = mutableMapOf()

    suspend fun perform(
        post: ReaderPost,
        isAskingToLike: Boolean,
        source: String
    ) = flow {
        val wpComUserId = accountStore.account.userId
        val request = PostLikeRequest(post.postId, post.blogId, isAskingToLike, wpComUserId)

        if (!networkUtilsWrapper.isNetworkAvailable()) {
            emit(Failed.NoNetwork)
            return@flow
        }

        // There is already an action running for this request
        if (continuations[request] != null) {
            emit(AlreadyRunning)
            return@flow
        }

        // track like event
        trackEvent(request, post, source)

        handleLocalDb(post, request)
    }

    private fun trackEvent(
        request: PostLikeRequest,
        post: ReaderPost,
        source: String
    ) {
        if (request.isAskingToLike) {
            val likedStat = if (source == ReaderTracker.SOURCE_POST_DETAIL) {
                AnalyticsTracker.Stat.READER_ARTICLE_DETAIL_LIKED
            } else {
                AnalyticsTracker.Stat.READER_ARTICLE_LIKED
            }
            readerTracker.trackPost(likedStat, post, source)
            // Consider a like to be enough to push a page view - solves a long-standing question
            // from folks who ask 'why do I have more likes than page views?'.
            readerPostActionsWrapper.bumpPageViewForPost(post)
        } else {
            val unLikedStat = if (source == ReaderTracker.SOURCE_POST_DETAIL) {
                AnalyticsTracker.Stat.READER_ARTICLE_DETAIL_UNLIKED
            } else {
                AnalyticsTracker.Stat.READER_ARTICLE_UNLIKED
            }
            readerTracker.trackPost(unLikedStat, post, source)
        }
    }

    private suspend fun FlowCollector<PostLikeState>.handleLocalDb(
        post: ReaderPost,
        request: PostLikeRequest
    ) {
        val response = readerPostActionsWrapper.performLikeActionLocal(
            post,
            request.isAskingToLike,
            request.wpComUserId
        )
        if (response) {
            emit(PostLikeState.PostLikedInLocalDb)
            val postLikeState = performLikeAndWaitForResult(post, request)
            emit(postLikeState)
        } else {
            emit(Unchanged)
        }
    }

    private suspend fun performLikeAndWaitForResult(
        post: ReaderPost,
        request: PostLikeRequest
    ): PostLikeState {
        val actionListener = ActionListener { succeeded ->
            val postLikeState = if (succeeded) {
                Success
            } else {
                RequestFailed
            }
            continuations[request]?.resume(postLikeState)
            continuations[request] = null
        }

        return suspendCancellableCoroutine { cont ->
            continuations[request] = cont
            readerPostActionsWrapper.performLikeActionRemote(
                post,
                request.isAskingToLike,
                request.wpComUserId,
                actionListener
            )
        }
    }

    sealed class PostLikeState {
        object Success : PostLikeState()
        object PostLikedInLocalDb : PostLikeState()
        object AlreadyRunning : PostLikeState()
        object Unchanged : PostLikeState()
        sealed class Failed : PostLikeState() {
            object NoNetwork : Failed()
            object RequestFailed : Failed()
        }
    }

    data class PostLikeRequest(
        val postId: Long,
        val blogId: Long,
        val isAskingToLike: Boolean,
        val wpComUserId: Long
    )
}
