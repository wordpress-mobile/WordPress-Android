package org.wordpress.android.models;

import org.wordpress.android.util.HtmlUtils;

/**
 * Used by the reader stream view to determine which type of "card" to use
 */

public enum ReaderCardType {
    DEFAULT,
    PHOTO;

    private static final int MAX_TEXT_CHARS = 100;

    public static ReaderCardType fromReaderPost(ReaderPost post) {
        // posts with a featured image and little or no text get the photo card treatment - note
        // that we have to strip HTML tags from the text to determine its length, which can be
        // an expensive operation so we try to avoid it
        if (post != null && post.hasFeaturedImage()) {
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
        if (cardType != null && cardType == PHOTO) {
            return "PHOTO";
        }
        return "DEFAULT";
    }

    public static ReaderCardType fromString(String s) {
        if (s != null && s.equals("PHOTO")) {
            return PHOTO;
        }
        return DEFAULT;
    }
}