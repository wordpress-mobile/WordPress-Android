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
    val score = views * VIEWS_WEIGHT + likes * LIKES_WEIGHT + comments * COMMENTS_WEIGHT

    companion object {
        private const val VIEWS_WEIGHT = 1
        private const val LIKES_WEIGHT = 0.5
        private const val COMMENTS_WEIGHT = 0.5

        fun create(site: SiteModel, periodData: PeriodData) = WeeklyRoundupData(
            site = site,
            period = periodData.period,
            views = periodData.views,
            likes = periodData.likes,
            comments = periodData.comments
        )
    }
}
