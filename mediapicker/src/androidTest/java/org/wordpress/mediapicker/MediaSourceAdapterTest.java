package org.wordpress.mediapicker;

import android.content.ContentResolver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.toolbox.ImageLoader;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.wordpress.mediapicker.source.MediaSource;
import org.wordpress.mediapicker.source.MediaSourceDeviceImages;
import org.wordpress.mediapicker.source.MediaSourceDeviceVideos;

import java.util.ArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class MediaSourceAdapterTest {
    /**
     * Verifies that a view type exists even if there is no media source, for an empty view.
     */
    @Test
    public void testViewTypeCountWithNoSources() {
        final ArrayList<MediaSource> testSources = new ArrayList<>();
        final MediaSourceAdapter testAdapter = new MediaSourceAdapter(Robolectric.application, testSources, null, null);

        Assert.assertEquals(1, testAdapter.getViewTypeCount());
    }

    /**
     * Verifies that a single source has a single view type.
     */
    @Test
    public void testViewTypeCountWithOneSource() {
        final ArrayList<MediaSource> testSources = new ArrayList<>();
        testSources.add(new MediaSourceDeviceImages(mock(ContentResolver.class)));

        final MediaSourceAdapter testAdapter = new MediaSourceAdapter(Robolectric.application, testSources, null, null);

        Assert.assertEquals(testSources.size(), testAdapter.getViewTypeCount());
    }

    /**
     * Verifies there are as many view types as media sources.
     */
    @Test
    public void testViewTypeCountWithManySources() {
        final ArrayList<MediaSource> testSources = new ArrayList<>();
        testSources.add(new MediaSourceDeviceImages(mock(ContentResolver.class)));
        testSources.add(new MediaSourceDeviceVideos(mock(ContentResolver.class)));

        final MediaSourceAdapter testAdapter = new MediaSourceAdapter(Robolectric.application, testSources, null, null);

        Assert.assertEquals(testSources.size(), testAdapter.getViewTypeCount());
    }

    /**
     * Verifies an invalid view type is returned if no MediaSources are present.
     */
    @Test
    public void testViewTypeWithNoSources() {
        final ArrayList<MediaSource> testSources = new ArrayList<>();
        final MediaSourceAdapter testAdapter = new MediaSourceAdapter(Robolectric.application, testSources, null, null);

        Assert.assertEquals(-1, testAdapter.getItemViewType(0));
    }

    /**
     * Verifies that the view type is unique for each source.
     */
    @Test
    public void testViewTypeWithManySources() {
        final ArrayList<MediaSource> testSources = new ArrayList<>();
        final MediaSourceDeviceImages mockSource1 = mock(MediaSourceDeviceImages.class);
        final MediaSourceDeviceImages mockSource2 = mock(MediaSourceDeviceImages.class);
        final int testItemCount = 10;

        when(mockSource1.getCount()).thenReturn(testItemCount);
        when(mockSource2.getCount()).thenReturn(testItemCount);
        testSources.add(mockSource1);
        testSources.add(mockSource2);

        final MediaSourceAdapter testAdapter = new MediaSourceAdapter(Robolectric.application, testSources, null, null);

        Assert.assertEquals(1, testAdapter.getItemViewType(testItemCount + 1));
    }

    /**
     * Verifies that the count can succeed with a single source.
     */
    @Test
    public void testCountWithOneSource() {
        final ArrayList<MediaSource> testSources = new ArrayList<>();
        final MediaSourceDeviceImages mockSource = mock(MediaSourceDeviceImages.class);
        final int testItemCount = 10;

        when(mockSource.getCount()).thenReturn(testItemCount);
        testSources.add(mockSource);

        final MediaSourceAdapter testAdapter = new MediaSourceAdapter(Robolectric.application, testSources, null, null);

        Assert.assertEquals(testItemCount, testAdapter.getCount());
    }

    /**
     * Verifies that all items from all sources are included in the count.
     */
    @Test
    public void testCountWithManySources() {
        final ArrayList<MediaSource> testSources = new ArrayList<>();
        final MediaSourceDeviceImages mockSource1 = mock(MediaSourceDeviceImages.class);
        final MediaSourceDeviceImages mockSource2 = mock(MediaSourceDeviceImages.class);
        final int testItemCount = 10;

        when(mockSource1.getCount()).thenReturn(testItemCount);
        when(mockSource2.getCount()).thenReturn(testItemCount);
        testSources.add(mockSource1);
        testSources.add(mockSource2);

        final MediaSourceAdapter testAdapter = new MediaSourceAdapter(Robolectric.application, testSources, null, null);

        Assert.assertEquals(testItemCount * testSources.size(), testAdapter.getCount());
    }

    /**
     * Verifies that the appropriate {@link org.wordpress.mediapicker.MediaItem} is retrieved given
     * a multiple number of sources.
     */
    @Test
    public void testGetItem() {
        final ArrayList<MediaSource> testSources = new ArrayList<>();
        final MediaSourceDeviceImages mockSource1 = mock(MediaSourceDeviceImages.class);
        final MediaSourceDeviceImages mockSource2 = mock(MediaSourceDeviceImages.class);
        final int testItemCount = 10;
        final MediaItem testItem = new MediaItem();

        when(mockSource1.getCount()).thenReturn(testItemCount);
        when(mockSource2.getCount()).thenReturn(testItemCount);
        when(mockSource2.getMedia(1)).thenReturn(testItem);
        testSources.add(mockSource1);
        testSources.add(mockSource2);

        final MediaSourceAdapter testAdapter = new MediaSourceAdapter(Robolectric.application, testSources, null, null);
        final MediaItem mediaItem = testAdapter.getItem(testItemCount + 1);

        Assert.assertEquals(testItem, mediaItem);
    }

    /**
     * Verifies that the correct {@link org.wordpress.mediapicker.source.MediaSource} is used to
     * create a {@link android.view.View}.
     *
     * TODO: seems to me that anyInt() is inappropriate, it should be specifically the first element of mockSource2.
     */
    @Test
    public void testGetView() {
        final ArrayList<MediaSource> testSources = new ArrayList<>();
        final MediaSourceDeviceImages mockSource1 = mock(MediaSourceDeviceImages.class);
        final MediaSourceDeviceImages mockSource2 = mock(MediaSourceDeviceImages.class);
        final int testItemCount = 10;
        final MediaItem testItem = new MediaItem();
        final View testView = mock(View.class);

        when(mockSource2
                .getView(anyInt(), any(View.class), any(ViewGroup.class), any(LayoutInflater.class), any(ImageLoader.ImageCache.class)))
                .thenReturn(testView);
        when(mockSource1.getCount()).thenReturn(testItemCount);
        when(mockSource2.getCount()).thenReturn(testItemCount);
        when(mockSource2.getMedia(1)).thenReturn(testItem);
        testSources.add(mockSource1);
        testSources.add(mockSource2);

        final MediaSourceAdapter testAdapter = new MediaSourceAdapter(Robolectric.application, testSources, null, null);

        Assert.assertEquals(testView, testAdapter.getView(testItemCount, null, null));
    }

    /**
     * Verifies that each {@link org.wordpress.mediapicker.MediaItem} has a unique ID.s
     */
    @Test
    public void testItemId() {
        final ArrayList<MediaSource> testSources = new ArrayList<>();
        final MediaSourceDeviceImages mockSource1 = mock(MediaSourceDeviceImages.class);
        final MediaSourceDeviceImages mockSource2 = mock(MediaSourceDeviceImages.class);
        final int testItemCount = 10;

        when(mockSource1.getCount()).thenReturn(testItemCount);
        when(mockSource2.getCount()).thenReturn(testItemCount);
        testSources.add(mockSource1);
        testSources.add(mockSource2);

        final MediaSourceAdapter testAdapter = new MediaSourceAdapter(Robolectric.application, testSources, null, null);

        for (int i = 0; i < testItemCount * testSources.size(); ++i) {
            Assert.assertEquals(i, testAdapter.getItemId(i));
        }
    }
}
