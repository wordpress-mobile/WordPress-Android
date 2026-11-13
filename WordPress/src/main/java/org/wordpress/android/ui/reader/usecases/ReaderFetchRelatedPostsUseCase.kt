package org.wordpress.android.ui.reader.usecases

import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.models.ReaderSimplePostList
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.ui.reader.usecases.ReaderFetchRelatedPostsUseCase.FetchRelatedPostsState.AlreadyRunning
import org.wordpress.android.ui.reader.usecases.ReaderFetchRelatedPostsUseCase.FetchRelatedPostsState.Failed.NoNetwork
import org.wordpress.android.ui.reader.usecases.ReaderFetchRelatedPostsUseCase.FetchRelatedPostsState.Failed.RequestFailed
import org.wordpress.android.ui.reader.usecases.ReaderFetchRelatedPostsUseCase.FetchRelatedPostsState.Success
import org.wordpress.android.util.NetworkUtilsWrapper
import java.util.Collections
import javax.inject.Inject

class ReaderFetchRelatedPostsUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val readerPostRepository: ReaderPostRepository
) {
    private val inFlightRequests = Collections.synchronizedSet(mutableSetOf<RelatedPostsRequest>())

    suspend fun fetchRelatedPosts(sourcePost: ReaderPost): FetchRelatedPostsState {
        if (!networkUtilsWrapper.isNetworkAvailable()) return NoNetwork

        val request = RelatedPostsRequest(postId = sourcePost.postId, blogId = sourcePost.blogId)
        if (!inFlightRequests.add(request)) return AlreadyRunning
        return try {
            readerPostRepository.fetchRelatedPosts(sourcePost)
                ?.let { (local, global) -> Success(localRelatedPosts = local, globalRelatedPosts = global) }
                ?: RequestFailed
        } finally {
            inFlightRequests.remove(request)
        }
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
