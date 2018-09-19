package org.wordpress.android.fluxc.model.stats

data class InsightsAllTimeModel(
    val siteId: Long,
    val date: String? = null,
    val visitors: Int,
    val views: Int,
    val posts: Int,
    val viewsBestDay: String,
    val viewsBestDayTotal: Int
)
