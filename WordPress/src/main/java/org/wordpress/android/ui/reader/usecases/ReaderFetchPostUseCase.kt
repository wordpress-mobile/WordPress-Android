package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.suspendCancellableCoroutine
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
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class ReaderFetchPostUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val readerPostActionsWrapper: ReaderPostActionsWrapper
) {
    private val continuations: MutableMap<FetchPostRequestParams, Continuation<Int>?> = mutableMapOf()

    suspend fun fetchPost(blogId: Long, postId: Long, isFeed: Boolean): FetchReaderPostState {
        return if (!networkUtilsWrapper.isNetworkAvailable()) {
            NoNetwork
        } else {
            val requestParams = FetchPostRequestParams(blogId, postId, isFeed)
            // There is already an action running for this request
            if (continuations[requestParams] != null) {
                AlreadyRunning
            } else {
                when (fetchPostAndWaitForResult(requestParams)) {
                    HttpURLConnection.HTTP_OK -> Success
                    HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN -> NotAuthorised
                    HttpURLConnection.HTTP_NOT_FOUND -> PostNotFound
                    else -> RequestFailed
                }
            }
        }
    }

    private suspend fun fetchPostAndWaitForResult(requestParams: FetchPostRequestParams): Int {
        val listener = object : ReaderActions.OnRequestListener<String> {
            override fun onSuccess(result: String?) {
                continuations[requestParams]?.resume(HttpURLConnection.HTTP_OK)
                continuations[requestParams] = null
            }

            override fun onFailure(statusCode: Int) {
                continuations[requestParams]?.resume(statusCode)
                continuations[requestParams] = null
            }
        }

        return suspendCancellableCoroutine { cont ->
            continuations[requestParams] = cont

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
}
