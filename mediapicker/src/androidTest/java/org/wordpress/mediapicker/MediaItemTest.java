package org.wordpress.mediapicker;

import android.os.Parcel;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

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
    public void testWriteToParcelWithValidData() {
        final String testTitle = "test-title";
        final String testTag = "test-tag";
        final String testSource = "test-source";
        final String testPreview = "test-preview";
        final int testRotation = 180;

        final MediaItem testItem = new MediaItem();
        final Parcel mockParcel = mock(Parcel.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object parameter = invocationOnMock.getArguments()[0];
                Assert.assertTrue(parameter instanceof List<?>);
                List<String> parcelData = (List<String>) parameter;

                if (parcelData.size() > 0) {
                    while (parcelData.size() > 0) {
                        String data = parcelData.remove(0);
                        String key = data.substring(0, data.indexOf('='));
                        String value = data.substring(data.indexOf('=') + 1, data.length());

                        if (!key.isEmpty()) {
                            switch (key) {
                                case MediaItem.PARCEL_KEY_TAG:
                                    Assert.assertTrue(testTag.equals(value));
                                    break;
                                case MediaItem.PARCEL_KEY_TITLE:
                                    Assert.assertTrue(testTitle.equals(value));
                                    break;
                                case MediaItem.PARCEL_KEY_PREVIEW:
                                    Assert.assertTrue(testPreview.equals(value));
                                    break;
                                case MediaItem.PARCEL_KEY_SOURCE:
                                    Assert.assertTrue(testSource.equals(value));
                                    break;
                                case MediaItem.PARCEL_KEY_ROTATION:
                                    Assert.assertTrue(testRotation == Integer.valueOf(value));
                                    break;
                            }
                        }
                    }
                }

                return null;
            }
        }).when(mockParcel).writeStringList(anyListOf(String.class));

        testItem.setTitle(testTitle);
        testItem.setTag(testTag);
        testItem.setSource(testSource);
        testItem.setPreviewSource(testPreview);
        testItem.setRotation(testRotation);

        testItem.writeToParcel(mockParcel, 0);
    }

    @Test
    public void testWriteToParcelWithNullData() {
        final MediaItem testItem = new MediaItem();
        final Parcel mockParcel = mock(Parcel.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object parameter = invocationOnMock.getArguments()[0];
                Assert.assertTrue(parameter instanceof List<?>);
                List<String> parcelData = (List<String>) parameter;

                if (parcelData.size() > 0) {
                    while (parcelData.size() > 0) {
                        String data = parcelData.remove(0);
                        String key = data.substring(0, data.indexOf('='));
                        String value = data.substring(data.indexOf('=') + 1, data.length());

                        if (!key.isEmpty()) {
                            switch (key) {
                                case MediaItem.PARCEL_KEY_TAG:
                                    Assert.fail();
                                    break;
                                case MediaItem.PARCEL_KEY_TITLE:
                                    Assert.fail();
                                    break;
                                case MediaItem.PARCEL_KEY_PREVIEW:
                                    Assert.fail();
                                    break;
                                case MediaItem.PARCEL_KEY_SOURCE:
                                    Assert.fail();
                                    break;
                                case MediaItem.PARCEL_KEY_ROTATION:
                                    Assert.assertTrue(0 == Integer.valueOf(value));
                                    break;
                            }
                        }
                    }
                }

                return null;
            }
        }).when(mockParcel).writeStringList(anyListOf(String.class));

        testItem.writeToParcel(mockParcel, 0);

        // Default values are null, now test with empty values
        testItem.setTitle("");
        testItem.setTag("");
        testItem.setSource("");
        testItem.setPreviewSource("");

        testItem.writeToParcel(mockParcel, 0);
    }

    @Test
    public void testDescribeContents() {
        final MediaItem testItem = new MediaItem();
        final int expectedContents = 0;

        Assert.assertEquals(expectedContents, testItem.describeContents());
    }
}
