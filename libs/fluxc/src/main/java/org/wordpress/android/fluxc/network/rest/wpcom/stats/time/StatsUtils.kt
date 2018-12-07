package org.wordpress.android.fluxc.network.rest.wpcom.stats.time

import android.text.TextUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.CurrentDateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class StatsUtils
@Inject constructor(val locale: Locale, private val currentDateUtils: CurrentDateUtils) {
    fun getCurrentDateTZ(site: SiteModel): String {
        val statsDatePattern = "yyyy-MM-dd"
        return getCurrentDateTimeTZ(site.timezone, statsDatePattern, currentDateUtils.getCurrentDate())
    }

    private fun getCurrentDateTimeTZ(blogTimeZoneOption: String?, pattern: String, date: Date): String {
        val gmtDf = SimpleDateFormat(pattern, locale)

        if (blogTimeZoneOption == null) {
            return gmtDf.format(date)
        }

        /*
        Convert the timezone to a form that is compatible with Java TimeZone class
        WordPress returns something like the following:
        UTC+0:30 ----> 0.5
        UTC+1 ----> 1.0
        UTC-0:30 ----> -1.0
        */

        var timezoneNormalized: String
        if (TextUtils.isEmpty(blogTimeZoneOption) || blogTimeZoneOption == "0" || blogTimeZoneOption == "0.0") {
            timezoneNormalized = "GMT"
        } else {
            val timezoneSplitted = org.apache.commons.lang3.StringUtils.split(blogTimeZoneOption, ".")
            timezoneNormalized = timezoneSplitted[0]
            if (timezoneSplitted.size > 1 && timezoneSplitted[1] == "5") {
                timezoneNormalized += ":30"
            }
            timezoneNormalized = if (timezoneNormalized.startsWith("-")) {
                "GMT$timezoneNormalized"
            } else {
                if (timezoneNormalized.startsWith("+")) {
                    "GMT$timezoneNormalized"
                } else {
                    "GMT+$timezoneNormalized"
                }
            }
        }

        gmtDf.timeZone = TimeZone.getTimeZone(timezoneNormalized)
        return gmtDf.format(date)
    }
}
