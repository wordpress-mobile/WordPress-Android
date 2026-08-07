package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.wordpress.android.ui.reader.actions.ReaderActions
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState.AlreadyRunning
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState.Failed.NoNetwork
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState.Failed.NotAuthorised
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState.Failed.PostNotFound
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState.Failed.RequestFailed
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState.Success
import org.wordpress.android.util.NetworkUtilsWrapper
import java.net.HttpURLConnection
import java.util.Collections
import javax.inject.Inject
import kotlin.coroutines.resume

class ReaderFetchPostUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val readerPostActionsWrapper: ReaderPostActionsWrapper
) {
    // Tracks in-flight fetches so a second request for the same post short-circuits to
    // AlreadyRunning. Synchronized because the request callbacks fire on a background thread.
    private val inFlightRequests = Collections.synchronizedSet(mutableSetOf<FetchPostRequestParams>())

    suspend fun fetchPost(blogId: Long, postId: Long, isFeed: Boolean): FetchReaderPostState {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return NoNetwork
        }

        val requestParams = FetchPostRequestParams(blogId, postId, isFeed)
        // add() returns false when an identical request is already in flight
        if (!inFlightRequests.add(requestParams)) {
            return AlreadyRunning
        }

        return try {
            // Never wait forever: a request that neither succeeds nor fails (e.g. a stalled
            // connection) resolves to RequestFailed so the caller can leave the loading state.
            val statusCode = withTimeoutOrNull(FETCH_TIMEOUT_MS) {
                fetchPostAndWaitForResult(requestParams)
            }
            when (statusCode) {
                HttpURLConnection.HTTP_OK -> Success
                HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN -> NotAuthorised
                HttpURLConnection.HTTP_NOT_FOUND -> PostNotFound
                else -> RequestFailed
            }
        } finally {
            // Always release the slot, including on timeout or coroutine cancellation, so a
            // later retry of the same post isn't wrongly rejected as AlreadyRunning.
            inFlightRequests.remove(requestParams)
        }
    }

    private suspend fun fetchPostAndWaitForResult(requestParams: FetchPostRequestParams): Int =
        suspendCancellableCoroutine { cont ->
            val listener = object : ReaderActions.OnRequestListener<String> {
                override fun onSuccess(result: String?) {
                    // Guard against resuming an already-cancelled (e.g. timed-out) request
                    if (cont.isActive) cont.resume(HttpURLConnection.HTTP_OK)
                }

                override fun onFailure(statusCode: Int) {
                    if (cont.isActive) cont.resume(statusCode)
                }
            }

            if (requestParams.isFeed) {
                readerPostActionsWrapper.requestFeedPost(
                    feedId = requestParams.blogId,
                    postId = requestParams.postId,
                    requestListener = listener
                )
            } else {
                readerPostActionsWrapper.requestBlogPost(
                    blogId = requestParams.blogId,
                    postId = requestParams.postId,
                    requestListener = listener
                )
            }
        }

    sealed class FetchReaderPostState {
        object Success : FetchReaderPostState()
        object AlreadyRunning : FetchReaderPostState()
        sealed class Failed : FetchReaderPostState() {
            object NoNetwork : Failed()
            object NotAuthorised : Failed()
            object PostNotFound : Failed()
            object RequestFailed : Failed()
        }
    }

    data class FetchPostRequestParams(
        val blogId: Long,
        val postId: Long,
        val isFeed: Boolean
    )

    companion object {
        private const val FETCH_TIMEOUT_MS = 30_000L
    }
}
