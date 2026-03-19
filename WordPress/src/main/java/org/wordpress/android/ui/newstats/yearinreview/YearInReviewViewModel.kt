package org.wordpress.android.ui.newstats.yearinreview

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.datasource.YearInsightsData
import org.wordpress.android.ui.newstats.repository.InsightsResult
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class YearInReviewViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<YearInReviewCardUiState>(
            YearInReviewCardUiState.Loading
        )
    val uiState: StateFlow<YearInReviewCardUiState> =
        _uiState.asStateFlow()

    fun handleResult(result: InsightsResult) {
        _uiState.value = when (result) {
            is InsightsResult.Success -> {
                val years = result.data.years
                    .map { it.toUiModel() }
                    .ensureCurrentYear()
                    .sortedByDescending {
                        it.year
                    }
                YearInReviewCardUiState.Loaded(
                    years = years
                )
            }
            is InsightsResult.Error ->
                YearInReviewCardUiState.Error(
                    message = resourceProvider
                        .getString(
                            R.string.stats_error_api
                        )
                )
        }
    }

    fun showLoading() {
        _uiState.value = YearInReviewCardUiState.Loading
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
            val currentYear = Calendar.getInstance()
                .get(Calendar.YEAR).toString()
            return if (any { it.year == currentYear }) {
                this
            } else {
                this + YearSummary(
                    year = currentYear,
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
