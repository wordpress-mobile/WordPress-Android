package org.wordpress.android.fluxc.network.utils

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.utils.SiteUtils
import java.util.Date

const val DATE_FORMAT_DAY = "yyyy-MM-dd"
const val DATE_FORMAT_WEEK = "yyyy-'W'ww"
const val DATE_FORMAT_MONTH = "yyyy-MM"
const val DATE_FORMAT_YEAR = "yyyy"

enum class StatsGranularity(private val value: String) {
    DAYS("day"),
    WEEKS("week"),
    MONTHS("month"),
    YEARS("year");

    override fun toString(): String {
        return value
    }}

fun getFormattedDate(site: SiteModel, date: Date, granularity: StatsGranularity): String {
    return when (granularity) {
        StatsGranularity.DAYS -> SiteUtils.getDateTimeForSite(site, DATE_FORMAT_DAY, date)
        StatsGranularity.WEEKS -> SiteUtils.getDateTimeForSite(site, DATE_FORMAT_WEEK, date)
        StatsGranularity.MONTHS -> SiteUtils.getDateTimeForSite(site, DATE_FORMAT_MONTH, date)
        StatsGranularity.YEARS -> SiteUtils.getDateTimeForSite(site, DATE_FORMAT_YEAR, date)
    }
}
