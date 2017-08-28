package org.wordpress.android.util;

import android.graphics.BitmapFactory;
import android.test.InstrumentationTestCase;

public class ImageUtilsTest extends InstrumentationTestCase {
    public void testGetScaleForResizingReturnsOneWhenMaxSizeIsZero() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        int scale = ImageUtils.getScaleForResizing(0, options);

        assertEquals(1, scale);
    }

    public void testGetScaleForResizingSameSizeReturnsOne() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outHeight = 100;
        options.outWidth = 100;
        int maxSize = 100;

        int scale = ImageUtils.getScaleForResizing(maxSize, options);

        assertEquals(1, scale);
    }
}
