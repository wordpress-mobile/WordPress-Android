package org.wordpress.android.ui.notifications

import android.text.SpannableStringBuilder
import dagger.hilt.android.testing.HiltAndroidTest
import junit.framework.TestCase
import org.junit.Test
import org.wordpress.android.ui.notifications.utils.NotificationsUtils

@HiltAndroidTest
class NotificationsUtilsTest {
    @Test
    fun testSpannableHasCharacterAtIndex() {
        val spannableStringBuilder = SpannableStringBuilder("This is only a test.")

        TestCase.assertTrue(NotificationsUtils.spannableHasCharacterAtIndex(spannableStringBuilder, 's', 3))
        TestCase.assertFalse(NotificationsUtils.spannableHasCharacterAtIndex(spannableStringBuilder, 's', 4))

        // Test with bogus params
        TestCase.assertFalse(NotificationsUtils.spannableHasCharacterAtIndex(null, 'b', -1))
    }
}
