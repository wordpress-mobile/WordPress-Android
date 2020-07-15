package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.BACKGROUND
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.reader.actions.ReaderPostActions
import org.wordpress.android.ui.reader.repository.OnReaderRepositoryEvent
import org.wordpress.android.ui.reader.repository.OnReaderRepositoryEvent.OnPostLikeEnded
import org.wordpress.android.util.EventBusWrapper
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PostLikeActionUseCase @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val eventBusWrapper: EventBusWrapper
) : ReaderRepositoryDispatchingUseCase(ioDispatcher) {
    private var continuation: kotlin.coroutines.Continuation<OnReaderRepositoryEvent>? = null

    init {
        eventBusWrapper.register(this)
    }

    suspend fun perform(
        post: ReaderPost,
        isAskingToLike: Boolean,
        wpComUserId: Long
    ): OnReaderRepositoryEvent {
        if (continuation != null) {
            throw IllegalStateException("Perform Like action has already been sent")
        }
        return suspendCoroutine { cont ->
            continuation = cont
            ReaderPostActions.performLikeAction(post, isAskingToLike, wpComUserId)
           // ReaderPostTable.getBlogPost(post.blogId, post.postId, false)
        }
    }

    @Subscribe(threadMode = BACKGROUND)
    @SuppressWarnings("unused")
    fun onPerformPostLikeEnded(event: OnReaderRepositoryEvent) {
        if (event is OnPostLikeEnded) {
            continuation?.resume(event)
            continuation = null
        }
    }

    override fun stop() {
        super.stop()
        eventBusWrapper.unregister(this)
    }
}
