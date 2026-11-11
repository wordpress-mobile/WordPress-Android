package org.wordpress.android.ui.newstats.mostpopulartime

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.datasource.StatsInsightsData
import org.wordpress.android.ui.newstats.repository.InsightsResult
import org.wordpress.android.util.DateFormatWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.math.round

@HiltViewModel
class MostPopularTimeViewModel @Inject constructor(
    private val dateFormatWrapper: DateFormatWrapper,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<MostPopularTimeCardUiState>(
        MostPopularTimeCardUiState.Loading
    )
    val uiState: StateFlow<MostPopularTimeCardUiState> = _uiState.asStateFlow()

    fun handleResult(result: InsightsResult) {
        val use24Hour = dateFormatWrapper.is24HourFormat()
        _uiState.value = when (result) {
            is InsightsResult.Success -> mapToUiState(result.data, use24Hour)
            is InsightsResult.Error -> MostPopularTimeCardUiState.Error(
                message = resourceProvider.getString(R.string.stats_error_api)
            )
        }
    }

    fun showLoading() {
        _uiState.value = MostPopularTimeCardUiState.Loading
    }

    companion object {
        internal fun mapToUiState(
            data: StatsInsightsData,
            use24HourFormat: Boolean
        ): MostPopularTimeCardUiState {
            if (data.highestDayPercent == 0.0 || data.highestHourPercent == 0.0) {
                return MostPopularTimeCardUiState.NoData
            }
            return MostPopularTimeCardUiState.Loaded(
                bestDay = formatDayOfWeek(data.highestDayOfWeek),
                bestDayPercent = formatPercent(data.highestDayPercent),
                bestHour = formatHour(data.highestHour, use24HourFormat),
                bestHourPercent = formatPercent(data.highestHourPercent)
            )
        }

        private const val DAYS_IN_WEEK = 7

        private fun formatDayOfWeek(wpDayOfWeek: Int): String {
            if (wpDayOfWeek !in 0 until DAYS_IN_WEEK) return ""
            val dayOfWeek = DayOfWeek.of((wpDayOfWeek % DAYS_IN_WEEK) + 1)
            return dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        }

        private const val MAX_HOUR = 23

        private fun formatHour(hour: Int, use24HourFormat: Boolean): String {
            if (hour !in 0..MAX_HOUR) return ""
            val time = LocalTime.of(hour, 0)
            val pattern = if (use24HourFormat) "HH:mm" else "h:mm a"
            return time.format(
                DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            )
        }

        private fun formatPercent(percent: Double): String {
            return round(percent).toInt().toString()
        }
    }
}
