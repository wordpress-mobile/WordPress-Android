package org.wordpress.android.models;

import androidx.annotation.NonNull;

import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.utils.ReaderImageScanner;
import org.wordpress.android.util.HtmlUtils;

/**
 * Used by the reader stream view to determine which type of "card" to use
 */

public enum ReaderCardType {
    DEFAULT,
    PHOTO,
    GALLERY,
    VIDEO;

    private static final int MIN_CONTENT_CHARS = 100;

    public static ReaderCardType fromReaderPost(ReaderPost post) {
        if (post == null) {
            return DEFAULT;
        }

        // posts with a featured image and little or no text get the photo card treatment
        if (post.hasFeaturedImage() && hasMinContent(post)) {
            return PHOTO;
        }

        // posts that have a featured video show an embedded video card
        if (post.hasFeaturedVideo()) {
            return VIDEO;
        }

        // if this post doesn't have a featured image but has enough usable images to fill the
        // stream's thumbnail strip, treat it as a gallery
        if (!post.hasFeaturedImage()
            && post.hasImages()
            && new ReaderImageScanner(post.getText(), post.isPrivate)
                    .hasUsableImageCount(ReaderConstants.THUMBNAIL_STRIP_IMG_COUNT,
                            ReaderConstants.MIN_GALLERY_IMAGE_WIDTH)) {
            return GALLERY;
        }

        return DEFAULT;
    }

    /*
     * returns true if the post's content is 100 characters or less
     */
    private static boolean hasMinContent(@NonNull ReaderPost post) {
        if (post.getExcerpt().length() > MIN_CONTENT_CHARS) {
            return false;
        }
        if (post.getText().length() <= MIN_CONTENT_CHARS) {
            return true;
        }
        return (HtmlUtils.fastStripHtml(post.getText()).length() <= MIN_CONTENT_CHARS);
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
            case VIDEO:
                return "VIDEO";
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
        if ("VIDEO".equals(s)) {
            return VIDEO;
        }
        return DEFAULT;
    }
}
