package org.wordpress.android.ui.reader.usecases

import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateBlogInfoListener
import org.wordpress.android.ui.reader.actions.ReaderBlogActionsWrapper
import org.wordpress.android.ui.reader.usecases.ReaderFetchSiteUseCase.FetchSiteState.AlreadyRunning
import org.wordpress.android.ui.reader.usecases.ReaderFetchSiteUseCase.FetchSiteState.Failed.NoNetwork
import org.wordpress.android.ui.reader.usecases.ReaderFetchSiteUseCase.FetchSiteState.Failed.RequestFailed
import org.wordpress.android.ui.reader.usecases.ReaderFetchSiteUseCase.FetchSiteState.Success
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ReaderFetchSiteUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val readerBlogActionsWrapper: ReaderBlogActionsWrapper
) {
    private val continuations: MutableMap<FetchSiteRequestParams, Continuation<ReaderBlog?>?> = mutableMapOf()

    suspend fun fetchSite(blogId: Long, feedId: Long, blogUrl: String? = null): FetchSiteState {
        return if (!networkUtilsWrapper.isNetworkAvailable()) {
            NoNetwork
        } else {
            val requestParams = FetchSiteRequestParams(blogId, feedId, blogUrl)
            // There is already an action running for this request
            if (continuations[requestParams] != null) {
                AlreadyRunning
            } else {
                fetchSiteAndWaitForResult(requestParams)?.let { Success } ?: RequestFailed
            }
        }
    }

    private suspend fun fetchSiteAndWaitForResult(requestParams: FetchSiteRequestParams): ReaderBlog? {
        val actionListener = UpdateBlogInfoListener { readerBlog ->
            continuations[requestParams]?.resume(readerBlog)
            continuations[requestParams] = null
        }

        return suspendCoroutine { cont ->
            continuations[requestParams] = cont
            readerBlogActionsWrapper.updateBlogInfo(
                requestParams.blogId,
                requestParams.feedId,
                requestParams.blogUrl,
                actionListener
            )
        }
    }

    sealed class FetchSiteState {
        object Success : FetchSiteState()
        object AlreadyRunning : FetchSiteState()
        sealed class Failed : FetchSiteState() {
            object NoNetwork : Failed()
            object RequestFailed : Failed()
        }
    }

    data class FetchSiteRequestParams(
        val blogId: Long,
        val feedId: Long,
        val blogUrl: String?
    )
}
