package org.wordpress.android.ui.posts;

import android.test.AndroidTestCase;

import org.wordpress.android.editor.EditorFragment;
import org.wordpress.android.util.helpers.MediaFile;

import java.util.Locale;

public class EditorAsyncMediaTest extends AndroidTestCase {
    public void testHybridEditorUploadingImageSwap() {
        MediaFile mediaFile = generateSampleUploadedMediaFile1();

        String expectedTag = String.format(Locale.US, "<a href=\"%s\"><img src=\"%s\" alt=\"\" class=\"wp-image-%s " +
                "alignnone size-full\" width=\"%d\" height=\"%d\"></a>", mediaFile.getFileURL(), mediaFile.getFileURL(),
                mediaFile.getMediaId(), mediaFile.getWidth(), mediaFile.getHeight());

        String uploadingImageHtml = "<span id=\"img_container_54\" class=\"img_container\">" +
                "<progress id=\"progress_54\" value=\"0.1\" class=\"wp_media_indicator\"></progress>" +
                "<img data-wpid=\"54\" src=\"/storage/emulated/0/Android/data/image.jpg\" " +
                "alt=\"\" class=\"uploading\"></span>";

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

    public void testHybridEditorUploadingImageSwapMultiple() {
        MediaFile mediaFile = generateSampleUploadedMediaFile1();

        String expectedTag = String.format(Locale.US, "<a href=\"%s\"><img src=\"%s\" alt=\"\" class=\"wp-image-%s " +
                "alignnone size-full\" width=\"%d\" height=\"%d\"></a>", mediaFile.getFileURL(), mediaFile.getFileURL(),
                mediaFile.getMediaId(), mediaFile.getWidth(), mediaFile.getHeight());

        String uploadingImageHtml = "<span id=\"img_container_54\" class=\"img_container\">" +
                "<progress id=\"progress_54\" value=\"0.1\" class=\"wp_media_indicator\"></progress>" +
                "<img data-wpid=\"54\" src=\"/storage/emulated/0/Android/data/image.jpg\" " +
                "alt=\"\" class=\"uploading\"></span>";

        // --- Post with two uploading images ---
        // -- Replace first image --
        String secondUploadingImageHtml = uploadingImageHtml.replaceAll("54", "65")
                .replaceAll("image.jpg", "image2.jpg");
        String originalContent = "Some text\n" + uploadingImageHtml + "\nMore text" + secondUploadingImageHtml;
        String modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals("Some text\n" + expectedTag + "\nMore text" + secondUploadingImageHtml, modifiedContent);

        // -- Replace second image --
        MediaFile mediaFile2 = generateSampleUploadedMediaFile2();
        modifiedContent = EditorFragment.replaceMediaFileWithUrl(modifiedContent, mediaFile2);

        String expectedSecondTag = String.format(Locale.US, "<a href=\"%s\"><img src=\"%s\" alt=\"\"" +
                " class=\"wp-image-%s alignnone size-full\" width=\"%d\" height=\"%d\"></a>", mediaFile2.getFileURL(),
                mediaFile2.getFileURL(), mediaFile2.getMediaId(), mediaFile2.getWidth(), mediaFile2.getHeight());
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

    public void testHybridEditorUploadingImageSwapOldApis() {
        MediaFile mediaFile = generateSampleUploadedMediaFile1();

        String expectedTag = String.format(Locale.US, "<a href=\"%s\"><img src=\"%s\" alt=\"\" class=\"wp-image-%s " +
                "alignnone size-full\" width=\"%d\" height=\"%d\"></a>", mediaFile.getFileURL(), mediaFile.getFileURL(),
                mediaFile.getMediaId(), mediaFile.getWidth(), mediaFile.getHeight());

        // Pre-API19, we use nested spans for an 'Uploading...' overlay instead of a progress element
        String uploadingImageHtmlOldApis = "<span id=\"img_container_54\" class=\"img_container compat\"><span " +
                "class=\"upload-overlay\">Uploading…</span><span class=\"upload-overlay-bg\"></span><img " +
                "data-wpid=\"54\" src=\"/storage/emulated/0/Android/data/image.jpg\" alt=\"\" class=\"uploading\">" +
                "</span>";

        // --- Single image post with no other content ---
        String originalContent = "Some text\n" + uploadingImageHtmlOldApis + "\nMore text";
        String modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals("Some text\n" + expectedTag + "\nMore text", modifiedContent);

        // --- Single image with surrounding text ---
        originalContent = "Some text\n" + uploadingImageHtmlOldApis + "\nMore text";
        modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals("Some text\n" + expectedTag + "\nMore text", modifiedContent);
    }

    public void testHybridEditorUploadingImageSwapOldApisMultiple() {
        MediaFile mediaFile = generateSampleUploadedMediaFile1();

        String expectedTag = String.format(Locale.US, "<a href=\"%s\"><img src=\"%s\" alt=\"\" class=\"wp-image-%s " +
                "alignnone size-full\" width=\"%d\" height=\"%d\"></a>", mediaFile.getFileURL(), mediaFile.getFileURL(),
                mediaFile.getMediaId(), mediaFile.getWidth(), mediaFile.getHeight());

        // Pre-API19, we use nested spans for an 'Uploading...' overlay instead of a progress element
        String uploadingImageHtmlOldApis = "<span id=\"img_container_54\" class=\"img_container compat\"><span " +
                "class=\"upload-overlay\">Uploading…</span><span class=\"upload-overlay-bg\"></span><img " +
                "data-wpid=\"54\" src=\"/storage/emulated/0/Android/data/image.jpg\" alt=\"\" class=\"uploading\">" +
                "</span>";

        // --- Post with two uploading images ---
        // -- Replace first image --
        String secondUploadingImageHtml = uploadingImageHtmlOldApis.replaceAll("54", "65")
                .replaceAll("image.jpg", "image2.jpg");
        String originalContent = "Some text\n" + uploadingImageHtmlOldApis + "\nMore text" + secondUploadingImageHtml;
        String modifiedContent = EditorFragment.replaceMediaFileWithUrl(originalContent, mediaFile);

        assertEquals("Some text\n" + expectedTag + "\nMore text" + secondUploadingImageHtml, modifiedContent);

        // -- Replace second image --
        MediaFile mediaFile2 = generateSampleUploadedMediaFile2();
        modifiedContent = EditorFragment.replaceMediaFileWithUrl(modifiedContent, mediaFile2);

        String expectedSecondTag = String.format(Locale.US, "<a href=\"%s\"><img src=\"%s\" alt=\"\"" +
                " class=\"wp-image-%s alignnone size-full\" width=\"%d\" height=\"%d\"></a>", mediaFile2.getFileURL(),
                mediaFile2.getFileURL(), mediaFile2.getMediaId(), mediaFile2.getWidth(), mediaFile2.getHeight());
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

    private static MediaFile generateSampleUploadedMediaFile1() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(54);
        mediaFile.setMediaId("673");
        mediaFile.setFileURL("http://mysite.wordpress.com/something.jpg");
        mediaFile.setWidth(600);
        mediaFile.setHeight(700);

        return mediaFile;
    }

    private static MediaFile generateSampleUploadedMediaFile2() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(65);
        mediaFile.setMediaId("679");
        mediaFile.setFileURL("http://mysite.wordpress.com/something2.jpg");
        mediaFile.setWidth(400);
        mediaFile.setHeight(800);

        return mediaFile;
    }
}
