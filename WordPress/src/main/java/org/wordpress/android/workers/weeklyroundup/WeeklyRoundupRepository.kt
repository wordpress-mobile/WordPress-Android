package org.wordpress.android.workers.weeklyroundup

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters.previousOrSame
import java.util.Locale
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

        return getLastWeekPeriodData(visitsAndViewsModel)
    }

    private fun getLastWeekPeriodData(visitsAndViewsModel: VisitsAndViewsModel): PeriodData? {
        val currentDateForSite = parseStandardDate(visitsAndViewsModel.period) ?: return null
        val lastWeekStartDate = currentDateForSite.minusWeeks(1).with(previousOrSame(MONDAY))
        return visitsAndViewsModel.dates.find { parseWeekPeriodDate(it.period) == lastWeekStartDate }
    }

    companion object {
        private const val STANDARD_DATE_PATTERN = "yyyy-MM-dd"
        private const val WEEK_PERIOD_DATE_PATTERN = "yyyy'W'MM'W'dd"

        private val standardFormatter = DateTimeFormatter.ofPattern(STANDARD_DATE_PATTERN, Locale.ROOT)
        private val weekPeriodFormatter = DateTimeFormatter.ofPattern(WEEK_PERIOD_DATE_PATTERN, Locale.ROOT)

        fun parseStandardDate(date: String): LocalDate? = safelyParseDate(date, standardFormatter)

        fun parseWeekPeriodDate(date: String): LocalDate? = safelyParseDate(date, weekPeriodFormatter)

        private fun safelyParseDate(
            date: String,
            formatter: DateTimeFormatter
        ) = runCatching { LocalDate.parse(date, formatter) }
                .onFailure { AppLog.e(T.NOTIFS, "Weekly Roundup – Couldn't parse date: $date", it) }
                .getOrNull()
    }
}
