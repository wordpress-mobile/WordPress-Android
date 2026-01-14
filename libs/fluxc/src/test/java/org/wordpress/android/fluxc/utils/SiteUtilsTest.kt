package org.wordpress.android.fluxc.utils

import org.assertj.core.api.Assertions
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SiteUtilsTest {
    companion object {
        const val UTC8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX"
        private const val DATE_FORMAT_DAY = "yyyy-MM-dd"
        private const val DATE_FORMAT_WEEK = "yyyy-'W'ww"
        private const val DATE_FORMAT_MONTH = "yyyy-MM"
        private const val DATE_FORMAT_YEAR = "yyyy"
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
    fun testGetFormattedDateForUtcSite() {
        val siteModel = SiteModel()
        with(siteModel) {
            val formattedDate = DateUtils.getDateTimeForSite(this, UTC8601_FORMAT, "")
            val currentTimeUtc = DateTimeUtils.iso8601UTCFromDate(Date())
            assertEquals(currentTimeUtc, formattedDate.replace("Z", "+00:00"))
        }

        siteModel.timezone = ""
        with(siteModel) {
            val formattedDate = DateUtils.getDateTimeForSite(this, UTC8601_FORMAT, null)
            val currentTimeUtc = DateTimeUtils.iso8601UTCFromDate(Date())
            assertEquals(currentTimeUtc, formattedDate.replace("Z", "+00:00"))
        }

        siteModel.timezone = "0"
        val dateString = "2019-01-31"
        val date = SimpleDateFormat(DATE_FORMAT_DAY, Locale.ROOT).parse(dateString)

        with(siteModel) {
            val formattedDate = DateUtils.getDateTimeForSite(this, DATE_FORMAT_DAY, dateString)
            val currentTimeUtc = DateUtils.formatDate(DATE_FORMAT_DAY, date)
            assertEquals(currentTimeUtc, formattedDate)
        }

        siteModel.timezone = ""
        with(siteModel) {
            val formattedDate = DateUtils.getDateTimeForSite(this, DATE_FORMAT_WEEK, dateString)
            val currentTimeUtc = DateUtils.formatDate(DATE_FORMAT_WEEK, date)
            assertEquals(currentTimeUtc, formattedDate)
        }

        siteModel.timezone = "0"
        with(siteModel) {
            val formattedDate = DateUtils.getDateTimeForSite(this, DATE_FORMAT_MONTH, dateString)
            val currentTimeUtc = DateUtils.formatDate(DATE_FORMAT_MONTH, date)
            assertEquals(currentTimeUtc, formattedDate)
        }

        siteModel.timezone = ""
        with(siteModel) {
            val formattedDate = DateUtils.getDateTimeForSite(this, DATE_FORMAT_YEAR, dateString)
            val currentTimeUtc = DateUtils.formatDate(DATE_FORMAT_YEAR, date)
            assertEquals(currentTimeUtc, formattedDate)
        }
    }

    @Test
    fun testGetFormattedDateForNonUtcSite() {
        val hourFormat = SimpleDateFormat("HH", Locale.ROOT)
        val dateString = "2019-01-31"
        val date = DateUtils.getDateFromString(dateString)

        val estSite = SiteModel().apply { timezone = "-4" }
        with(estSite) {
            val formattedDate = DateUtils.getDateTimeForSite(this, UTC8601_FORMAT, dateString)
            assertEquals("-04:00", formattedDate.takeLast(6))

            val currentHour = hourFormat.format(DateTimeUtils.localDateToUTC(date))
            assertNotEquals(currentHour, SiteUtils.getDateTimeForSite(this, hourFormat, date))
        }

        val acstSite = SiteModel().apply { timezone = "9.5" }
        with(acstSite) {
            val formattedDate = DateUtils.getDateTimeForSite(this, UTC8601_FORMAT, dateString)
            assertEquals("+09:30", formattedDate.takeLast(6))

            val currentHour = hourFormat.format(DateTimeUtils.localDateToUTC(date))
            assertNotEquals(currentHour, SiteUtils.getDateTimeForSite(this, hourFormat, date))
        }

        val nptSite = SiteModel().apply { timezone = "5.75" }
        with(nptSite) {
            val formattedDate = DateUtils.getDateTimeForSite(this, UTC8601_FORMAT, dateString)
            assertEquals("+05:45", formattedDate.takeLast(6))

            val currentHour = hourFormat.format(DateTimeUtils.localDateToUTC(date))
            assertNotEquals(currentHour, SiteUtils.getDateTimeForSite(this, hourFormat, date))
        }

        val imaginaryQuarterTimeZoneSite = SiteModel().apply { timezone = "-2.25" }
        with(imaginaryQuarterTimeZoneSite) {
            val formattedDate = DateUtils.getDateTimeForSite(this, UTC8601_FORMAT, dateString)
            assertEquals("-02:15", formattedDate.takeLast(6))

            val currentHour = hourFormat.format(DateTimeUtils.localDateToUTC(date))
            assertNotEquals(currentHour, SiteUtils.getDateTimeForSite(this, hourFormat, date))
        }
    }

    @Test
    fun `returns correct timezone for positive offset with plus sign`() {
        val timeZone = SiteUtils.getNormalizedTimezone("+10")

        Assertions.assertThat(timeZone.displayName).isEqualTo("GMT+10:00")
    }

    @Test
    fun `returns correct timezone for positive numeric offset`() {
        val timeZone = SiteUtils.getNormalizedTimezone("5")

        Assertions.assertThat(timeZone.id).isEqualTo("GMT+05:00")
    }

    @Test
    fun `returns correct timezone for negative numeric offset`() {
        val timeZone = SiteUtils.getNormalizedTimezone("-8")

        Assertions.assertThat(timeZone.id).isEqualTo("GMT-08:00")
    }

    @Test
    fun `returns correct timezone for fractional offset`() {
        val timeZone = SiteUtils.getNormalizedTimezone("5.5")

        Assertions.assertThat(timeZone.id).isEqualTo("GMT+05:30")
    }

    @Test
    fun `returns GMT for null timezone`() {
        val timeZone = SiteUtils.getNormalizedTimezone(null)

        Assertions.assertThat(timeZone.id).isEqualTo("GMT")
    }

    @Test
    fun `returns GMT for empty timezone`() {
        val timeZone = SiteUtils.getNormalizedTimezone("")

        Assertions.assertThat(timeZone.id).isEqualTo("GMT")
    }

    @Test
    fun `returns GMT for zero timezone`() {
        val timeZone = SiteUtils.getNormalizedTimezone("0")

        Assertions.assertThat(timeZone.id).isEqualTo("GMT")
    }

    @Test
    fun `returns named timezone for Europe Madrid`() {
        val timeZone = SiteUtils.getNormalizedTimezone("Europe/Madrid")

        Assertions.assertThat(timeZone.id).isEqualTo("Europe/Madrid")
    }

    @Test
    fun `returns named timezone for America New_York`() {
        val timeZone = SiteUtils.getNormalizedTimezone("America/New_York")

        Assertions.assertThat(timeZone.id).isEqualTo("America/New_York")
    }

    @Test
    fun `returns named timezone for Asia Tokyo`() {
        val timeZone = SiteUtils.getNormalizedTimezone("Asia/Tokyo")

        Assertions.assertThat(timeZone.id).isEqualTo("Asia/Tokyo")
    }

    @Test
    fun `returns named timezone for Australia Sydney`() {
        val timeZone = SiteUtils.getNormalizedTimezone("Australia/Sydney")

        Assertions.assertThat(timeZone.id).isEqualTo("Australia/Sydney")
    }

    @Test
    fun `falls back to numeric parsing for invalid named timezone`() {
        // An invalid named timezone that contains "/" should fall back to numeric parsing
        // which will also fail, resulting in GMT
        val timeZone = SiteUtils.getNormalizedTimezone("Invalid/Timezone")

        // Falls back to numeric parsing, which creates "GMT+Invalid/Timezone"
        // TimeZone.getTimeZone returns GMT for unrecognized IDs
        Assertions.assertThat(timeZone.id).isEqualTo("GMT")
    }

    @Test
    fun `verifies DST transition for Europe Madrid - winter time is UTC+1`() {
        val madridSite = SiteModel().apply { timezone = "Europe/Madrid" }

        // Winter time (CET, UTC+1): January 15, 2025
        val winterDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.parse("2025-01-15 12:00:00")

        val offsetFormat = SimpleDateFormat("XXX", Locale.ROOT)
        val winterFormatted = SiteUtils.getDateTimeForSite(madridSite, offsetFormat, winterDate)

        Assertions.assertThat(winterFormatted).isEqualTo("+01:00")
    }

    @Test
    fun `verifies DST transition for Europe Madrid - summer time is UTC+2`() {
        val madridSite = SiteModel().apply { timezone = "Europe/Madrid" }

        // Summer time (CEST, UTC+2): July 15, 2025
        val summerDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.parse("2025-07-15 12:00:00")

        val offsetFormat = SimpleDateFormat("XXX", Locale.ROOT)
        val summerFormatted = SiteUtils.getDateTimeForSite(madridSite, offsetFormat, summerDate)

        Assertions.assertThat(summerFormatted).isEqualTo("+02:00")
    }

    @Test
    fun `verifies DST transition for America New_York - winter time is UTC-5`() {
        val newYorkSite = SiteModel().apply { timezone = "America/New_York" }

        // Winter time (EST, UTC-5): January 15, 2025
        val winterDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.parse("2025-01-15 12:00:00")

        val offsetFormat = SimpleDateFormat("XXX", Locale.ROOT)
        val winterFormatted = SiteUtils.getDateTimeForSite(newYorkSite, offsetFormat, winterDate)

        Assertions.assertThat(winterFormatted).isEqualTo("-05:00")
    }

    @Test
    fun `verifies DST transition for America New_York - summer time is UTC-4`() {
        val newYorkSite = SiteModel().apply { timezone = "America/New_York" }

        // Summer time (EDT, UTC-4): July 15, 2025
        val summerDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.parse("2025-07-15 12:00:00")

        val offsetFormat = SimpleDateFormat("XXX", Locale.ROOT)
        val summerFormatted = SiteUtils.getDateTimeForSite(newYorkSite, offsetFormat, summerDate)

        Assertions.assertThat(summerFormatted).isEqualTo("-04:00")
    }

    @Test
    fun `numeric offset does not change between winter and summer - demonstrates the bug`() {
        // This test demonstrates why numeric offsets are problematic:
        // They don't adjust for DST, so the offset stays the same year-round
        val numericOffsetSite = SiteModel().apply { timezone = "1" } // UTC+1

        val winterDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.parse("2025-01-15 12:00:00")

        val summerDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.parse("2025-07-15 12:00:00")

        val offsetFormat = SimpleDateFormat("XXX", Locale.ROOT)
        val winterFormatted = SiteUtils.getDateTimeForSite(numericOffsetSite, offsetFormat, winterDate)
        val summerFormatted = SiteUtils.getDateTimeForSite(numericOffsetSite, offsetFormat, summerDate)

        // Both are +01:00 because numeric offset doesn't account for DST
        Assertions.assertThat(winterFormatted).isEqualTo("+01:00")
        Assertions.assertThat(summerFormatted).isEqualTo("+01:00")
    }
}
