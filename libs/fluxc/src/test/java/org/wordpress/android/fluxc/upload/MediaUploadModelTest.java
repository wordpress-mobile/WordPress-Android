package org.wordpress.android.fluxc.upload;

import android.text.TextUtils;

import org.junit.Test;
import org.wordpress.android.fluxc.model.MediaUploadModel;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType;
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType.Type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MediaUploadModelTest {
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

    @Test
    public void testNotEqualsOnErrorSubType() {
        MediaUploadModel mediaUploadModel1 = new MediaUploadModel(1);
        MediaUploadModel mediaUploadModel2 = new MediaUploadModel(1);

        MediaError mediaError1 = new MediaError(
                MediaErrorType.MALFORMED_MEDIA_ARG,
                "File type not supported!",
                new MalformedMediaArgSubType(MalformedMediaArgSubType.Type.UNSUPPORTED_MIME_TYPE)
        );

        MediaError mediaError2 = new MediaError(
                MediaErrorType.MALFORMED_MEDIA_ARG,
                "File type not supported!",
                new MalformedMediaArgSubType(Type.DIRECTORY_PATH_SUPPLIED_FILE_NEEDED)
        );

        mediaUploadModel1.setMediaError(mediaError1);
        mediaUploadModel2.setMediaError(mediaError2);

        assertFalse(mediaUploadModel2.equals(mediaUploadModel1));
    }
    public void testEqualsOnErrorSubType() {
        MediaUploadModel mediaUploadModel1 = new MediaUploadModel(1);
        MediaUploadModel mediaUploadModel2 = new MediaUploadModel(1);

        MediaError mediaError1 = new MediaError(
                MediaErrorType.MALFORMED_MEDIA_ARG,
                "File type not supported!",
                new MalformedMediaArgSubType(MalformedMediaArgSubType.Type.UNSUPPORTED_MIME_TYPE)
        );
        MediaError mediaError2 = new MediaError(
                MediaErrorType.MALFORMED_MEDIA_ARG,
                "File type not supported!",
                new MalformedMediaArgSubType(MalformedMediaArgSubType.Type.UNSUPPORTED_MIME_TYPE)
        );

        mediaUploadModel1.setMediaError(mediaError1);
        mediaUploadModel2.setMediaError(mediaError2);

        assertFalse(mediaUploadModel2.equals(mediaUploadModel1));
    }
}
