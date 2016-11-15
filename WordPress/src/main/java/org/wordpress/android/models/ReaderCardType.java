package org.wordpress.android.models;

public enum ReaderCardType {
    DEFAULT,
    PHOTO;

    public static ReaderCardType fromReaderPost(ReaderPost post) {
        if (post != null && post.hasFeaturedImage() && post.getText().length() < 100) {
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