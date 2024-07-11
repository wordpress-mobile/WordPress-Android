package org.wordpress.android.fluxc.model.stats.insights

import java.util.Date

data class PostingActivityModel(val streak: StreakModel, val months: List<Month>, val max: Int, val hasMore: Boolean) {
    data class StreakModel(
        val currentStreakStart: Date?,
        val currentStreakEnd: Date?,
        val currentStreakLength: Int?,
        val longestStreakStart: Date?,
        val longestStreakEnd: Date?,
        val longestStreakLength: Int?
    )

    data class Month(val year: Int, val month: Int, val days: Map<Int, Int>)
    data class Day(val year: Int, val month: Int, val day: Int)
}
