package org.wordpress.android.ui.reader.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResultListener
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Updates Reader posts from the network and reports when each update starts and ends through [events].
 * Updates run in the application scope so they finish, and their results are saved, even if the screen
 * that asked for them goes away. Every screen showing the same posts sees the events, so (for example) the
 * post list reloads when the detail pager loads older posts.
 */
@Singleton
class ReaderPostUpdater @Inject constructor(
    private val readerPostRepository: ReaderPostRepository,
    @Named(APPLICATION_SCOPE) private val applicationScope: CoroutineScope,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    private val _events = MutableSharedFlow<ReaderPostListEvent>(extraBufferCapacity = EVENT_BUFFER)
    val events: SharedFlow<ReaderPostListEvent> = _events.asSharedFlow()

    fun updatePosts(target: ReaderPostListTarget, action: UpdateAction) {
        // blog and feed updates have always been reported without a tag
        val tag = (target as? ReaderPostListTarget.Tag)?.tag
        applicationScope.launch(ioDispatcher) {
            _events.emit(ReaderPostListEvent.UpdateStarted(tag, action))
            val result = requestPosts(target, action)
            _events.emit(ReaderPostListEvent.UpdateEnded(tag, action, result))
        }
    }

    fun searchPosts(query: String, offset: Int) {
        applicationScope.launch(ioDispatcher) {
            _events.emit(ReaderPostListEvent.SearchStarted(query, offset))
            val didSucceed = readerPostRepository.searchPosts(query, offset)
            _events.emit(ReaderPostListEvent.SearchEnded(query, offset, didSucceed))
        }
    }

    private suspend fun requestPosts(target: ReaderPostListTarget, action: UpdateAction): UpdateResult =
        suspendCancellableCoroutine { cont ->
            val listener = UpdateResultListener { result ->
                if (cont.isActive) cont.resume(result)
            }
            with(readerPostRepository) {
                when (target) {
                    is ReaderPostListTarget.Tag -> requestPostsWithTag(target.tag, action, listener)
                    is ReaderPostListTarget.Blog -> requestPostsForBlog(target.blogId, action, listener)
                    is ReaderPostListTarget.Feed -> requestPostsForFeed(target.feedId, action, listener)
                }
            }
        }

    companion object {
        private const val EVENT_BUFFER = 16
    }
}
