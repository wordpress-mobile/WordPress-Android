package org.wordpress.android.fluxc.utils;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MediaUtilsTest {
    @Test
    public void testImageMimeTypeRecognition() {
        final String[] validImageMimeTypes = {
                "image/jpg", "image/*", "image/png", "image/mp4"
        };
        final String[] invalidImageMimeTypes = {
                "imagejpg", "video/jpg", "", null, "/", "image/jpg/png", "jpg", "jpg/image"
        };

        for (String validImageMimeType : validImageMimeTypes) {
            Assert.assertTrue(MediaUtils.isImageMimeType(validImageMimeType));
        }
        for (String invalidImageMimeType : invalidImageMimeTypes) {
            Assert.assertFalse(MediaUtils.isImageMimeType(invalidImageMimeType));
        }
    }

    @Test
    public void testVideoMimeTypeRecognition() {
        final String[] validVideoMimeTypes = {
                "video/mp4", "video/*", "video/mkv", "video/png"
        };
        final String[] invalidVideoMimeTypes = {
                "videomp4", "image/mp4", "", null, "/", "video/mp4/mkv", "mp4", "mp4/video"
        };

        for (String validVideoMimeType : validVideoMimeTypes) {
            Assert.assertTrue(MediaUtils.isVideoMimeType(validVideoMimeType));
        }
        for (String invalidVideoMimeType : invalidVideoMimeTypes) {
            Assert.assertFalse(MediaUtils.isVideoMimeType(invalidVideoMimeType));
        }
    }

    @Test
    public void testAudioMimeTypeRecognition() {
        final String[] validAudioMimeTypes = {
                "audio/mp3", "audio/*", "audio/wav", "audio/png"
        };
        final String[] invalidAudioMimeTypes = {
                "audiomp3", "video/mp3", "", null, "/", "audio/mp4/mkv", "mp3", "mp4/audio"
        };

        for (String validAudioMimeType : validAudioMimeTypes) {
            Assert.assertTrue(MediaUtils.isAudioMimeType(validAudioMimeType));
        }
        for (String invalidAudioMimeType : invalidAudioMimeTypes) {
            Assert.assertFalse(MediaUtils.isAudioMimeType(invalidAudioMimeType));
        }
    }

    @Test
    public void testApplicationMimeTypeRecognition() {
        final String[] validApplicationMimeTypes = {
                "application/pdf", "application/*", "application/ppsx", "application/png"
        };
        final String[] invalidApplicationMimeTypes = {
                "applicationpdf", "audio/pdf", "", null, "/", "application/pdf/doc", "pdf", "pdf/application"
        };

        for (String validApplicationMimeType : validApplicationMimeTypes) {
            Assert.assertTrue(MediaUtils.isApplicationMimeType(validApplicationMimeType));
        }
        for (String invalidApplicationMimeType : invalidApplicationMimeTypes) {
            Assert.assertFalse(MediaUtils.isApplicationMimeType(invalidApplicationMimeType));
        }
    }

    @Test
    public void testSupportedImageRecognition() {
        final String[] unsupportedImageTypes = { "bmp", "tif", "tiff", "ppm", "pgm", "svg" };
        for (String supportedImageType : MediaUtils.SUPPORTED_IMAGE_SUBTYPES) {
            String supportedImageMimeType = MediaUtils.MIME_TYPE_IMAGE + supportedImageType;
            Assert.assertTrue(MediaUtils.isSupportedImageMimeType(supportedImageMimeType));
        }
        for (String unsupportedImageType : MediaUtils.SUPPORTED_VIDEO_SUBTYPES) {
            String unsupportedImageMimeType = MediaUtils.MIME_TYPE_IMAGE + unsupportedImageType;
            Assert.assertFalse(MediaUtils.isSupportedImageMimeType(unsupportedImageMimeType));
        }
        for (String unsupportedImageType : unsupportedImageTypes) {
            String unsupportedImageMimeType = MediaUtils.MIME_TYPE_IMAGE + unsupportedImageType;
            Assert.assertFalse(MediaUtils.isSupportedImageMimeType(unsupportedImageMimeType));
        }
    }

    @Test
    public void testSupportedVideoRecognition() {
        final String[] unsupportedVideoTypes = { "flv", "webm", "vob", "yuv", "mpeg", "m2v" };
        for (String supportedVideoType : MediaUtils.SUPPORTED_VIDEO_SUBTYPES) {
            String supportedVideoMimeType = MediaUtils.MIME_TYPE_VIDEO + supportedVideoType;
            Assert.assertTrue(MediaUtils.isSupportedVideoMimeType(supportedVideoMimeType));
        }
        for (String unsupportedVideoType : MediaUtils.SUPPORTED_AUDIO_SUBTYPES) {
            String unsupportedVideoMimeType = MediaUtils.MIME_TYPE_VIDEO + unsupportedVideoType;
            Assert.assertFalse(MediaUtils.isSupportedVideoMimeType(unsupportedVideoMimeType));
        }
        for (String unsupportedVideoType : unsupportedVideoTypes) {
            String unsupportedVideoMimeType = MediaUtils.MIME_TYPE_VIDEO + unsupportedVideoType;
            Assert.assertFalse(MediaUtils.isSupportedVideoMimeType(unsupportedVideoMimeType));
        }
    }

    @Test
    public void testSupportedAudioRecognition() {
        final String[] unsupportedAudioTypes = { "m4p", "raw", "tta", "wma", "dss", "webm" };
        for (String supportedAudioType : MediaUtils.SUPPORTED_AUDIO_SUBTYPES) {
            String supportedAudioMimeType = MediaUtils.MIME_TYPE_AUDIO + supportedAudioType;
            Assert.assertTrue(MediaUtils.isSupportedAudioMimeType(supportedAudioMimeType));
        }
        for (String unsupportedAudioType : MediaUtils.SUPPORTED_APPLICATION_SUBTYPES) {
            String unsupportedAudioMimeType = MediaUtils.MIME_TYPE_AUDIO + unsupportedAudioType;
            Assert.assertFalse(MediaUtils.isSupportedAudioMimeType(unsupportedAudioMimeType));
        }
        for (String unsupportedAudioType : unsupportedAudioTypes) {
            String unsupportedAudioMimeType = MediaUtils.MIME_TYPE_AUDIO + unsupportedAudioType;
            Assert.assertFalse(MediaUtils.isSupportedAudioMimeType(unsupportedAudioMimeType));
        }
    }

    @Test
    public void testSupportedApplicationRecognition() {
        final String[] unsupportedApplicationTypes = { "com", "bin", "exe", "jar", "xif", "xsl" };
        for (String supportedApplicationType : MediaUtils.SUPPORTED_APPLICATION_SUBTYPES) {
            String supportedApplicationMimeType = MediaUtils.MIME_TYPE_APPLICATION + supportedApplicationType;
            Assert.assertTrue(MediaUtils.isSupportedApplicationMimeType(supportedApplicationMimeType));
        }
        for (String unsupportedApplicationType : MediaUtils.SUPPORTED_IMAGE_SUBTYPES) {
            String unsupportedApplicationMimeType = MediaUtils.MIME_TYPE_APPLICATION + unsupportedApplicationType;
            Assert.assertFalse(MediaUtils.isSupportedApplicationMimeType(unsupportedApplicationMimeType));
        }
        for (String unsupportedApplicationType : unsupportedApplicationTypes) {
            String unsupportedApplicationMimeType = MediaUtils.MIME_TYPE_APPLICATION + unsupportedApplicationType;
            Assert.assertFalse(MediaUtils.isSupportedApplicationMimeType(unsupportedApplicationMimeType));
        }
    }

    @Test
    public void testGetMimeTypeFromExtension() {
        for (String supportedImageExtension : MediaUtils.SUPPORTED_IMAGE_SUBTYPES) {
            String mimeType = MediaUtils.getMimeTypeForExtension(supportedImageExtension);
            Assert.assertNotNull(mimeType);
            Assert.assertTrue(mimeType.equals(MediaUtils.MIME_TYPE_IMAGE + supportedImageExtension));
        }
        for (String supportedVideoExtension : MediaUtils.SUPPORTED_VIDEO_SUBTYPES) {
            String mimeType = MediaUtils.getMimeTypeForExtension(supportedVideoExtension);
            Assert.assertNotNull(mimeType);
            Assert.assertTrue(mimeType.equals(MediaUtils.MIME_TYPE_VIDEO + supportedVideoExtension));
        }
        for (String supportedAudioExtension : MediaUtils.SUPPORTED_AUDIO_SUBTYPES) {
            String mimeType = MediaUtils.getMimeTypeForExtension(supportedAudioExtension);
            Assert.assertNotNull(mimeType);
            Assert.assertTrue(mimeType.equals(MediaUtils.MIME_TYPE_AUDIO + supportedAudioExtension));
        }
        for (String supportedApplicationExtension : MediaUtils.SUPPORTED_APPLICATION_SUBTYPES) {
            String mimeType = MediaUtils.getMimeTypeForExtension(supportedApplicationExtension);
            Assert.assertNotNull(mimeType);
            Assert.assertTrue(mimeType.equals(MediaUtils.MIME_TYPE_APPLICATION + supportedApplicationExtension));
        }

        final String[] unsupportedImageTypes = { "bmp", "tif", "tiff", "ppm", "pgm", "svg" };
        for (String supportedImageExtension : unsupportedImageTypes) {
            String mimeType = MediaUtils.getMimeTypeForExtension(supportedImageExtension);
            Assert.assertNull(mimeType);
        }
        final String[] unsupportedVideoTypes = { "flv", "webm", "vob", "yuv", "mpeg", "m2v" };
        for (String supportedVideoExtension : unsupportedVideoTypes) {
            String mimeType = MediaUtils.getMimeTypeForExtension(supportedVideoExtension);
            Assert.assertNull(mimeType);
        }
        final String[] unsupportedAudioTypes = { "m4p", "raw", "tta", "wma", "dss", "webm" };
        for (String supportedAudioExtension : unsupportedAudioTypes) {
            String mimeType = MediaUtils.getMimeTypeForExtension(supportedAudioExtension);
            Assert.assertNull(mimeType);
        }
        final String[] unsupportedApplicationTypes = { "com", "bin", "exe", "jar", "xif", "xsl" };
        for (String supportedApplicationExtension : unsupportedApplicationTypes) {
            String mimeType = MediaUtils.getMimeTypeForExtension(supportedApplicationExtension);
            Assert.assertNull(mimeType);
        }
    }
}
