package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.BACKGROUND
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.reader.actions.ReaderPostActions
import org.wordpress.android.ui.reader.repository.ReaderRepositoryEvent
import org.wordpress.android.ui.reader.repository.ReaderRepositoryEvent.PostLikeEnded
import org.wordpress.android.util.EventBusWrapper
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PostLikeActionUseCase @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val eventBusWrapper: EventBusWrapper
) : ReaderRepositoryDispatchingUseCase(ioDispatcher) {
    private val continuations: MutableMap<PostLikeRequest, Continuation<ReaderRepositoryEvent>?> = mutableMapOf()

    init {
        eventBusWrapper.register(this)
    }

    suspend fun perform(
        post: ReaderPost,
        isAskingToLike: Boolean,
        wpComUserId: Long
    ): ReaderRepositoryEvent {
        val request = PostLikeRequest(post.postId, post.blogId, isAskingToLike, wpComUserId)

        if (continuations[request] != null) {
            throw IllegalStateException("Perform Like action has already been sent for this request")
        }

        return suspendCoroutine { cont ->
            continuations[request] = cont
            ReaderPostActions.performLikeAction(post, isAskingToLike, wpComUserId)
        }
    }

    @Subscribe(threadMode = BACKGROUND)
    @SuppressWarnings("unused")
    fun onPerformPostLikeEnded(event: ReaderRepositoryEvent) {
        if (event is PostLikeEnded) {
            val request = PostLikeRequest(event.postId, event.blogId, event.isAskingToLike, event.wpComUserId)
            continuations[request]?.resume(event)
            continuations[request] = null
        }
    }

    override fun stop() {
        super.stop()
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
