package org.wordpress.android.fluxc.model.stats

import java.util.Date

data class InsightsAllTimeModel(
    val siteId: Long,
    val date: Date? = null,
    val visitors: Int,
    val views: Int,
    val posts: Int,
    val viewsBestDay: String,
    val viewsBestDayTotal: Int
)
