package org.wordpress.android.workers.weeklyroundup

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData

data class WeeklyRoundupData(
    val site: SiteModel,
    val period: String,
    val views: Long,
    val likes: Long,
    val comments: Long
) {
    companion object {
        fun create(site: SiteModel, periodData: PeriodData) = WeeklyRoundupData(
                site = site,
                period = periodData.period,
                views = periodData.views,
                likes = periodData.likes,
                comments = periodData.comments
        )
    }
}
