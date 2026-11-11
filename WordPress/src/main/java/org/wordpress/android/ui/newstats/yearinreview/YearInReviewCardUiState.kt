package org.wordpress.android.ui.newstats.yearinreview

import org.wordpress.android.ui.newstats.datasource.YearInsightsData
import java.time.Year

sealed class YearInReviewCardUiState {
    data object Loading : YearInReviewCardUiState()

    data class Loaded(
        val years: List<YearSummary>
    ) : YearInReviewCardUiState()

    data class Error(
        val message: String
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
) {
    companion object {
        fun fromInsightsData(data: YearInsightsData) = YearSummary(
            year = data.year,
            totalPosts = data.totalPosts,
            totalWords = data.totalWords,
            avgWords = data.avgWords,
            totalLikes = data.totalLikes,
            avgLikes = data.avgLikes,
            totalComments = data.totalComments,
            avgComments = data.avgComments
        )

        fun List<YearSummary>.ensureCurrentYear(): List<YearSummary> {
            val currentYear = Year.now().value.toString()
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
