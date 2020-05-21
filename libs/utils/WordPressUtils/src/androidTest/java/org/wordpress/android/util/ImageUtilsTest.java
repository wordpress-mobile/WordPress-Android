package org.wordpress.android.util;

import android.graphics.BitmapFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImageUtilsTest {
    @Test
    public void testGetScaleForResizingReturnsOneWhenMaxSizeIsZero() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        int scale = ImageUtils.getScaleForResizing(0, options);

        assertEquals(1, scale);
    }

    @Test
    public void testGetScaleForResizingSameSizeReturnsOne() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outHeight = 100;
        options.outWidth = 100;
        int maxSize = 100;

        int scale = ImageUtils.getScaleForResizing(maxSize, options);

        assertEquals(1, scale);
    }

    @Test
    public void testGetScaleForResizingPortraitMaxHeightSameAsMaxSizeReturnsOne() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outHeight = 100;
        options.outWidth = 1;
        int maxSize = 100;

        int scale = ImageUtils.getScaleForResizing(maxSize, options);

        assertEquals(1, scale);
    }

    @Test
    public void testGetScaleForResizingLandscapeMaxWidthSameAsMaxSizeReturnsOne() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outHeight = 1;
        options.outWidth = 100;
        int maxSize = 100;

        int scale = ImageUtils.getScaleForResizing(maxSize, options);

        assertEquals(1, scale);
    }

    @Test
    public void testGetScaleForResizingDoubleSizeReturnsTwo() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outHeight = 100;
        options.outWidth = 200;
        int maxSize = 100;

        int scale = ImageUtils.getScaleForResizing(maxSize, options);

        assertEquals(2, scale);
    }

    @Test
    public void testGetScaleForResizingThreeTimesSizeReturnsTwo() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outHeight = 100;
        options.outWidth = 300;
        int maxSize = 100;

        int scale = ImageUtils.getScaleForResizing(maxSize, options);

        assertEquals(2, scale);
    }

    @Test
    public void testGetScaleForResizingEightTimesSizeReturnsEight() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outHeight = 100;
        options.outWidth = 800;
        int maxSize = 100;

        int scale = ImageUtils.getScaleForResizing(maxSize, options);

        assertEquals(8, scale);
    }
}
