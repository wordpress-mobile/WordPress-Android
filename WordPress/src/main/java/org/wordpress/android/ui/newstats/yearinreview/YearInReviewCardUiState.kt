package org.wordpress.android.ui.newstats.yearinreview

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class YearInReviewCardUiState {
    data object Loading : YearInReviewCardUiState()

    data class Loaded(
        val years: List<YearSummary>
    ) : YearInReviewCardUiState()

    data class Error(
        val message: String
    ) : YearInReviewCardUiState()
}

@Parcelize
data class YearSummary(
    val year: String,
    val totalPosts: Long,
    val totalWords: Long,
    val avgWords: Double,
    val totalLikes: Long,
    val avgLikes: Double,
    val totalComments: Long,
    val avgComments: Double
) : Parcelable
