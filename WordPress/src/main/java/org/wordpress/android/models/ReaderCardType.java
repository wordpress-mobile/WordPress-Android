package org.wordpress.android.models;

/**
 * Used by the reader stream view to determine which type of "card" to use
 */

public enum ReaderCardType {
    DEFAULT,
    PHOTO;

    public static ReaderCardType fromReaderPost(ReaderPost post) {
        // photo cards have a suitable featured image and little or no text
        if (post != null && post.hasFeaturedImage() && post.getExcerpt().length() < 100) {
            return PHOTO;
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