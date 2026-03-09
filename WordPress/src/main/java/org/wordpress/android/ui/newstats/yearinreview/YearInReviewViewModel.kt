package org.wordpress.android.ui.newstats.yearinreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.datasource.YearInsightsData
import org.wordpress.android.ui.newstats.repository.InsightsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.Year
import javax.inject.Inject

@HiltViewModel
class YearInReviewViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<YearInReviewCardUiState>(
            YearInReviewCardUiState.Loading
        )
    val uiState: StateFlow<YearInReviewCardUiState> =
        _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> =
        _isRefreshing.asStateFlow()

    private var isLoading = false
    private var isLoadedSuccessfully = false

    fun loadDataIfNeeded() {
        if (isLoadedSuccessfully || isLoading) return
        isLoading = true
        loadData()
    }

    fun refresh() {
        val site = selectedSiteRepository
            .getSelectedSite() ?: return
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                loadDataInternal(site)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadData() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            isLoading = false
            _uiState.value = YearInReviewCardUiState.Error(
                message = resourceProvider.getString(
                    R.string.stats_error_no_site
                ),
                onRetry = ::loadData
            )
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            isLoading = false
            _uiState.value = YearInReviewCardUiState.Error(
                message = resourceProvider.getString(
                    R.string.stats_error_api
                ),
                onRetry = ::loadData
            )
            return
        }

        statsRepository.init(accessToken)
        _uiState.value = YearInReviewCardUiState.Loading

        viewModelScope.launch {
            try {
                loadDataInternal(site)
            } finally {
                isLoading = false
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadDataInternal(site: SiteModel) {
        try {
            val result = statsRepository
                .fetchInsights(site.siteId)
            when (result) {
                is InsightsResult.Success -> {
                    isLoadedSuccessfully = true
                    val years = result.years
                        .map { it.toUiModel() }
                        .ensureCurrentYear()
                        .sortedByDescending {
                            it.year
                        }
                    _uiState.value =
                        YearInReviewCardUiState.Loaded(
                            years = years
                        )
                }
                is InsightsResult.Error -> {
                    _uiState.value =
                        YearInReviewCardUiState.Error(
                            message = resourceProvider
                                .getString(
                                    R.string.stats_error_api
                                ),
                            onRetry = ::loadData
                        )
                }
            }
        } catch (e: Exception) {
            _uiState.value = YearInReviewCardUiState.Error(
                message = e.message
                    ?: resourceProvider.getString(
                        R.string.stats_error_unknown
                    ),
                onRetry = ::loadData
            )
        }
    }

    fun onRetry() {
        loadData()
    }

    fun getDetailData(): List<YearSummary> {
        val state = _uiState.value
        return if (state is YearInReviewCardUiState.Loaded) {
            state.years
        } else {
            emptyList()
        }
    }

    companion object {
        private val CURRENT_YEAR = Year.now().toString()

        private fun YearInsightsData.toUiModel() =
            YearSummary(
                year = year,
                totalPosts = totalPosts,
                totalWords = totalWords,
                avgWords = avgWords,
                totalLikes = totalLikes,
                avgLikes = avgLikes,
                totalComments = totalComments,
                avgComments = avgComments
            )

        private fun List<YearSummary>.ensureCurrentYear():
            List<YearSummary> {
            return if (any { it.year == CURRENT_YEAR }) {
                this
            } else {
                this + YearSummary(
                    year = CURRENT_YEAR,
                    totalPosts = 0L,
                    totalWords = 0L,
                    avgWords = 0.0,
                    totalLikes = 0L,
                    avgLikes = 0.0,
                    totalComments = 0L,
                    avgComments = 0.0
                )
            }
        }
    }
}
