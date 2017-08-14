package org.wordpress.android.fluxc.upload;

import android.text.TextUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.model.MediaUploadModel;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class MediaUploadModelTest {
    @Before
    public void setUp() {
    }

    @Test
    public void testEquals() {
        MediaUploadModel mediaUploadModel1 = new MediaUploadModel(1);
        MediaUploadModel mediaUploadModel2 = new MediaUploadModel(1);

        mediaUploadModel1.setUploadState(MediaUploadModel.FAILED);
        assertFalse(mediaUploadModel1.equals(mediaUploadModel2));

        mediaUploadModel2.setUploadState(MediaUploadModel.FAILED);

        MediaError mediaError = new MediaError(MediaErrorType.EXCEEDS_MEMORY_LIMIT, "Too large!");
        mediaUploadModel1.setMediaError(mediaError);
        assertFalse(mediaUploadModel1.equals(mediaUploadModel2));

        mediaUploadModel2.setErrorType(mediaError.type.toString());
        mediaUploadModel2.setErrorMessage(mediaError.message);

        assertTrue(mediaUploadModel1.equals(mediaUploadModel2));
    }

    @Test
    public void testMediaError() {
        MediaUploadModel mediaUploadModel = new MediaUploadModel(1);

        assertNull(mediaUploadModel.getMediaError());
        assertTrue(TextUtils.isEmpty(mediaUploadModel.getErrorType()));
        assertTrue(TextUtils.isEmpty(mediaUploadModel.getErrorMessage()));

        mediaUploadModel.setMediaError(new MediaError(MediaErrorType.EXCEEDS_MEMORY_LIMIT, "Too large!"));
        assertNotNull(mediaUploadModel.getMediaError());
        assertEquals(MediaErrorType.EXCEEDS_MEMORY_LIMIT, MediaErrorType.fromString(mediaUploadModel.getErrorType()));
        assertEquals("Too large!", mediaUploadModel.getErrorMessage());
    }
}
