package org.wordpress.android.ui.notifications;


import android.test.AndroidTestCase;

import org.wordpress.android.GCMIntentService;

public class GCMIntentServiceTest extends AndroidTestCase {

    public void testShouldCircularizeNoteIcon() {
        GCMIntentService intentService = new GCMIntentService();

        String type = "c";
        assertTrue(intentService.shouldCircularizeNoteIcon(type));

        assertFalse(intentService.shouldCircularizeNoteIcon(null));

        type = "invalidType";
        assertFalse(intentService.shouldCircularizeNoteIcon(type));
    }
}
