package org.wordpress.android.models;

import android.support.annotation.NonNull;

import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.utils.ReaderImageScanner;
import org.wordpress.android.ui.reader.views.ReaderThumbnailStrip;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

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

        // posts that have a usable featured video show an embedded video card
        if (post.hasFeaturedVideo() && WPNetworkImageView.canShowVideoThumbnail(post.getFeaturedVideo())) {
            return VIDEO;
        }

        // if this post is a gallery, scan its content to make sure we have enough images to
        // show in the stream's thumbnail strip
        if (post.isGallery()) {
            ReaderImageScanner scanner = new ReaderImageScanner(post.getText(), post.isPrivate);
            if (scanner.hasUsableImageCount(ReaderThumbnailStrip.IMAGE_COUNT, ReaderConstants.MIN_GALLERY_IMAGE_WIDTH)) {
                return GALLERY;
            }
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