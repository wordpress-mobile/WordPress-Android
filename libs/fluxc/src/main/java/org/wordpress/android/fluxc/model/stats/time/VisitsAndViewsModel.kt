package org.wordpress.android.fluxc.model.stats.time

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
