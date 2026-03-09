package org.wordpress.android.ui.newstats.yearinreview

sealed class YearInReviewCardUiState {
    data object Loading : YearInReviewCardUiState()

    data class Loaded(
        val years: List<YearSummary>
    ) : YearInReviewCardUiState()

    data class Error(
        val message: String,
        val onRetry: () -> Unit
    ) : YearInReviewCardUiState()
}

data class YearSummary(
    val year: String,
    val totalPosts: Long,
    val totalWords: Long,
    val avgWords: Double,
    val totalLikes: Long,
    val avgLikes: Double,
    val totalComments: Long,
    val avgComments: Double
)
