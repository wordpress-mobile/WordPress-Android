package org.wordpress.android.models;

public enum ReaderPostCardType {
    DEFAULT,
    PHOTO;

    public static ReaderPostCardType fromReaderPost(ReaderPost post) {
        if (post != null && post.hasFeaturedImage() && post.getText().length() < 100) {
            return PHOTO;
        }
        return DEFAULT;
    }

    public static String toString(ReaderPostCardType cardType) {
        if (cardType != null && cardType == PHOTO) {
            return "PHOTO";
        }
        return "DEFAULT";
    }

    public static ReaderPostCardType fromString(String s) {
        if (s != null && s.equals("PHOTO")) {
            return PHOTO;
        }
        return DEFAULT;
    }
}