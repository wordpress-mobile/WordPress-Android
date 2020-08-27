package org.wordpress.android.models;

public enum ReaderTagType {
    FOLLOWED,
    DEFAULT,
    BOOKMARKED,
    CUSTOM_LIST,
    SEARCH,
    INTERESTS,
    DISCOVER_POST_CARDS;

    private static final int INT_DEFAULT = 0;
    private static final int INT_FOLLOWED = 1;
    // 2 - don't use it. It was used for recommended tags which are not used anymore
    private static final int INT_CUSTOM_LIST = 3;
    private static final int INT_SEARCH = 4;
    private static final int INT_BOOKMARKED = 5;
    private static final int INT_INTERESTS = 6;
    private static final int INT_DISCOVER_POST_CARDS = 7;


    public static ReaderTagType fromInt(int value) {
        switch (value) {
            case INT_FOLLOWED:
                return FOLLOWED;
            case INT_CUSTOM_LIST:
                return CUSTOM_LIST;
            case INT_SEARCH:
                return SEARCH;
            case INT_BOOKMARKED:
                return BOOKMARKED;
            case INT_INTERESTS:
                return INTERESTS;
            case INT_DISCOVER_POST_CARDS:
                return DISCOVER_POST_CARDS;
            default:
                return DEFAULT;
        }
    }

    public int toInt() {
        switch (this) {
            case FOLLOWED:
                return INT_FOLLOWED;
            case CUSTOM_LIST:
                return INT_CUSTOM_LIST;
            case SEARCH:
                return INT_SEARCH;
            case BOOKMARKED:
                return INT_BOOKMARKED;
            case INTERESTS:
                return INT_INTERESTS;
            case DISCOVER_POST_CARDS:
                return INT_DISCOVER_POST_CARDS;
            case DEFAULT:
            default:
                return INT_DEFAULT;
        }
    }
}
