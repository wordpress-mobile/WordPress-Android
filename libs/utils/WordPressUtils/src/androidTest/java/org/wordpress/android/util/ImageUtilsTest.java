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

    public void testGetScaleForResizingPortraitMaxHeightSameAsMaxSizeReturnsOne() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outHeight = 100;
        options.outWidth = 1;
        int maxSize = 100;

        int scale = ImageUtils.getScaleForResizing(maxSize, options);

        assertEquals(1, scale);
    }

    public void testGetScaleForResizingLandscapeMaxWidthSameAsMaxSizeReturnsOne() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outHeight = 1;
        options.outWidth = 100;
        int maxSize = 100;

        int scale = ImageUtils.getScaleForResizing(maxSize, options);

        assertEquals(1, scale);
    }

    public void testGetScaleForResizingDoubleSizeReturnsTwo() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outHeight = 100;
        options.outWidth = 200;
        int maxSize = 100;

        int scale = ImageUtils.getScaleForResizing(maxSize, options);

        assertEquals(2, scale);
    }

    public void testGetScaleForResizingThreeTimesSizeReturnsTwo() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outHeight = 100;
        options.outWidth = 300;
        int maxSize = 100;

        int scale = ImageUtils.getScaleForResizing(maxSize, options);

        assertEquals(2, scale);
    }

    public void testGetScaleForResizingEightTimesSizeReturnsEight() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outHeight = 100;
        options.outWidth = 800;
        int maxSize = 100;

        int scale = ImageUtils.getScaleForResizing(maxSize, options);

        assertEquals(8, scale);
    }
}
