package org.wordpress.mediapicker.source;

import android.os.Parcel;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.wordpress.mediapicker.MediaItem;

import java.util.List;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class MediaSourceDeviceImagesTest {
    /**
     * Verifies that the CREATOR correctly constructs a {@link org.wordpress.mediapicker.source.MediaSourceDeviceImages}
     * from valid {@link android.os.Parcel} data.
     */
    @Test
    public void testCreator() {
        final String testTitle = "test-title";
        final String testTag = "test-tag";
        final String testSource = "test-source";
        final String testPreview = "test-preview";
        final int testRotation = 180;
        final MediaItem testItem = new MediaItem();

        testItem.setTag(testTag);
        testItem.setTitle(testTitle);
        testItem.setSource(testSource);
        testItem.setPreviewSource(testPreview);
        testItem.setRotation(testRotation);

        final Parcel mockParcel = mock(Parcel.class);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object parameter = invocationOnMock.getArguments()[0];
                Assert.assertTrue(parameter instanceof List<?>);
                List<MediaItem> parcelData = (List<MediaItem>) parameter;

                parcelData.add(testItem);

                return null;
            }
        }).when(mockParcel).readTypedList(anyListOf(MediaItem.class), eq(MediaItem.CREATOR));

        final MediaSourceDeviceImages testMediaSource = MediaSourceDeviceImages.CREATOR.createFromParcel(mockParcel);
        final MediaItem actualItem = testMediaSource.getMedia(0);

        Assert.assertEquals(testTag, actualItem.getTag());
        Assert.assertEquals(testTitle, actualItem.getTitle());
        Assert.assertEquals(testSource, actualItem.getSource().toString());
        Assert.assertEquals(testPreview, actualItem.getPreviewSource().toString());
        Assert.assertEquals(testRotation, actualItem.getRotation());
    }

    /**
     * Verifies that no media is present when no {@link android.content.ContentResolver} is given.
     */
    @Test
    public void testNoMedia() {
        final MediaSourceDeviceImages testSource = new MediaSourceDeviceImages();

        Assert.assertEquals(0, testSource.getCount());
        Assert.assertEquals(null, testSource.getMedia(0));
    }
}
