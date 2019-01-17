package org.wordpress.android.fluxc.network.rest.wpcom.stats.time

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.CurrentDateUtils
import org.wordpress.android.fluxc.utils.SiteUtils
import java.util.Date
import javax.inject.Inject

const val DATE_FORMAT_DAY = "yyyy-MM-dd"

class StatsUtils
@Inject constructor(private val currentDateUtils: CurrentDateUtils) {
    fun getFormattedDate(site: SiteModel, date: Date? = null): String {
        return SiteUtils.getDateTimeForSite(site, DATE_FORMAT_DAY, date ?: currentDateUtils.getCurrentDate())
    }
}
