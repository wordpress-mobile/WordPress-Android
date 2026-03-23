package org.wordpress.android.ui.newstats.mostpopularday

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.datasource.StatsSummaryData
import org.wordpress.android.ui.newstats.repository.StatsSummaryResult
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MostPopularDayViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<MostPopularDayCardUiState>(
        MostPopularDayCardUiState.Loading
    )
    val uiState: StateFlow<MostPopularDayCardUiState> = _uiState.asStateFlow()

    fun handleResult(result: StatsSummaryResult) {
        _uiState.value = when (result) {
            is StatsSummaryResult.Success -> mapToUiState(result.data)
            is StatsSummaryResult.Error -> MostPopularDayCardUiState.Error(
                message = resourceProvider.getString(R.string.stats_error_api)
            )
        }
    }

    fun showLoading() {
        _uiState.value = MostPopularDayCardUiState.Loading
    }

    companion object {
        private val INPUT_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE
        private const val DISPLAY_PATTERN = "MMMM d"
        private const val PERCENTAGE_MULTIPLIER = 100.0

        internal fun mapToUiState(data: StatsSummaryData): MostPopularDayCardUiState {
            val bestDay = data.viewsBestDay
            if (bestDay.isBlank()) return MostPopularDayCardUiState.NoData

            val parsed = parseBestDay(bestDay)
            val totalViews = data.views
            val bestDayViews = data.viewsBestDayTotal
            val percentage = if (totalViews > 0) {
                val pct = bestDayViews.toDouble() / totalViews.toDouble() * PERCENTAGE_MULTIPLIER
                String.format(Locale.getDefault(), "%.1f", pct)
            } else {
                "0"
            }
            return MostPopularDayCardUiState.Loaded(
                dayAndMonth = parsed.first,
                year = parsed.second,
                views = bestDayViews,
                viewsPercentage = percentage
            )
        }

        private fun parseBestDay(bestDay: String): Pair<String, String> {
            return try {
                val date = LocalDate.parse(bestDay, INPUT_FORMAT)
                val displayFormat = DateTimeFormatter.ofPattern(
                    DISPLAY_PATTERN, Locale.getDefault()
                )
                val dayMonth = date.format(displayFormat).replaceFirstChar { it.uppercase() }
                dayMonth to date.year.toString()
            } catch (
                @Suppress("TooGenericExceptionCaught")
                e: Exception
            ) {
                AppLog.w(
                    AppLog.T.STATS,
                    "Failed to parse bestDay '$bestDay': ${e.message}"
                )
                bestDay to ""
            }
        }
    }
}
