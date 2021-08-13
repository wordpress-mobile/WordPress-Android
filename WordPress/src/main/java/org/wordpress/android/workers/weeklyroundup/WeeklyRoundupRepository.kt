package org.wordpress.android.workers.weeklyroundup

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.workers.weeklyroundup.WeeklyRoundupUtils.parseStandardDate
import org.wordpress.android.workers.weeklyroundup.WeeklyRoundupUtils.parseWeekPeriodDate
import java.time.DayOfWeek.MONDAY
import java.time.temporal.TemporalAdjusters.previousOrSame
import javax.inject.Inject

class WeeklyRoundupRepository @Inject constructor(
    private val visitsAndViewsStore: VisitsAndViewsStore
) {
    suspend fun fetchWeeklyRoundupData(site: SiteModel): WeeklyRoundupData? {
        val response = visitsAndViewsStore.fetchVisits(site, WEEKS, LimitMode.Top(2), true)

        val visitsAndViewsModel = response.model

        if (visitsAndViewsModel == null) {
            val visitsAndViewsError = response.error
            // TODO Handle error
            return null
        }

        return getLastWeekPeriodData(visitsAndViewsModel)?.let { WeeklyRoundupData.create(site, it) }
    }

    private fun getLastWeekPeriodData(visitsAndViewsModel: VisitsAndViewsModel): PeriodData? {
        val currentDateForSite = parseStandardDate(visitsAndViewsModel.period) ?: return null
        val lastWeekStartDate = currentDateForSite.minusWeeks(1).with(previousOrSame(MONDAY))
        return visitsAndViewsModel.dates.find { parseWeekPeriodDate(it.period) == lastWeekStartDate }
    }
}
