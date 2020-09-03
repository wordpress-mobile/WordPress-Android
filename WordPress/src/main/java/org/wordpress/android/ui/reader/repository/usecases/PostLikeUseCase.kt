package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
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

    suspend fun perform(post: ReaderPost, isAskingToLike: Boolean) = flow<PostLikeState> {
        val wpComUserId = accountStore.account.userId
        val request = PostLikeRequest(post.postId, post.blogId, isAskingToLike, wpComUserId)

        if (!networkUtilsWrapper.isNetworkAvailable()) {
            emit(Failed.NoNetwork(request))
        }

        // There is already an action running for this request
        if (continuations[request] != null) {
            emit(AlreadyRunning(request))
        }

        // track like action
        if (request.isAskingToLike) {
            analyticsUtilsWrapper.trackWithReaderPostDetails(READER_ARTICLE_LIKED, post)
            // Consider a like to be enough to push a page view - solves a long-standing question
            // from folks who ask 'why do I have more likes than page views?'.
            readerPostActionsWrapper.bumpPageViewForPost(post)
        } else {
            analyticsUtilsWrapper.trackWithReaderPostDetails(READER_ARTICLE_UNLIKED, post)
        }

        handleLocalDb(post, request)
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
            emit(PostLikeState.PostLikedInLocalDb(request))
            val postLikeState = performLikeAndWaitForResult(post, request)
            emit(postLikeState)
        } else {
            emit(Unchanged(request)) // this can be considered a success
        }
    }

    private suspend fun performLikeAndWaitForResult(
        post: ReaderPost,
        request: PostLikeRequest
    ): PostLikeState {
        val actionListener = ActionListener { succeeded ->
            val postLikeState = if (succeeded) {
                Success(request)
            } else {
                RequestFailed(request)
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

    fun stop() {
        continuations.run { clear() }
    }

    sealed class PostLikeState {
        abstract val request: PostLikeRequest

        data class Success(override val request: PostLikeRequest) : PostLikeState()
        data class PostLikedInLocalDb(override val request: PostLikeRequest) : PostLikeState()
        data class AlreadyRunning(override val request: PostLikeRequest) : PostLikeState()
        data class Unchanged(override val request: PostLikeRequest) : PostLikeState()
        sealed class Failed : PostLikeState() {
            data class NoNetwork(override val request: PostLikeRequest) : Failed()
            data class RequestFailed(override val request: PostLikeRequest) : Failed()
        }
    }

    data class PostLikeRequest(
        val postId: Long,
        val blogId: Long,
        val isAskingToLike: Boolean,
        val wpComUserId: Long
    )
}
