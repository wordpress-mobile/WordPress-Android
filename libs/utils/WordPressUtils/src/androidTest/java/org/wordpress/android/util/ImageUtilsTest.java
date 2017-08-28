package org.wordpress.android.util;

import android.graphics.BitmapFactory;
import android.test.InstrumentationTestCase;

public class ImageUtilsTest extends InstrumentationTestCase {
    public void testGetScaleForResizingReturnsOneWhenMaxSizeIsZero() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        int scale = ImageUtils.getScaleForResizing(0, options);

        assertEquals(1, scale);
    }
}
