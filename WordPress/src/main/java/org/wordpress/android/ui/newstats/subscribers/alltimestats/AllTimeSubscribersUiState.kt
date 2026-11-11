package org.wordpress.android.ui.newstats.subscribers.alltimestats

/**
 * UI state for the All-time Subscribers card.
 */
sealed class AllTimeSubscribersUiState {
    data object Loading : AllTimeSubscribersUiState()

    data class Loaded(
        val currentCount: Long,
        val count30DaysAgo: Long,
        val count60DaysAgo: Long,
        val count90DaysAgo: Long
    ) : AllTimeSubscribersUiState()

    data class Error(
        val message: String,
        val isAuthError: Boolean = false
    ) : AllTimeSubscribersUiState()
}
