package org.wordpress.mediapicker;

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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class MediaSourceAdapterTest {
    @Test
    public void testViewTypeCountWithNoSources() {
        final ArrayList<MediaSource> testSources = new ArrayList<>();
        final MediaSourceAdapter testAdapter = new MediaSourceAdapter(Robolectric.application, testSources, null);

        Assert.assertEquals(1, testAdapter.getViewTypeCount());
    }

    @Test
    public void testViewTypeCountWithOneSource() {
        final ArrayList<MediaSource> testSources = new ArrayList<>();
        testSources.add(new MediaSourceDeviceImages());

        final MediaSourceAdapter testAdapter = new MediaSourceAdapter(Robolectric.application, testSources, null);

        Assert.assertEquals(testSources.size(), testAdapter.getViewTypeCount());
    }

    @Test
    public void testViewTypeCountWithManySources() {
        final ArrayList<MediaSource> testSources = new ArrayList<>();
        testSources.add(new MediaSourceDeviceImages());
        testSources.add(new MediaSourceDeviceVideos());

        final MediaSourceAdapter testAdapter = new MediaSourceAdapter(Robolectric.application, testSources, null);

        Assert.assertEquals(testSources.size(), testAdapter.getViewTypeCount());
    }

    @Test
    public void testViewTypeWithNoSources() {
        final ArrayList<MediaSource> testSources = new ArrayList<>();
        final MediaSourceAdapter testAdapter = new MediaSourceAdapter(Robolectric.application, testSources, null);

        Assert.assertEquals(-1, testAdapter.getItemViewType(0));
    }

    @Test
    public void testViewTypeWithOneSource() {
        final ArrayList<MediaSource> testSources = new ArrayList<>();
        final MediaSourceDeviceImages mockSource = mock(MediaSourceDeviceImages.class);
        final int testItemCount = 1;

        when(mockSource.getCount()).thenReturn(testItemCount);
        testSources.add(mockSource);

        final MediaSourceAdapter testAdapter = new MediaSourceAdapter(Robolectric.application, testSources, null);

        Assert.assertEquals(0, testAdapter.getItemViewType(0));
    }

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

        final MediaSourceAdapter testAdapter = new MediaSourceAdapter(Robolectric.application, testSources, null);

        Assert.assertEquals(1, testAdapter.getItemViewType(testItemCount + 1));
    }

    @Test
    public void testCountWithOneSource() {
        final ArrayList<MediaSource> testSources = new ArrayList<>();
        final MediaSourceDeviceImages mockSource = mock(MediaSourceDeviceImages.class);
        final int testItemCount = 10;

        when(mockSource.getCount()).thenReturn(testItemCount);
        testSources.add(mockSource);

        final MediaSourceAdapter testAdapter = new MediaSourceAdapter(Robolectric.application, testSources, null);

        Assert.assertEquals(testItemCount, testAdapter.getCount());
    }

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

        final MediaSourceAdapter testAdapter = new MediaSourceAdapter(Robolectric.application, testSources, null);

        Assert.assertEquals(testItemCount * testSources.size(), testAdapter.getCount());
    }

    @Test
    public void testGetItem() {
    }

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

        final MediaSourceAdapter testAdapter = new MediaSourceAdapter(Robolectric.application, testSources, null);

        Assert.assertEquals(testItemCount, testAdapter.getItemId(testItemCount));
    }
}
