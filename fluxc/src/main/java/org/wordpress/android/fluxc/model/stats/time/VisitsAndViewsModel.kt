package org.wordpress.android.fluxc.model.stats.time

import org.wordpress.android.fluxc.network.utils.StatsGranularity

data class VisitsAndViewsModel(val period: String, val dates: List<PeriodData>) {
    data class PeriodData(
        val period: String,
        val views: Long,
        val visitors: Long,
        val likes: Long,
        val reblogs: Long,
        val comments: Long,
        val posts: Long
    )
}
