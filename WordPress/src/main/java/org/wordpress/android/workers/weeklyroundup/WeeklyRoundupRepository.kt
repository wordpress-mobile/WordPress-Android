package org.wordpress.android.workers.weeklyroundup

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import javax.inject.Inject

class WeeklyRoundupRepository @Inject constructor(
    private val visitsAndViewsStore: VisitsAndViewsStore
) {
    suspend fun fetchWeeklyRoundupData(site: SiteModel): PeriodData? {
        val response = visitsAndViewsStore.fetchVisits(site, WEEKS, LimitMode.Top(2), true)

        val visitsAndViewsModel = response.model

        if (visitsAndViewsModel == null) {
            val visitsAndViewsError = response.error
            // TODO Handle error
            return null
        }

        return visitsAndViewsModel.dates[0]
    }
}
