package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.BACKGROUND
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Failure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.SuccessWithData
import org.wordpress.android.ui.reader.repository.ReaderRepositoryEvent
import org.wordpress.android.ui.reader.repository.ReaderRepositoryEvent.PostLikeEnded
import org.wordpress.android.ui.reader.repository.ReaderRepositoryEvent.PostLikeEnded.PostLikeFailure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryEvent.PostLikeEnded.PostLikeSuccess
import org.wordpress.android.ui.reader.repository.ReaderRepositoryEvent.PostLikeEnded.PostLikeUnChanged
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class PostLikeUseCase @Inject constructor(
    private val eventBusWrapper: EventBusWrapper,
    private val readerPostActionsWrapper: ReaderPostActionsWrapper,
    private val accountStore: AccountStore,
    private val networkUtilsWrapper: NetworkUtilsWrapper
) {
    private val continuations:
            MutableMap<PostLikeRequest, Continuation<ReaderRepositoryCommunication>?> = mutableMapOf()

    init {
        eventBusWrapper.register(this)
    }

    suspend fun perform(
        post: ReaderPost,
        isAskingToLike: Boolean
    ): ReaderRepositoryCommunication {
        val wpComUserId = accountStore.account.userId
        val request = PostLikeRequest(post.postId, post.blogId, isAskingToLike, wpComUserId)

        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return NetworkUnavailable
        }

        if (continuations[request] != null) {
            return Failure(PostLikeUnChanged(
                    post.postId,
                    post.blogId,
                    isAskingToLike,
                    wpComUserId
            ))
        }

        return suspendCancellableCoroutine { cont ->
            continuations[request] = cont
            readerPostActionsWrapper.performLikeAction(post, isAskingToLike, wpComUserId)
        }
    }

    @Subscribe(threadMode = BACKGROUND)
    @SuppressWarnings("unused")
    fun onPerformPostLikeEnded(event: ReaderRepositoryEvent) {
        if (event is PostLikeEnded) {
            val request = PostLikeRequest(
                    event.postId,
                    event.blogId,
                    event.isAskingToLike,
                    event.wpComUserId
            )

            val comm: ReaderRepositoryCommunication = when (event) {
                is PostLikeUnChanged -> Failure(event)
                is PostLikeSuccess -> SuccessWithData(event)
                is PostLikeFailure -> Failure(event)
            }

            continuations[request]?.resume(comm)
            continuations[request] = null
        }
    }

    fun stop() {
        eventBusWrapper.unregister(this)
        continuations.run { clear() }
    }

    data class PostLikeRequest(
        val postId: Long,
        val blogId: Long,
        val isAskingToLike: Boolean,
        val wpComUserId: Long
    )
}
