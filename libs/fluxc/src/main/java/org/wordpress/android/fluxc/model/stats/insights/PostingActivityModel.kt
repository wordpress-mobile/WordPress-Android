package org.wordpress.android.fluxc.model.stats.insights

import java.util.Date

data class PostingActivityModel(val streak: StreakModel, val events: List<StreakEvent>, val hasMore: Boolean) {
    data class StreakModel(
        val currentStreakStart: Date?,
        val currentStreakEnd: Date?,
        val currentStreakLength: Int?,
        val longestStreakStart: Date?,
        val longestStreakEnd: Date?,
        val longestStreakLength: Int?
    )
    data class StreakEvent(val date: Date, val postCount: Int)
}
