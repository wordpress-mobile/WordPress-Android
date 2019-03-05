package org.wordpress.android;

import org.junit.Test;
import org.wordpress.android.editor.EditorFragment;
import org.wordpress.android.util.helpers.MediaFile;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class EditorAsyncMediaTest {
    @Test
    public void testHybridEditorUploadingImageSwap() {
        MediaFile mediaFile = generateSampleUploadedPhoto1();

        String expectedTag = String.format(Locale.US, "<a href=\"%s\"><img src=\"%s\" alt=\"\""
                                                      + " class=\"wp-image-%s alignnone size-full\" width=\"%d\""
                                                      + " height=\"%d\"></a>",
                                           mediaFile.getFileURL(), mediaFile.getFileURL(),
                                           mediaFile.getMediaId(), mediaFile.getWidth(), mediaFile.getHeight());

        String uploadingImageHtml = "<span id=\"img_container_54\" class=\"img_container\">"
                                    + "<progress id=\"progress_54\" value=\"0.1\" class=\"wp_media_indicator\">"
                                    + "</progress>"
                                    + "<img data-wpid=\"54\" src=\"/storage/emulated/0/Android/data/image.jpg\" "
                                    + "alt=\"\" class=\"uploading\"></span>";

        // --- Single image post with no other content ---
        String originalContent = "Some text\n" + uploadingImageHtml + "\nMore text";
        String modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals("Some text\n" + expectedTag + "\nMore text", modifiedContent);

        // --- Single image with surrounding text ---
        originalContent = "Some text\n" + uploadingImageHtml + "\nMore text";
        modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals("Some text\n" + expectedTag + "\nMore text", modifiedContent);

        // --- Post with no images ---
        originalContent = "Some text\nMore text";
        modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals(originalContent, modifiedContent);

        // --- Empty post ---
        originalContent = "";
        modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals(originalContent, modifiedContent);
    }

    @Test
    public void testHybridEditorUploadingImageSwapMultiple() {
        MediaFile mediaFile = generateSampleUploadedPhoto1();

        String expectedTag = String.format(Locale.US, "<a href=\"%s\"><img src=\"%s\" alt=\"\""
                                                      + " class=\"wp-image-%s alignnone size-full\" width=\"%d\""
                                                      + " height=\"%d\"></a>",
                                           mediaFile.getFileURL(), mediaFile.getFileURL(),
                                           mediaFile.getMediaId(), mediaFile.getWidth(), mediaFile.getHeight());

        String uploadingImageHtml = "<span id=\"img_container_54\" class=\"img_container\">"
                                    + "<progress id=\"progress_54\" value=\"0.1\" class=\"wp_media_indicator\">"
                                    + "</progress>"
                                    + "<img data-wpid=\"54\" src=\"/storage/emulated/0/Android/data/image.jpg\" "
                                    + "alt=\"\" class=\"uploading\"></span>";

        // --- Post with two uploading images ---
        // -- Replace first image --
        String secondUploadingImageHtml = uploadingImageHtml.replaceAll("54", "65")
                                                            .replaceAll("image.jpg", "image2.jpg");
        String originalContent = "Some text\n" + uploadingImageHtml + "\nMore text" + secondUploadingImageHtml;
        String modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals("Some text\n" + expectedTag + "\nMore text" + secondUploadingImageHtml, modifiedContent);

        // -- Replace second image --
        MediaFile mediaFile2 = generateSampleUploadedPhoto2();
        modifiedContent = EditorFragment.replaceMediaFileWithUrl(modifiedContent, mediaFile2);

        String expectedSecondTag = String.format(Locale.US, "<a href=\"%s\"><img src=\"%s\" alt=\"\""
                                                            + " class=\"wp-image-%s alignnone size-full\" width=\"%d\""
                                                            + " height=\"%d\"></a>",
                                                 mediaFile2.getFileURL(),
                                                 mediaFile2.getFileURL(), mediaFile2.getMediaId(),
                                                 mediaFile2.getWidth(), mediaFile2.getHeight());
        assertEquals("Some text\n" + expectedTag + "\nMore text" + expectedSecondTag, modifiedContent);

        // --- Post with two uploading images, update in reverse order ---
        // -- Replace second image --
        originalContent = "Some text\n" + uploadingImageHtml + "\nMore text" + secondUploadingImageHtml;
        modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile2);

        assertEquals("Some text\n" + uploadingImageHtml + "\nMore text" + expectedSecondTag, modifiedContent);

        // -- Replace first image --
        modifiedContent = EditorFragment.replaceMediaFileWithUrl(modifiedContent, mediaFile);

        assertEquals("Some text\n" + expectedTag + "\nMore text" + expectedSecondTag, modifiedContent);
    }

    @Test
    public void testHybridEditorUploadingImageSwapOldApis() {
        MediaFile mediaFile = generateSampleUploadedPhoto1();

        String expectedTag = String.format(Locale.US, "<a href=\"%s\"><img src=\"%s\" alt=\"\""
                                                      + " class=\"wp-image-%s alignnone size-full\" width=\"%d\""
                                                      + " height=\"%d\"></a>",
                                           mediaFile.getFileURL(), mediaFile.getFileURL(),
                                           mediaFile.getMediaId(), mediaFile.getWidth(), mediaFile.getHeight());

        // Pre-API19, we use nested spans for an 'Uploading...' overlay instead of a progress element
        String uploadingImageHtmlOldApis = "<span id=\"img_container_54\" class=\"img_container compat\"><span "
                                           + "class=\"upload-overlay\">Uploading…</span>"
                                           + "<span class=\"upload-overlay-bg\"></span><img "
                                           + "data-wpid=\"54\" src=\"/storage/emulated/0/Android/data/image.jpg\""
                                           + " alt=\"\" class=\"uploading\"></span>";

        // --- Single image post with no other content ---
        String originalContent = "Some text\n" + uploadingImageHtmlOldApis + "\nMore text";
        String modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals("Some text\n" + expectedTag + "\nMore text", modifiedContent);

        // --- Single image with surrounding text ---
        originalContent = "Some text\n" + uploadingImageHtmlOldApis + "\nMore text";
        modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals("Some text\n" + expectedTag + "\nMore text", modifiedContent);
    }

    @Test
    public void testHybridEditorUploadingImageSwapOldApisMultiple() {
        MediaFile mediaFile = generateSampleUploadedPhoto1();

        String expectedTag = String.format(Locale.US, "<a href=\"%s\"><img src=\"%s\" alt=\"\""
                                                      + " class=\"wp-image-%s alignnone size-full\" width=\"%d\""
                                                      + " height=\"%d\"></a>",
                                           mediaFile.getFileURL(), mediaFile.getFileURL(),
                                           mediaFile.getMediaId(), mediaFile.getWidth(), mediaFile.getHeight());

        // Pre-API19, we use nested spans for an 'Uploading...' overlay instead of a progress element
        String uploadingImageHtmlOldApis = "<span id=\"img_container_54\" class=\"img_container compat\"><span "
                                           + "class=\"upload-overlay\">Uploading…</span>"
                                           + "<span class=\"upload-overlay-bg\"></span><img "
                                           + "data-wpid=\"54\" src=\"/storage/emulated/0/Android/data/image.jpg\""
                                           + " alt=\"\" class=\"uploading\"></span>";

        // --- Post with two uploading images ---
        // -- Replace first image --
        String secondUploadingImageHtml = uploadingImageHtmlOldApis.replaceAll("54", "65")
                                                                   .replaceAll("image.jpg", "image2.jpg");
        String originalContent = "Some text\n" + uploadingImageHtmlOldApis + "\nMore text" + secondUploadingImageHtml;
        String modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals("Some text\n" + expectedTag + "\nMore text" + secondUploadingImageHtml, modifiedContent);

        // -- Replace second image --
        MediaFile mediaFile2 = generateSampleUploadedPhoto2();
        modifiedContent = EditorFragment.replaceMediaFileWithUrl(modifiedContent, mediaFile2);

        String expectedSecondTag = String.format(Locale.US, "<a href=\"%s\"><img src=\"%s\" alt=\"\""
                                                            + " class=\"wp-image-%s alignnone size-full\" width=\"%d\""
                                                            + " height=\"%d\"></a>",
                                                 mediaFile2.getFileURL(),
                                                 mediaFile2.getFileURL(), mediaFile2.getMediaId(),
                                                 mediaFile2.getWidth(), mediaFile2.getHeight());
        assertEquals("Some text\n" + expectedTag + "\nMore text" + expectedSecondTag, modifiedContent);

        // --- Post with two uploading images, update in reverse order ---
        // -- Replace second image --
        originalContent = "Some text\n" + uploadingImageHtmlOldApis + "\nMore text" + secondUploadingImageHtml;
        modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile2);

        assertEquals("Some text\n" + uploadingImageHtmlOldApis + "\nMore text" + expectedSecondTag, modifiedContent);

        // -- Replace first image --
        modifiedContent = EditorFragment.replaceMediaFileWithUrl(modifiedContent, mediaFile);

        assertEquals("Some text\n" + expectedTag + "\nMore text" + expectedSecondTag, modifiedContent);
    }

    @Test
    public void testHybridEditorUploadingVideoSwapVideopress() {
        MediaFile mediaFile = generateSampleUploadedVideoVideopress();

        String expectedTag = mediaFile.getVideoPressShortCode();

        String uploadingVideoHtml = "<span id=\"video_container_87\" class=\"video_container\">"
                                    + "<progress id=\"progress_87\" value=\"0.09\" class=\"wp_media_indicator\""
                                    + " contenteditable=\"false\">"
                                    + "</progress><img data-video_wpid=\"87\" src=\"/data/user/0/org.wordpress"
                                    + ".android.beta/cache/thumb-67374471.png\" alt=\"\" class=\"uploading\"></span>";

        // --- Single video post with no other content ---
        String originalContent = "Some text\n" + uploadingVideoHtml + "\nMore text";
        String modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals("Some text\n" + expectedTag + "\nMore text", modifiedContent);

        // --- Single video with surrounding text ---
        originalContent = "Some text\n" + uploadingVideoHtml + "\nMore text";
        modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals("Some text\n" + expectedTag + "\nMore text", modifiedContent);

        // --- Post with no media ---
        originalContent = "Some text\nMore text";
        modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals(originalContent, modifiedContent);

        // --- Empty post ---
        originalContent = "";
        modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals(originalContent, modifiedContent);
    }

    @Test
    public void testHybridEditorUploadingVideoSwapSelfHosted() {
        MediaFile mediaFile = generateSampleUploadedVideo();

        String expectedTag = String.format(Locale.US, "[video src=\"%s\" poster=\"%s\"][/video]",
                                           mediaFile.getFileURL(), mediaFile.getThumbnailURL());

        String uploadingVideoHtml = "<span id=\"video_container_76\" class=\"video_container\">"
                                    + "<progress id=\"progress_76\" value=\"0.09\" class=\"wp_media_indicator\""
                                    + " contenteditable=\"false\">"
                                    + "</progress><img data-video_wpid=\"76\" src=\"/data/user/0/org.wordpress"
                                    + ".android.beta/cache/thumb-67374471.png\" alt=\"\" class=\"uploading\"></span>";

        // --- Single video post with no other content ---
        String originalContent = "Some text\n" + uploadingVideoHtml + "\nMore text";
        String modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals("Some text\n" + expectedTag + "\nMore text", modifiedContent);

        // --- Single video with surrounding text ---
        originalContent = "Some text\n" + uploadingVideoHtml + "\nMore text";
        modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals("Some text\n" + expectedTag + "\nMore text", modifiedContent);

        // --- Post with no media ---
        originalContent = "Some text\nMore text";
        modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals(originalContent, modifiedContent);

        // --- Empty post ---
        originalContent = "";
        modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals(originalContent, modifiedContent);
    }

    @Test
    public void testHybridEditorUploadingVideoSwapOldApis() {
        MediaFile mediaFile = generateSampleUploadedVideoVideopress();

        String expectedTag = mediaFile.getVideoPressShortCode();

        // Pre-API19, we use nested spans for an 'Uploading...' overlay instead of a progress element
        String uploadingImageHtmlOldApis = "<span id=\"video_container_87\" class=\"video_container compat\">"
                                           + "<span class=\"upload-overlay\">Uploading…</span>"
                                           + "<span class=\"upload-overlay-bg\"></span>"
                                           + "<img data-video_wpid=\"87\" "
                                           + "src=\"/storage/emulated/0/Android/data/image.jpg\" alt=\"\""
                                           + " class=\"uploading\"></span>";

        // --- Single video post with no other content ---
        String originalContent = "Some text\n" + uploadingImageHtmlOldApis + "\nMore text";
        String modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals("Some text\n" + expectedTag + "\nMore text", modifiedContent);

        // --- Single video with surrounding text ---
        originalContent = "Some text\n" + uploadingImageHtmlOldApis + "\nMore text";
        modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals("Some text\n" + expectedTag + "\nMore text", modifiedContent);
    }

    private static MediaFile generateSampleUploadedPhoto1() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(54);
        mediaFile.setMediaId("673");
        mediaFile.setFileURL("http://mysite.wordpress.com/something.jpg");
        mediaFile.setWidth(600);
        mediaFile.setHeight(700);

        return mediaFile;
    }

    private static MediaFile generateSampleUploadedPhoto2() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(65);
        mediaFile.setMediaId("679");
        mediaFile.setFileURL("http://mysite.wordpress.com/something2.jpg");
        mediaFile.setWidth(400);
        mediaFile.setHeight(800);

        return mediaFile;
    }

    private static MediaFile generateSampleUploadedVideo() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(76);
        mediaFile.setMediaId("679");
        mediaFile.setFileURL("http://mysite.wordpress.com/something2.mp4");
        mediaFile.setThumbnailURL("http://mysite.wordpress.com/something2-thumb.png");
        mediaFile.setWidth(400);
        mediaFile.setHeight(800);
        mediaFile.setVideo(true);

        return mediaFile;
    }

    private static MediaFile generateSampleUploadedVideoVideopress() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(87);
        mediaFile.setMediaId("679");
        mediaFile.setFileURL("http://mysite.wordpress.com/something2.mp4");
        mediaFile.setThumbnailURL("http://mysite.wordpress.com/something2-thumb.png");
        mediaFile.setWidth(400);
        mediaFile.setHeight(800);
        mediaFile.setVideo(true);
        mediaFile.setVideoPressShortCode("[wpvideo 225325]");

        return mediaFile;
    }
}
