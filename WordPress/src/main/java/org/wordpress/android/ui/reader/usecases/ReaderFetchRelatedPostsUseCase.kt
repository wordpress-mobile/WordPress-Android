package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.ReaderEvents.RelatedPostsUpdated
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
import org.wordpress.android.ui.reader.models.ReaderSimplePostList
import org.wordpress.android.ui.reader.usecases.ReaderFetchRelatedPostsUseCase.FetchRelatedPostsState.AlreadyRunning
import org.wordpress.android.ui.reader.usecases.ReaderFetchRelatedPostsUseCase.FetchRelatedPostsState.Failed.NoNetwork
import org.wordpress.android.ui.reader.usecases.ReaderFetchRelatedPostsUseCase.FetchRelatedPostsState.Failed.RequestFailed
import org.wordpress.android.ui.reader.usecases.ReaderFetchRelatedPostsUseCase.FetchRelatedPostsState.Success
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class ReaderFetchRelatedPostsUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val readerPostActionsWrapper: ReaderPostActionsWrapper
) {
    private val continuations: MutableMap<RelatedPostsRequest, Continuation<FetchRelatedPostsState>?> = mutableMapOf()

    suspend fun fetchRelatedPosts(sourcePost: ReaderPost): FetchRelatedPostsState {
        val request = RelatedPostsRequest(postId = sourcePost.postId, blogId = sourcePost.blogId)

        return when {
            continuations[request] != null -> AlreadyRunning

            !networkUtilsWrapper.isNetworkAvailable() -> NoNetwork

            else -> {
                suspendCancellableCoroutine { cont ->
                    continuations[request] = cont
                    readerPostActionsWrapper.requestRelatedPosts(sourcePost)
                }
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onRelatedPostUpdated(event: RelatedPostsUpdated) {
        val result = if (event.didSucceed()) {
            Success(
                localRelatedPosts = event.localRelatedPosts,
                globalRelatedPosts = event.globalRelatedPosts
            )
        } else {
            RequestFailed
        }

        val request = RelatedPostsRequest(postId = event.sourcePostId, blogId = event.sourceSiteId)
        continuations[request]?.resume(result)
        continuations[request] = null
    }

    sealed class FetchRelatedPostsState {
        data class Success(
            val localRelatedPosts: ReaderSimplePostList,
            val globalRelatedPosts: ReaderSimplePostList
        ) : FetchRelatedPostsState()

        object AlreadyRunning : FetchRelatedPostsState()
        sealed class Failed : FetchRelatedPostsState() {
            object NoNetwork : Failed()
            object RequestFailed : Failed()
        }
    }

    data class RelatedPostsRequest(
        val postId: Long,
        val blogId: Long
    )
}
