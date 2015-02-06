package org.wordpress.mediapicker;

import android.test.AndroidTestCase;

public class MediaItemTest extends AndroidTestCase {
    public void testSourceParsing() {
        final MediaItem testItem = new MediaItem();
        final String testValidSource = "file://test.img";
        final String testInvalidSource = "invalid-source";

        testItem.setPreviewSource(testValidSource);
        testItem.setSource(testValidSource);
        assertTrue("Failed to parse preview source.", testItem.getPreviewSource().isAbsolute());
        assertTrue("Failed to parse source.", testItem.getSource().isAbsolute());

        testItem.setPreviewSource(testInvalidSource);
        testItem.setSource(testInvalidSource);
        assertFalse(testItem.getPreviewSource().isAbsolute());
        assertFalse(testItem.getSource().isAbsolute());
    }

    public void testDescribeContents() {
        final MediaItem testItem = new MediaItem();
        final int expectedContents = 0;

        assertEquals(expectedContents, testItem.describeContents());
    }
}
