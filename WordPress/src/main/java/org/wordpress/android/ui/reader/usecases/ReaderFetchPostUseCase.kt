package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.withTimeoutOrNull
import org.wordpress.android.ui.reader.repository.FetchPostResult
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
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

class ReaderFetchPostUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val readerPostRepository: ReaderPostRepository
) {
    // Tracks in-flight fetches so a second request for the same post short-circuits to AlreadyRunning
    private val inFlightRequests = Collections.synchronizedSet(mutableSetOf<FetchPostRequestParams>())

    @Suppress("ReturnCount") // early guard clauses (no network / already in flight) read cleaner than nesting
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
            val result = withTimeoutOrNull(FETCH_TIMEOUT_MS) {
                readerPostRepository.fetchPost(blogId, postId, isFeed)
            }
            when (result) {
                is FetchPostResult.Success -> Success
                is FetchPostResult.Failure -> FetchReaderPostState.fromStatusCode(result.statusCode)
                null -> RequestFailed
            }
        } finally {
            // Always release the slot, including on timeout or coroutine cancellation, so a
            // later retry of the same post isn't wrongly rejected as AlreadyRunning.
            inFlightRequests.remove(requestParams)
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

        companion object {
            /**
             * The failure a failed request's status code stands for
             */
            fun fromStatusCode(statusCode: Int): Failed = when (statusCode) {
                HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN -> NotAuthorised
                HttpURLConnection.HTTP_NOT_FOUND -> PostNotFound
                else -> RequestFailed
            }
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
