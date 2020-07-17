package org.wordpress.android.fluxc.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MediaUtilsTest {
    private String[] mSupportedImageSubtypes = {
            "jpeg", "png", "gif"
    };
    private String[] mSupportedVideoSubtypes = {
            "mp4", "quicktime", "x-ms-wmv", "avi", "mpeg", "mp2p", "ogg", "3gpp", "3gpp2"
    };
    private String[] mSupportedAudioSubtypes = {
            "mpeg", "mp4", "ogg", "x-wav"
    };
    private String[] mSupportedApplicationSubtypes = {
            "pdf", "msword", "vnd.openxmlformats-officedocument.wordprocessingml.document", "mspowerpoint",
            "vnd.openxmlformats-officedocument.presentationml.presentation", "vnd.oasis.opendocument.text",
            "vnd.ms-excel", "vnd.openxmlformats-officedocument.spreadsheetml.sheet", "keynote", "zip"
    };
    private String[] mSupportedImageExtensions = {
            "jpg", "jpeg", "png", "gif"
    };
    private String[] mSupportedVideoExtensions = {
            "mp4", "m4v", "mov", "wmv", "avi", "mpg", "ogv", "3gp", "3g2"
    };
    private String[] mSupportedAudioExtensions = {
            "mp3", "m4a", "ogg", "wav"
    };
    private String[] mSupportedApplicationExtensions = {
            "pdf", "doc", "ppt", "odt", "pptx", "docx", "xls", "xlsx", "key", "zip"
    };

    @Test
    public void testImageMimeTypeRecognition() {
        final String[] validImageMimeTypes = {
                "image/jpg", "image/*", "image/png", "image/mp4"
        };
        final String[] invalidImageMimeTypes = {
                "imagejpg", "video/jpg", "", null, "/", "image/jpg/png", "jpg", "jpg/image"
        };

        for (String validImageMimeType : validImageMimeTypes) {
            assertThat(MediaUtils.isImageMimeType(validImageMimeType)).isTrue();
        }
        for (String invalidImageMimeType : invalidImageMimeTypes) {
            assertThat(MediaUtils.isImageMimeType(invalidImageMimeType)).isFalse();
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
            assertThat(MediaUtils.isVideoMimeType(validVideoMimeType)).isTrue();
        }
        for (String invalidVideoMimeType : invalidVideoMimeTypes) {
            assertThat(MediaUtils.isVideoMimeType(invalidVideoMimeType)).isFalse();
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
            assertThat(MediaUtils.isAudioMimeType(validAudioMimeType)).isTrue();
        }
        for (String invalidAudioMimeType : invalidAudioMimeTypes) {
            assertThat(MediaUtils.isAudioMimeType(invalidAudioMimeType)).isFalse();
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
            assertThat(MediaUtils.isApplicationMimeType(validApplicationMimeType)).isTrue();
        }
        for (String invalidApplicationMimeType : invalidApplicationMimeTypes) {
            assertThat(MediaUtils.isApplicationMimeType(invalidApplicationMimeType)).isFalse();
        }
    }

    @Test
    public void testSupportedImageRecognition() {
        final String[] unsupportedImageTypes = {"bmp", "tif", "tiff", "ppm", "pgm", "svg"};
        for (String supportedImageType : mSupportedImageSubtypes) {
            String supportedImageMimeType = "image/" + supportedImageType;
            assertThat(MediaUtils.isSupportedImageMimeType(supportedImageMimeType)).isTrue();
        }
        for (String unsupportedImageType : mSupportedVideoSubtypes) {
            String unsupportedImageMimeType = "image/" + unsupportedImageType;
            assertThat(MediaUtils.isSupportedImageMimeType(unsupportedImageMimeType)).isFalse();
        }
        for (String unsupportedImageType : unsupportedImageTypes) {
            String unsupportedImageMimeType = "image/" + unsupportedImageType;
            assertThat(MediaUtils.isSupportedImageMimeType(unsupportedImageMimeType)).isFalse();
        }
    }

    @Test
    public void testSupportedVideoRecognition() {
        final String[] unsupportedVideoTypes = {"flv", "vob", "yuv", "m2v"};
        for (String supportedVideoType : mSupportedVideoSubtypes) {
            String supportedVideoMimeType = "video/" + supportedVideoType;
            assertThat(MediaUtils.isSupportedVideoMimeType(supportedVideoMimeType)).isTrue();
        }
        for (String unsupportedVideoType : mSupportedApplicationSubtypes) {
            String unsupportedVideoMimeType = "video/" + unsupportedVideoType;
            assertThat(MediaUtils.isSupportedVideoMimeType(unsupportedVideoMimeType)).isFalse();
        }
        for (String unsupportedVideoType : unsupportedVideoTypes) {
            String unsupportedVideoMimeType = "video/" + unsupportedVideoType;
            assertThat(MediaUtils.isSupportedVideoMimeType(unsupportedVideoMimeType)).isFalse();
        }
    }

    @Test
    public void testSupportedAudioRecognition() {
        final String[] unsupportedAudioTypes = {"m4p", "raw", "tta", "wma", "dss", "webm"};
        for (String supportedAudioType : mSupportedAudioSubtypes) {
            String supportedAudioMimeType = "audio/" + supportedAudioType;
            assertThat(MediaUtils.isSupportedAudioMimeType(supportedAudioMimeType)).isTrue();
        }
        for (String unsupportedAudioType : mSupportedApplicationSubtypes) {
            String unsupportedAudioMimeType = "audio/" + unsupportedAudioType;
            assertThat(MediaUtils.isSupportedAudioMimeType(unsupportedAudioMimeType)).isFalse();
        }
        for (String unsupportedAudioType : unsupportedAudioTypes) {
            String unsupportedAudioMimeType = "audio/" + unsupportedAudioType;
            assertThat(MediaUtils.isSupportedAudioMimeType(unsupportedAudioMimeType)).isFalse();
        }
    }

    @Test
    public void testSupportedApplicationRecognition() {
        final String[] unsupportedApplicationTypes = {"com", "bin", "exe", "jar", "xif", "xsl"};
        for (String supportedApplicationType : mSupportedApplicationSubtypes) {
            String supportedApplicationMimeType = "application/" + supportedApplicationType;
            assertThat(MediaUtils.isSupportedApplicationMimeType(supportedApplicationMimeType)).isTrue();
        }
        for (String unsupportedApplicationType : mSupportedImageSubtypes) {
            String unsupportedApplicationMimeType = "application/" + unsupportedApplicationType;
            assertThat(MediaUtils.isSupportedApplicationMimeType(unsupportedApplicationMimeType)).isFalse();
        }
        for (String unsupportedApplicationType : unsupportedApplicationTypes) {
            String unsupportedApplicationMimeType = "application/" + unsupportedApplicationType;
            assertThat(MediaUtils.isSupportedApplicationMimeType(unsupportedApplicationMimeType)).isFalse();
        }
    }

    @Test
    public void testGetMimeTypeFromExtension() {
        for (String supportedImageExtension : mSupportedImageExtensions) {
            String mimeType = MediaUtils.getMimeTypeForExtension(supportedImageExtension);
            assertThat(mimeType).isNotNull();
        }
        for (String supportedVideoExtension : mSupportedVideoExtensions) {
            String mimeType = MediaUtils.getMimeTypeForExtension(supportedVideoExtension);
            assertThat(mimeType).isNotNull();
        }
        for (String supportedAudioExtension : mSupportedAudioExtensions) {
            String mimeType = MediaUtils.getMimeTypeForExtension(supportedAudioExtension);
            assertThat(mimeType).isNotNull();
        }
        for (String supportedApplicationExtension : mSupportedApplicationExtensions) {
            String mimeType = MediaUtils.getMimeTypeForExtension(supportedApplicationExtension);
            assertThat(mimeType).isNotNull();
        }

        final String[] unsupportedImageTypes = {"bmp", "tif", "tiff", "ppm", "pgm", "svg"};
        for (String supportedImageExtension : unsupportedImageTypes) {
            String mimeType = MediaUtils.getMimeTypeForExtension(supportedImageExtension);
            assertThat(mimeType).isNull();
        }
        final String[] unsupportedVideoTypes = {"flv", "vob", "yuv", "m2v"};
        for (String supportedVideoExtension : unsupportedVideoTypes) {
            String mimeType = MediaUtils.getMimeTypeForExtension(supportedVideoExtension);
            assertThat(mimeType).isNull();
        }
        final String[] unsupportedAudioTypes = {"m4p", "raw", "tta", "wma", "dss"};
        for (String supportedAudioExtension : unsupportedAudioTypes) {
            String mimeType = MediaUtils.getMimeTypeForExtension(supportedAudioExtension);
            assertThat(mimeType).isNull();
        }
        final String[] unsupportedApplicationTypes = {"com", "bin", "exe", "jar", "xif", "xsl"};
        for (String supportedApplicationExtension : unsupportedApplicationTypes) {
            String mimeType = MediaUtils.getMimeTypeForExtension(supportedApplicationExtension);
            assertThat(mimeType).isNull();
        }
    }
}
