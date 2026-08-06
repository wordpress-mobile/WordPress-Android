package org.wordpress.android.ui.newstats.poststats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.datasource.PostViewsData
import org.wordpress.android.ui.newstats.repository.PostViewsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

@HiltViewModel
class PostStatsDetailViewModel @Inject constructor(
    private val selectedSiteRepository:
        SelectedSiteRepository,
    private val statsRepository: StatsRepository,
    private val accountStore: AccountStore,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<PostStatsDetailUiState>(
            PostStatsDetailUiState.Loading
        )
    val uiState: StateFlow<PostStatsDetailUiState> =
        _uiState.asStateFlow()

    @Suppress(
        "TooGenericExceptionCaught",
        "InstanceOfCheckForException"
    )
    fun loadData(postId: Long) {
        val site = selectedSiteRepository.getSelectedSite()
        val token = accountStore.accessToken
        if (site == null || token.isNullOrEmpty()) {
            _uiState.value = PostStatsDetailUiState.Error(
                resourceProvider.getString(
                    R.string.stats_error_no_site
                )
            )
            return
        }
        statsRepository.init(token)

        _uiState.value = PostStatsDetailUiState.Loading
        viewModelScope.launch {
            try {
                val result =
                    statsRepository.fetchPostViews(
                        siteId = site.siteId,
                        postId = postId,
                        recentDays = RECENT_DAYS
                    )
                _uiState.value = when (result) {
                    is PostViewsResult.Success ->
                        PostStatsDetailUiState.Loaded(
                            result.data
                        )
                    is PostViewsResult.Error ->
                        PostStatsDetailUiState.Error(
                            resourceProvider.getString(
                                R.string.stats_error_api
                            )
                        )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLog.e(
                    AppLog.T.STATS,
                    "Error fetching post views: " +
                        "${e.message}",
                    e
                )
                _uiState.value =
                    PostStatsDetailUiState.Error(
                        resourceProvider.getString(
                            R.string.stats_error_unknown
                        )
                    )
            }
        }
    }

    companion object {
        // A month of daily views for the detail chart.
        private const val RECENT_DAYS = 30
    }
}

sealed class PostStatsDetailUiState {
    data object Loading : PostStatsDetailUiState()

    data class Loaded(
        val data: PostViewsData
    ) : PostStatsDetailUiState()

    data class Error(
        val message: String
    ) : PostStatsDetailUiState()
}
