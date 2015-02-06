package org.wordpress.mediapicker;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class MediaItemTest {
    @Test
    public void testSourceParsing() {
        final MediaItem testItem = new MediaItem();
        final String testValidSource = "file://test.img";
        final String testInvalidSource = "invalid-source";

        testItem.setPreviewSource(testValidSource);
        testItem.setSource(testValidSource);
        Assert.assertTrue("Failed to parse preview source.", testItem.getPreviewSource().isAbsolute());
        Assert.assertTrue("Failed to parse source.", testItem.getSource().isAbsolute());

        testItem.setPreviewSource(testInvalidSource);
        testItem.setSource(testInvalidSource);
        Assert.assertFalse(testItem.getPreviewSource().isAbsolute());
        Assert.assertFalse(testItem.getSource().isAbsolute());
    }

    @Test
    public void testDescribeContents() {
        final MediaItem testItem = new MediaItem();
        final int expectedContents = 0;

        Assert.assertEquals(expectedContents, testItem.describeContents());
    }
}
