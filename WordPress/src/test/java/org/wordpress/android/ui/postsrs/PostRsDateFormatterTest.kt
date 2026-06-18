package org.wordpress.android.ui.postsrs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uniffi.wp_api.PostStatus
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PostRsDateFormatterTest {
    @Test
    fun `scheduled date includes the time of day`() {
        withFixedLocaleAndZone {
            val date = Date(FIXED_MILLIS)

            val formatted = PostRsDateFormatter.format(date, PostStatus.Future)
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

            val formatted = PostRsDateFormatter.format(date, PostStatus.Publish)
            val expected = DateFormat.getDateInstance(DateFormat.MEDIUM).format(date)

            assertThat(formatted).isEqualTo(expected)
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
    }
}
