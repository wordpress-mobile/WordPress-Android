package org.wordpress.android.fluxc.network.utils

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.SiteUtils
import java.util.Date

const val DATE_FORMAT_DAY = "yyyy-MM-dd"

enum class StatsGranularity(private val value: String) {
    DAYS("day"),
    WEEKS("week"),
    MONTHS("month"),
    YEARS("year");

    override fun toString(): String {
        return value
    }
}

fun getFormattedDate(site: SiteModel, date: Date): String {
    return SiteUtils.getDateTimeForSite(site, DATE_FORMAT_DAY, date)
}
