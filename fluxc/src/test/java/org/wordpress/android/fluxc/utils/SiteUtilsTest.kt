package org.wordpress.android.fluxc.utils

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
    }

    @Test
    fun testGetCurrentDateTimeUtcSite() {
        val siteModel = SiteModel()
        with (siteModel) {
            val formattedDate = SiteUtils.getCurrentDateTimeForSite(this, UTC8601_FORMAT)
            val currentTimeUtc = DateTimeUtils.iso8601UTCFromDate(Date())
            assertEquals(currentTimeUtc, formattedDate.replace("Z", "+00:00"))
        }

        siteModel.timezone = ""
        with (siteModel) {
            val formattedDate = SiteUtils.getCurrentDateTimeForSite(this, UTC8601_FORMAT)
            val currentTimeUtc = DateTimeUtils.iso8601UTCFromDate(Date())
            assertEquals(currentTimeUtc, formattedDate.replace("Z", "+00:00"))
        }

        siteModel.timezone = "0"
        with (siteModel) {
            val formattedDate = SiteUtils.getCurrentDateTimeForSite(this, UTC8601_FORMAT)
            val currentTimeUtc = DateTimeUtils.iso8601UTCFromDate(Date())
            assertEquals(currentTimeUtc, formattedDate.replace("Z", "+00:00"))
        }
    }

    @Test
    fun testGetCurrentDateTimeForNonUtcSite() {
        val hourFormat = SimpleDateFormat("HH", Locale.ROOT)

        val estSite = SiteModel().apply { timezone = "-4" }
        with (estSite) {
            val formattedDate = SiteUtils.getCurrentDateTimeForSite(this, UTC8601_FORMAT)
            assertEquals("-04:00", formattedDate.takeLast(6))

            val currentHour = hourFormat.format(DateTimeUtils.nowUTC())
            assertNotEquals(currentHour, SiteUtils.getCurrentDateTimeForSite(this, hourFormat))
        }

        val acstSite = SiteModel().apply { timezone = "9.5" }
        with (acstSite) {
            val formattedDate = SiteUtils.getCurrentDateTimeForSite(this, UTC8601_FORMAT)
            assertEquals("+09:30", formattedDate.takeLast(6))

            val currentHour = hourFormat.format(DateTimeUtils.nowUTC())
            assertNotEquals(currentHour, SiteUtils.getCurrentDateTimeForSite(this, hourFormat))
        }
    }
}
