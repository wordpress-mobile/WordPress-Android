package org.wordpress.android.ui.notifications;


import android.test.AndroidTestCase;

import org.wordpress.android.GCMMessageService;

public class GCMIntentServiceTest extends AndroidTestCase {

    public void testShouldCircularizeNoteIcon() {
        GCMMessageService intentService = new GCMMessageService();

        String type = "c";
        assertTrue(intentService.shouldCircularizeNoteIcon(type));

        assertFalse(intentService.shouldCircularizeNoteIcon(null));

        type = "invalidType";
        assertFalse(intentService.shouldCircularizeNoteIcon(type));
    }
}
