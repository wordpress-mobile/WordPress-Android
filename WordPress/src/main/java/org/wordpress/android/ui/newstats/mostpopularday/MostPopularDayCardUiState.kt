package org.wordpress.android.ui.newstats.mostpopularday

sealed class MostPopularDayCardUiState {
    data object Loading : MostPopularDayCardUiState()

    data class Loaded(
        val dayAndMonth: String,
        val year: String,
        val views: Long,
        val viewsPercentage: String
    ) : MostPopularDayCardUiState()

    class Error(
        val message: String,
        val onRetry: () -> Unit
    ) : MostPopularDayCardUiState()
}
