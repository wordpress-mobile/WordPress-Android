package org.wordpress.android.fluxc.utils

import org.wordpress.android.fluxc.model.SiteModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private const val DATE_FORMAT_DEFAULT = "yyyy-MM-dd"


    /**
     * Given a [SiteModel] and a [String] compatible with [SimpleDateFormat]
     * and a {@param dateString}
     * returns a formatted date that accounts for the site's timezone setting.
     *
     */
    fun getDateTimeForSite(
        site: SiteModel,
        pattern: String,
        dateString: String?
    ): String {
        val currentDate = Date()

        if (dateString.isNullOrEmpty()) {
            return SiteUtils.getDateTimeForSite(site, pattern, currentDate)
        }

        /*
         * Since only date is provided without the time,
         * by default the time is set to the start of the day.
         *
         * This might cause timezone issues so getting the current time
         * and setting this time to the date value
         * */
        val now = Calendar.getInstance()
        now.time = currentDate

        val date = getDateFromString(dateString)
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
        calendar.add(Calendar.MINUTE, now.get(Calendar.MINUTE))
        calendar.add(Calendar.SECOND, now.get(Calendar.SECOND))
        return SiteUtils.getDateTimeForSite(site, pattern, calendar.time)
    }

    /**
     * returns a [Date] instance
     * based on {@param pattern} and {@param dateString}
     */
    fun getDateFromString(dateString: String, pattern: String = DATE_FORMAT_DEFAULT): Date {
        val dateFormat = SimpleDateFormat(pattern, Locale.ROOT)
        return dateFormat.parse(dateString)
    }

    /**
     * returns a [String] formatted
     * based on {@param pattern} and {@param date}
     */
    fun formatDate(
        pattern: String,
        date: Date
    ): String {
        val dateFormat = SimpleDateFormat(pattern, Locale.ROOT)
        return dateFormat.format(date)
    }
}
