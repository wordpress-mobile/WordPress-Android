package org.wordpress.android.ui.rs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class RsDateFormatterTest {
    @Test
    fun `scheduled date includes the time of day`() {
        withFixedLocaleAndZone {
            val date = Date(FIXED_MILLIS)

            val formatted = RsDateFormatter.format(date, NOW_LABEL, isScheduled = true)
            val dateOnly = DateFormat.getDateInstance(DateFormat.MEDIUM).format(date)

            // Scheduled dates show the medium date plus the time of day (iOS parity).
            assertThat(formatted).startsWith(dateOnly)
            assertThat(formatted).isNotEqualTo(dateOnly)
        }
    }

    @Test
    fun `published date older than a week shows an absolute medium date`() {
        withFixedLocaleAndZone {
            // Older than the one-week relative window (and well under a year) — iOS shows an
            // absolute medium date here, where legacy Android still showed relative time.
            val date = Date(System.currentTimeMillis() - THIRTY_DAYS_MILLIS)

            val formatted = RsDateFormatter.format(date, NOW_LABEL)
            val expected = DateFormat.getDateInstance(DateFormat.MEDIUM).format(date)

            assertThat(formatted).isEqualTo(expected)
        }
    }

    // The two now-label cases return the label verbatim without reaching DateFormat, so unlike
    // the others they don't need withFixedLocaleAndZone.
    @Test
    fun `date seconds in the past shows the now label`() {
        // Without this the relative formatter renders a just-published post as "0 minutes ago".
        val date = Date(System.currentTimeMillis() - THIRTY_SECONDS_MILLIS)

        assertThat(RsDateFormatter.format(date, NOW_LABEL)).isEqualTo(NOW_LABEL)
    }

    @Test
    fun `date seconds in the future shows the now label`() {
        // The server's date can sit just ahead of the device clock; that shouldn't read
        // "In 0 minutes".
        val date = Date(System.currentTimeMillis() + THIRTY_SECONDS_MILLIS)

        assertThat(RsDateFormatter.format(date, NOW_LABEL)).isEqualTo(NOW_LABEL)
    }

    @Test
    fun `scheduled date seconds away still shows its date and time`() {
        withFixedLocaleAndZone {
            // A page about to publish should say when, not "Now".
            val date = Date(System.currentTimeMillis() + THIRTY_SECONDS_MILLIS)

            val formatted = RsDateFormatter.format(date, NOW_LABEL, isScheduled = true)

            assertThat(formatted).isNotEqualTo(NOW_LABEL)
            assertThat(formatted).startsWith(DateFormat.getDateInstance(DateFormat.MEDIUM).format(date))
        }
    }

    private fun withFixedLocaleAndZone(block: () -> Unit) {
        val previousLocale = Locale.getDefault()
        val previousZone = TimeZone.getDefault()
        try {
            Locale.setDefault(Locale.US)
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            block()
        } finally {
            Locale.setDefault(previousLocale)
            TimeZone.setDefault(previousZone)
        }
    }

    companion object {
        // A fixed instant so the formatted output is deterministic under the pinned locale/zone.
        private const val FIXED_MILLIS = 1_765_792_800_000L
        private const val THIRTY_DAYS_MILLIS = 30L * 24 * 60 * 60 * 1000
        private const val THIRTY_SECONDS_MILLIS = 30L * 1000

        // Stands in for R.string.rs_date_now, which the ViewModels resolve before mapping.
        private const val NOW_LABEL = "Now"
    }
}
