package org.wordpress.android.fluxc.utils

import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SiteUtilsTest {
    companion object {
        const val UTC8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX"

        const val DATE_FORMAT_DAY = "yyyy-MM-dd"
        const val DATE_FORMAT_WEEK = "yyyy-'W'ww"
        const val DATE_FORMAT_MONTH = "yyyy-MM"
        const val DATE_FORMAT_YEAR = "yyyy"
    }

    @Test
    fun testGetCurrentDateTimeUtcSite() {
        val siteModel = SiteModel()
        with(siteModel) {
            val formattedDate = SiteUtils.getCurrentDateTimeForSite(this, UTC8601_FORMAT)
            val currentTimeUtc = DateTimeUtils.iso8601UTCFromDate(Date())
            assertEquals(currentTimeUtc, formattedDate.replace("Z", "+00:00"))
        }

        siteModel.timezone = ""
        with(siteModel) {
            val formattedDate = SiteUtils.getCurrentDateTimeForSite(this, UTC8601_FORMAT)
            val currentTimeUtc = DateTimeUtils.iso8601UTCFromDate(Date())
            assertEquals(currentTimeUtc, formattedDate.replace("Z", "+00:00"))
        }

        siteModel.timezone = "0"
        with(siteModel) {
            val formattedDate = SiteUtils.getCurrentDateTimeForSite(this, UTC8601_FORMAT)
            val currentTimeUtc = DateTimeUtils.iso8601UTCFromDate(Date())
            assertEquals(currentTimeUtc, formattedDate.replace("Z", "+00:00"))
        }
    }

    @Test
    fun testGetCurrentDateTimeForNonUtcSite() {
        val hourFormat = SimpleDateFormat("HH", Locale.ROOT)

        val estSite = SiteModel().apply { timezone = "-4" }
        with(estSite) {
            val formattedDate = SiteUtils.getCurrentDateTimeForSite(this, UTC8601_FORMAT)
            assertEquals("-04:00", formattedDate.takeLast(6))

            val currentHour = hourFormat.format(DateTimeUtils.nowUTC())
            assertNotEquals(currentHour, SiteUtils.getCurrentDateTimeForSite(this, hourFormat))
        }

        val acstSite = SiteModel().apply { timezone = "9.5" }
        with(acstSite) {
            val formattedDate = SiteUtils.getCurrentDateTimeForSite(this, UTC8601_FORMAT)
            assertEquals("+09:30", formattedDate.takeLast(6))

            val currentHour = hourFormat.format(DateTimeUtils.nowUTC())
            assertNotEquals(currentHour, SiteUtils.getCurrentDateTimeForSite(this, hourFormat))
        }

        val nptSite = SiteModel().apply { timezone = "5.75" }
        with(nptSite) {
            val formattedDate = SiteUtils.getCurrentDateTimeForSite(this, UTC8601_FORMAT)
            assertEquals("+05:45", formattedDate.takeLast(6))

            val currentHour = hourFormat.format(DateTimeUtils.nowUTC())
            assertNotEquals(currentHour, SiteUtils.getCurrentDateTimeForSite(this, hourFormat))
        }

        val imaginaryQuarterTimeZoneSite = SiteModel().apply { timezone = "-2.25" }
        with(imaginaryQuarterTimeZoneSite) {
            val formattedDate = SiteUtils.getCurrentDateTimeForSite(this, UTC8601_FORMAT)
            assertEquals("-02:15", formattedDate.takeLast(6))

            val currentHour = hourFormat.format(DateTimeUtils.nowUTC())
            assertNotEquals(currentHour, SiteUtils.getCurrentDateTimeForSite(this, hourFormat))
        }
    }


    @Test
    fun testGetQuantityForDays() {
        val quantity1 = SiteUtils.getQuantityByGranularity("2018-01-25", "2018-01-28", StatsGranularity.DAYS, 30)
        assertEquals(4, quantity1)

        val quantity2 = SiteUtils.getQuantityByGranularity("2018-01-01", "2018-01-01", StatsGranularity.DAYS, 30)
        assertEquals(1, quantity2)

        val quantity3 = SiteUtils.getQuantityByGranularity("2018-01-01", "2018-01-31", StatsGranularity.DAYS, 30)
        assertEquals(31, quantity3)

        val defaultQuantity1 = SiteUtils.getQuantityByGranularity("", "", StatsGranularity.DAYS, 30)
        assertEquals(30, defaultQuantity1)

        val defaultQuantity2 = SiteUtils.getQuantityByGranularity(null, null, StatsGranularity.DAYS, 30)
        assertEquals(30, defaultQuantity2)
    }


    @Test
    fun testGetQuantityForWeeks() {
        val quantity1 = SiteUtils.getQuantityByGranularity("2018-10-22", "2018-10-23", StatsGranularity.WEEKS, 17)
        assertEquals(1, quantity1)

        val quantity2 = SiteUtils.getQuantityByGranularity("2017-01-01", "2018-01-01", StatsGranularity.WEEKS, 17)
        assertEquals(53, quantity2)

        val quantity3 = SiteUtils.getQuantityByGranularity("2019-01-20", "2019-01-13", StatsGranularity.WEEKS, 17)
        assertEquals(1, quantity3)

        val quantity4 = SiteUtils.getQuantityByGranularity("2017-01-01", "2018-03-01", StatsGranularity.WEEKS, 17)
        assertEquals(61, quantity4)

        val quantity5 = SiteUtils.getQuantityByGranularity("2018-01-01", "2018-01-31", StatsGranularity.WEEKS, 17)
        assertEquals(5, quantity5)

        val quantity6 = SiteUtils.getQuantityByGranularity("2018-12-01", "2018-12-31", StatsGranularity.WEEKS, 17)
        assertEquals(6, quantity6)

        val quantity7 = SiteUtils.getQuantityByGranularity("2018-11-01", "2018-11-30", StatsGranularity.WEEKS, 17)
        assertEquals(5, quantity7)

        val defaultQuantity1 = SiteUtils.getQuantityByGranularity("", "", StatsGranularity.WEEKS, 17)
        assertEquals(17, defaultQuantity1)

        val defaultQuantity2 = SiteUtils.getQuantityByGranularity(null, null, StatsGranularity.WEEKS, 17)
        assertEquals(17, defaultQuantity2)
    }


    @Test
    fun testGetQuantityForMonths() {
        val quantity1 = SiteUtils.getQuantityByGranularity("2018-10-22", "2018-10-23", StatsGranularity.MONTHS, 12)
        assertEquals(1, quantity1)

        val quantity2 = SiteUtils.getQuantityByGranularity("2017-01-01", "2018-01-01", StatsGranularity.MONTHS, 12)
        assertEquals(13, quantity2)

        val quantity3 = SiteUtils.getQuantityByGranularity("2018-01-01", "2018-01-01", StatsGranularity.MONTHS, 12)
        assertEquals(1, quantity3)

        val quantity4 = SiteUtils.getQuantityByGranularity("2017-01-01", "2018-03-01", StatsGranularity.MONTHS, 12)
        assertEquals(15, quantity4)

        val quantity5 = SiteUtils.getQuantityByGranularity("2017-01-01", "2018-01-31", StatsGranularity.MONTHS, 12)
        assertEquals(13, quantity5)

        val defaultQuantity1 = SiteUtils.getQuantityByGranularity("", "", StatsGranularity.MONTHS, 12)
        assertEquals(12, defaultQuantity1)

        val defaultQuantity2 = SiteUtils.getQuantityByGranularity(null, null, StatsGranularity.MONTHS, 12)
        assertEquals(12, defaultQuantity2)
    }


    @Test
    fun testGetQuantityForYears() {
        val quantity1 = SiteUtils.getQuantityByGranularity("2017-01-01", "2018-01-01", StatsGranularity.YEARS, 1)
        assertEquals(1, quantity1)

        val quantity2 = SiteUtils.getQuantityByGranularity("2017-01-01", "2018-01-01", StatsGranularity.YEARS, 1)
        assertEquals(1, quantity2)

        val quantity3 = SiteUtils.getQuantityByGranularity("2017-01-01", "2018-03-01", StatsGranularity.YEARS, 1)
        assertEquals(1, quantity3)

        val quantity4 = SiteUtils.getQuantityByGranularity("2017-01-01", "2018-01-05", StatsGranularity.YEARS, 1)
        assertEquals(1, quantity4)

        val quantity5 = SiteUtils.getQuantityByGranularity("2017-01-01", "2018-03-05", StatsGranularity.YEARS, 1)
        assertEquals(1, quantity5)

        val quantity6 = SiteUtils.getQuantityByGranularity("2017-01-01", "2019-03-01", StatsGranularity.YEARS, 1)
        assertEquals(2, quantity6)

        val quantity7 = SiteUtils.getQuantityByGranularity("2015-03-05", "2017-01-01", StatsGranularity.YEARS, 1)
        assertEquals(2, quantity7)

        val quantity8 = SiteUtils.getQuantityByGranularity("2015-03-05", "2017-12-31", StatsGranularity.YEARS, 1)
        assertEquals(2, quantity8)

        val defaultQuantity1 = SiteUtils.getQuantityByGranularity("", "", StatsGranularity.YEARS, 1)
        assertEquals(1, defaultQuantity1)

        val defaultQuantity2 = SiteUtils.getQuantityByGranularity(null, null, StatsGranularity.YEARS, 1)
        assertEquals(1, defaultQuantity2)
    }


    @Test
    fun testGetDateFormattedDateForUtcSite() {
        val siteModel = SiteModel()
        with(siteModel) {
            val formattedDate = SiteUtils.getDateTimeForSite(this, DATE_FORMAT_DAY, "")
            val currentTimeUtc = "2019-01-31"
            assertEquals(currentTimeUtc, formattedDate)
        }

        siteModel.timezone = ""
        with(siteModel) {
            val formattedDate = SiteUtils.getDateTimeForSite(this, DATE_FORMAT_DAY, "2019-01-31")
            val currentTimeUtc = "2019-01-30"
            assertEquals(currentTimeUtc, formattedDate)
        }


        siteModel.timezone = ""
        with(siteModel) {
            val formattedDate = SiteUtils.getDateTimeForSite(this, DATE_FORMAT_WEEK, "2019-01-31")
            val currentTimeUtc = "2019-W05"
            assertEquals(currentTimeUtc, formattedDate)
        }

        siteModel.timezone = "0"
        with(siteModel) {
            val formattedDate = SiteUtils.getDateTimeForSite(this, DATE_FORMAT_MONTH, "2019-01-31")
            val currentTimeUtc = "2018-12"
            assertEquals(currentTimeUtc, formattedDate)
        }


        siteModel.timezone = "0"
        with(siteModel) {
            val formattedDate = SiteUtils.getDateTimeForSite(this, DATE_FORMAT_YEAR, "2019-01-31")
            val currentTimeUtc = "2018"
            assertEquals(currentTimeUtc, formattedDate)
        }
    }
}
