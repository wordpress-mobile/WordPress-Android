package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_DETAIL_LIKED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_DETAIL_UNLIKED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_LIKED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_UNLIKED
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState.AlreadyRunning
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState.Failed
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState.Failed.RequestFailed
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState.Success
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState.Unchanged
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class PostLikeUseCase @Inject constructor(
    private val readerPostActionsWrapper: ReaderPostActionsWrapper,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    private val accountStore: AccountStore,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private val continuations:
            MutableMap<PostLikeRequest, Continuation<PostLikeState>?> = mutableMapOf()

    suspend fun perform(
        post: ReaderPost,
        isAskingToLike: Boolean,
        fromPostDetails: Boolean = false
    ) = flow<PostLikeState> {
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
        trackEvent(request, post, fromPostDetails)

        handleLocalDb(post, request)
    }

    private fun trackEvent(
        request: PostLikeRequest,
        post: ReaderPost,
        fromPostDetails: Boolean
    ) {
        if (request.isAskingToLike) {
            val likedStat = if (fromPostDetails) {
                READER_ARTICLE_DETAIL_LIKED
            } else {
                READER_ARTICLE_LIKED
            }
            analyticsUtilsWrapper.trackWithReaderPostDetails(likedStat, post)
            // Consider a like to be enough to push a page view - solves a long-standing question
            // from folks who ask 'why do I have more likes than page views?'.
            readerPostActionsWrapper.bumpPageViewForPost(post)
        } else {
            val unLikedStat = if (fromPostDetails) {
                READER_ARTICLE_DETAIL_UNLIKED
            } else {
                READER_ARTICLE_UNLIKED
            }
            analyticsUtilsWrapper.trackWithReaderPostDetails(unLikedStat, post)
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
