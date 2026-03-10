package org.wordpress.android.ui.newstats.alltimestats

sealed class AllTimeStatsCardUiState {
    data object Loading : AllTimeStatsCardUiState()

    data class Loaded(
        val views: Long,
        val visitors: Long,
        val posts: Long,
        val comments: Long
    ) : AllTimeStatsCardUiState()

    class Error(
        val message: String,
        val onRetry: () -> Unit
    ) : AllTimeStatsCardUiState()
}
