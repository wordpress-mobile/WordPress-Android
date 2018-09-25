package org.wordpress.android.fluxc.model.stats

data class InsightsMostPopularModel(
    val highestDayOfWeek: Int,
    val highestHour: Int,
    val highestDayPercent: Double,
    val highestHourPercent: Double
)
