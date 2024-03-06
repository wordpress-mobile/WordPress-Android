package org.wordpress.android.ui.notifications

import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableRange
import org.wordpress.android.ui.notifications.blocks.NoteBlockClickableSpan
import org.wordpress.android.ui.notifications.utils.NotificationsUtils

@HiltAndroidTest
class NotificationsUtilsTest {
    @Test
    fun testSpannableHasCharacterAtIndex() {
        val spannableStringBuilder = SpannableStringBuilder("This is only a test.")

        assertTrue(NotificationsUtils.spannableHasCharacterAtIndex(spannableStringBuilder, 's', 3))
        assertFalse(NotificationsUtils.spannableHasCharacterAtIndex(spannableStringBuilder, 's', 4))

        // Test with bogus params
        assertFalse(NotificationsUtils.spannableHasCharacterAtIndex(null, 'b', -1))
    }

    @Test
    fun testGetSpannableContentForRangesAndSkipInvalidUrls() {
        // Create a FormattableContent object
        val range1 = FormattableRange(indices = listOf(10, 14), url = "https://example.com", type = "a")
        val range2 = FormattableRange(indices = listOf(5, 20), url = "", type = "a") // invalid url to skip
        val formattableContent = FormattableContent(
            text = "This is a test content with a link",
            ranges = listOf(range1, range2)
        )

        // Create a TextView object
        val textView = TextView(InstrumentationRegistry.getInstrumentation().context)

        // Call the method with the created objects
        val result = NotificationsUtils.getSpannableContentForRanges(formattableContent, textView, false) {}

        // Check the result
        assertNotNull(result)
        assertEquals("This is a test content with a link", result.toString())

        // Check if the link is correctly set
        val spans = result.getSpans(10, 14, ClickableSpan::class.java)
        assertTrue(spans.size == 1)
        assertEquals("https://example.com", (spans[0] as NoteBlockClickableSpan).formattableRange.url)
    }

    @Test
    fun testGetSpannableContentForRangesWithNoRanges() {
        // Create a FormattableContent object with no ranges
        val formattableContent = FormattableContent(text = "This is a test content with no link")

        // Create a TextView object
        val textView = TextView(InstrumentationRegistry.getInstrumentation().context)

        // Call the method with the created objects
        val result = NotificationsUtils.getSpannableContentForRanges(formattableContent, textView, false) {}

        // Check the result
        assertNotNull(result)
        assertEquals("This is a test content with no link", result.toString())

        // Check if no ClickableSpan is set
        val spans = result.getSpans(0, result.length, ClickableSpan::class.java)
        assertTrue(spans.isEmpty())
    }

    @Test
    fun testGetSpannableContentForRangesWithInvalidIndex() {
        // Create a FormattableContent object with a range with an invalid index
        val range = FormattableRange(indices = listOf(50, 54), url = "https://example.com", type = "a")
        val formattableContent = FormattableContent(text = "This is a test content", ranges = listOf(range))

        // Create a TextView object
        val textView = TextView(InstrumentationRegistry.getInstrumentation().context)

        // Call the method with the created objects
        val result = NotificationsUtils.getSpannableContentForRanges(formattableContent, textView, false) {}

        // Check the result
        assertNotNull(result)
        assertEquals("This is a test content", result.toString())

        // Check if no ClickableSpan is set
        val spans = result.getSpans(0, result.length, ClickableSpan::class.java)
        assertTrue(spans.isEmpty())
    }

    @Test
    fun testGetSpannableContentForRangesWithNullUrl() {
        // Create a FormattableContent object with a range with a null URL
        val range = FormattableRange(indices = listOf(10, 14), url = null, type = "a")
        val formattableContent = FormattableContent(text = "This is a test content with a link", ranges = listOf(range))

        // Create a TextView object
        val textView = TextView(InstrumentationRegistry.getInstrumentation().context)

        // Call the method with the created objects
        val result = NotificationsUtils.getSpannableContentForRanges(formattableContent, textView, false) {}

        // Check the result
        assertNotNull(result)
        assertEquals("This is a test content with a link", result.toString())

        // Check if no ClickableSpan is set for the range with the null URL
        val spans = result.getSpans(10, 14, ClickableSpan::class.java)
        assertTrue(spans.isEmpty())
    }
}
