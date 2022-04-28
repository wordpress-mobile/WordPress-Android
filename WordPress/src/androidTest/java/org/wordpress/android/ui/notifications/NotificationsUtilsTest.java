package org.wordpress.android.ui.notifications;

import android.text.SpannableStringBuilder;

import org.junit.Test;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class NotificationsUtilsTest {
    @Test
    public void testSpannableHasCharacterAtIndex() {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder("This is only a test.");

        assertTrue(NotificationsUtils.spannableHasCharacterAtIndex(spannableStringBuilder, 's', 3));
        assertFalse(NotificationsUtils.spannableHasCharacterAtIndex(spannableStringBuilder, 's', 4));

        // Test with bogus params
        assertFalse(NotificationsUtils.spannableHasCharacterAtIndex(null, 'b', -1));
    }
}
