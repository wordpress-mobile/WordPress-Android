package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.ui.reader.actions.ReaderActions
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState.AlreadyRunning
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState.Failed.NoNetwork
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState.Failed.RequestFailed
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState.Success
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState.Failed.NotAuthorised
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState.Failed.PostNotFound
import org.wordpress.android.util.NetworkUtilsWrapper
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
                    STATUS_CODE_200 -> Success
                    STATUS_CODE_401, STATUS_CODE_403 -> NotAuthorised
                    STATUS_CODE_404 -> PostNotFound
                    else -> RequestFailed
                }
            }
        }
    }

    private suspend fun fetchPostAndWaitForResult(requestParams: FetchPostRequestParams): Int {
        val listener = object : ReaderActions.OnRequestListener {
            override fun onSuccess() {
                continuations[requestParams]?.resume(STATUS_CODE_200)
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

    companion object {
        const val STATUS_CODE_200 = 200
        const val STATUS_CODE_401 = 401
        const val STATUS_CODE_403 = 403
        const val STATUS_CODE_404 = 404
    }
}
