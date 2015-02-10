package org.wordpress.mediapicker;

import android.graphics.Bitmap;
import android.net.Uri;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.android.volley.toolbox.ImageLoader;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class MediaUtilsTest {
    private static long FETCH_TIME = 2500;

    private boolean tImageAnimationStarted;
    private boolean tImageCached;

    @Before
    public void setUp() {
        tImageAnimationStarted = false;
        tImageCached = false;
    }

    /**
     * Verifies that a background image fetch fades the fetched image with the ImageView.
     */
    @Test
    public void testBackgroundImageFetch() {
        final Uri testUri = Uri.parse("file://test.jpg");
        final ImageView mockImage = mock(ImageView.class);
        final MediaUtils.BackgroundFetchThumbnail testFetch =
                new MediaUtils.BackgroundFetchThumbnail(mockImage, null, MediaUtils.BackgroundFetchThumbnail.TYPE_IMAGE, 0, 0, 0);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                tImageAnimationStarted = true;

                return null;
            }
        }).when(mockImage).startAnimation(any(Animation.class));

        when(mockImage.getTag()).thenReturn(testFetch);
        testFetch.execute(testUri);

        long startTime = System.currentTimeMillis();
        while (!tImageAnimationStarted && System.currentTimeMillis() - startTime < FETCH_TIME);

        Assert.assertTrue(tImageAnimationStarted);
    }

    /**
     * Verifies that fetched images are cached.
     */
    @Test
    public void testImageCached() {
        final Uri testUri = Uri.parse("file://test.jpg");
        final ImageView mockImage = mock(ImageView.class);
        final ImageLoader.ImageCache testCache = new TestImageCache();
        final MediaUtils.BackgroundFetchThumbnail testFetch =
                new MediaUtils.BackgroundFetchThumbnail(mockImage, testCache, MediaUtils.BackgroundFetchThumbnail.TYPE_IMAGE, 0, 0, 0);

        when(mockImage.getTag()).thenReturn(testFetch);
        testFetch.execute(testUri);

        long startTime = System.currentTimeMillis();
        while (!tImageAnimationStarted && System.currentTimeMillis() - startTime < FETCH_TIME);

        Assert.assertTrue(tImageCached);
    }

    /**
     * Verifies that {@link org.wordpress.mediapicker.MediaUtils#fadeInImage(android.widget.ImageView, android.graphics.Bitmap)}
     * starts an animation on the ImageView.
     */
    @Test
    public void testFadeInImage() {
        final ImageView mockImage = mock(ImageView.class);
        final Bitmap mockBitmap = mock(Bitmap.class);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                tImageAnimationStarted = true;

                return null;
            }
        }).when(mockImage).startAnimation(any(Animation.class));

        MediaUtils.fadeInImage(mockImage, mockBitmap);

        Assert.assertTrue(tImageAnimationStarted);
    }

    /**
     * Dummy class for testing image caching.
     */
    private class TestImageCache implements ImageLoader.ImageCache {
        @Override
        public Bitmap getBitmap(String url) {
            return null;
        }

        @Override
        public void putBitmap(String url, Bitmap bitmap) {
            tImageCached = true;
        }
    }
}
