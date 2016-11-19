package org.wordpress.android.models;

import org.wordpress.android.ui.reader.models.ReaderImageList;
import org.wordpress.android.ui.reader.utils.ReaderImageScanner;
import org.wordpress.android.ui.reader.views.ReaderThumbnailStrip;
import org.wordpress.android.util.HtmlUtils;

/**
 * Used by the reader stream view to determine which type of "card" to use
 */

public enum ReaderCardType {
    DEFAULT,
    PHOTO,
    GALLERY;

    private static final int MAX_TEXT_CHARS = 100;

    public static ReaderCardType fromReaderPost(ReaderPost post) {
        if (post == null) {
            return DEFAULT;
        }

        // if this post is a gallery, scan it to make sure we have enough images to
        // show in the stream's thumbnail strip
        if (post.isGallery()) {
            ReaderImageList imageList =
                    new ReaderImageScanner(post.getText(), post.isPrivate).getGalleryImageList();
            if (imageList.size() >= ReaderThumbnailStrip.IMAGE_COUNT) {
                return GALLERY;
            }
        }

        // posts with a featured image and little or no text get the photo card treatment - note
        // that we have to strip HTML tags from the text to determine its length, which can be
        // an expensive operation so we try to avoid it
        if (post.hasFeaturedImage()) {
            if (post.getExcerpt().length() > MAX_TEXT_CHARS) {
                return DEFAULT;
            }
            if (post.getText().length() < MAX_TEXT_CHARS) {
                return PHOTO;
            }
            if (HtmlUtils.fastStripHtml(post.getText()).length() < MAX_TEXT_CHARS) {
                return PHOTO;
            }
        }

        return DEFAULT;
    }

    public static String toString(ReaderCardType cardType) {
        if (cardType == null) {
            return "DEFAULT";
        }
        switch (cardType) {
            case PHOTO:
                return "PHOTO";
            case GALLERY:
                return "GALLERY";
            default:
                return "DEFAULT";
        }
    }

    public static ReaderCardType fromString(String s) {
        if ("PHOTO".equals(s)) {
            return PHOTO;
        }
        if ("GALLERY".equals(s)) {
            return GALLERY;
        }
        return DEFAULT;
    }
}