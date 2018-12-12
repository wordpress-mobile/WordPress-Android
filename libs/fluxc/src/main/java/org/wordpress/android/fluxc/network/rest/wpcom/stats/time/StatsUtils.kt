package org.wordpress.android.fluxc.network.rest.wpcom.stats.time

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.CurrentDateUtils
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.getFormattedDate
import java.util.Date
import javax.inject.Inject

class StatsUtils
@Inject constructor(private val currentDateUtils: CurrentDateUtils) {
    fun getFormattedDate(site: SiteModel, granularity: StatsGranularity, date: Date? = null): String {
        return getFormattedDate(site, date ?: currentDateUtils.getCurrentDate(), granularity)
    }
}
