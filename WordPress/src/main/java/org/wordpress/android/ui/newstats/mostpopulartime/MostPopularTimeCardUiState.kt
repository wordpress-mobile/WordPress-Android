package org.wordpress.android.ui.newstats.mostpopulartime

sealed class MostPopularTimeCardUiState {
    data object Loading : MostPopularTimeCardUiState()

    data object NoData : MostPopularTimeCardUiState()

    data class Loaded(
        val bestDay: String,
        val bestDayPercent: String,
        val bestHour: String,
        val bestHourPercent: String
    ) : MostPopularTimeCardUiState()

    data class Error(
        val message: String
    ) : MostPopularTimeCardUiState()
}
