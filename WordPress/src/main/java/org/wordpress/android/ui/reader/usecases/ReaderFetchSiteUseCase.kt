package org.wordpress.android.ui.reader.usecases

import org.wordpress.android.ui.reader.repository.ReaderBlogRepository
import org.wordpress.android.ui.reader.usecases.ReaderFetchSiteUseCase.FetchSiteState.AlreadyRunning
import org.wordpress.android.ui.reader.usecases.ReaderFetchSiteUseCase.FetchSiteState.Failed.NoNetwork
import org.wordpress.android.ui.reader.usecases.ReaderFetchSiteUseCase.FetchSiteState.Failed.RequestFailed
import org.wordpress.android.ui.reader.usecases.ReaderFetchSiteUseCase.FetchSiteState.Success
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

class ReaderFetchSiteUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val readerBlogRepository: ReaderBlogRepository
) {
    private val runningRequests = mutableSetOf<FetchSiteRequestParams>()

    suspend fun fetchSite(blogId: Long, feedId: Long, blogUrl: String? = null): FetchSiteState {
        val requestParams = FetchSiteRequestParams(blogId, feedId, blogUrl)
        return when {
            !networkUtilsWrapper.isNetworkAvailable() -> NoNetwork
            // There is already an action running for this request
            requestParams in runningRequests -> AlreadyRunning
            else -> {
                runningRequests += requestParams
                try {
                    readerBlogRepository.fetchSiteInfo(blogId, feedId, blogUrl)?.let { Success } ?: RequestFailed
                } finally {
                    runningRequests -= requestParams
                }
            }
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
