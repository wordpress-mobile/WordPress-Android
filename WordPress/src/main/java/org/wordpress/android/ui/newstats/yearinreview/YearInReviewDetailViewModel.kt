package org.wordpress.android.ui.newstats.yearinreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.InsightsResult
import org.wordpress.android.ui.newstats.repository.StatsInsightsUseCase
import org.wordpress.android.ui.newstats.yearinreview.YearSummary.Companion.ensureCurrentYear
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class YearInReviewDetailViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val statsInsightsUseCase: StatsInsightsUseCase,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<YearInReviewDetailUiState>(
        YearInReviewDetailUiState.Loading
    )
    val uiState: StateFlow<YearInReviewDetailUiState> = _uiState.asStateFlow()

    private val isLoaded = AtomicBoolean(false)
    private val isLoading = AtomicBoolean(false)

    fun loadData() {
        if (isLoaded.get() || !isLoading.compareAndSet(false, true)) return
        fetchData()
    }

    @Suppress("TooGenericExceptionCaught", "InstanceOfCheckForException")
    private fun fetchData() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            isLoading.set(false)
            _uiState.value = YearInReviewDetailUiState.Error(
                resourceProvider.getString(R.string.stats_error_no_site)
            )
            return
        }

        viewModelScope.launch {
            try {
                val result = statsInsightsUseCase(siteId = site.siteId)
                isLoaded.set(result is InsightsResult.Success)
                handleResult(result)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLog.e(
                    AppLog.T.STATS,
                    "Error fetching year in review: ${e.message}",
                    e
                )
                isLoaded.set(false)
                _uiState.value = YearInReviewDetailUiState.Error(
                    resourceProvider.getString(R.string.stats_error_unknown)
                )
            } finally {
                isLoading.set(false)
            }
        }
    }

    private fun handleResult(result: InsightsResult) {
        when (result) {
            is InsightsResult.Success -> {
                val years = result.data.years
                    .map { YearSummary.fromInsightsData(it) }
                    .ensureCurrentYear()
                    .sortedByDescending { it.year }
                _uiState.value = YearInReviewDetailUiState.Loaded(years = years)
            }
            is InsightsResult.Error -> {
                _uiState.value = YearInReviewDetailUiState.Error(
                    resourceProvider.getString(R.string.stats_error_api)
                )
            }
        }
    }
}

sealed class YearInReviewDetailUiState {
    data object Loading : YearInReviewDetailUiState()
    data class Loaded(val years: List<YearSummary>) : YearInReviewDetailUiState()
    data class Error(val message: String) : YearInReviewDetailUiState()
}
