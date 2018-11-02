package org.wordpress.android.fluxc.network.utils

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.SiteUtils
import java.util.Date

const val DATE_FORMAT_DAY = "yyyy-MM-dd"
const val DATE_FORMAT_WEEK = "yyyy-'W'ww"
const val DATE_FORMAT_MONTH = "yyyy-MM"
const val DATE_FORMAT_YEAR = "yyyy"

enum class StatsGranularity {
    DAYS, WEEKS, MONTHS, YEARS;
}

fun getFormattedDate(site: SiteModel, date: Date, granularity: StatsGranularity): String {
    return when (granularity) {
        StatsGranularity.DAYS -> SiteUtils.getDateTimeForSite(site, DATE_FORMAT_DAY, date)
        StatsGranularity.WEEKS -> SiteUtils.getDateTimeForSite(site, DATE_FORMAT_WEEK, date)
        StatsGranularity.MONTHS -> SiteUtils.getDateTimeForSite(site, DATE_FORMAT_MONTH, date)
        StatsGranularity.YEARS -> SiteUtils.getDateTimeForSite(site, DATE_FORMAT_YEAR, date)
    }
}
