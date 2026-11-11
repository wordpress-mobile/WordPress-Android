package org.wordpress.android.ui.newstats.yearinreview

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.repository.InsightsResult
import org.wordpress.android.ui.newstats.yearinreview.YearSummary.Companion.ensureCurrentYear
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@HiltViewModel
class YearInReviewViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<YearInReviewCardUiState>(
        YearInReviewCardUiState.Loading
    )
    val uiState: StateFlow<YearInReviewCardUiState> = _uiState.asStateFlow()

    fun handleResult(result: InsightsResult) {
        _uiState.value = when (result) {
            is InsightsResult.Success -> {
                val years = result.data.years
                    .map { YearSummary.fromInsightsData(it) }
                    .ensureCurrentYear()
                    .sortedByDescending { it.year }
                YearInReviewCardUiState.Loaded(years = years)
            }
            is InsightsResult.Error -> YearInReviewCardUiState.Error(
                message = resourceProvider.getString(R.string.stats_error_api)
            )
        }
    }

    fun showLoading() {
        _uiState.value = YearInReviewCardUiState.Loading
    }
}
