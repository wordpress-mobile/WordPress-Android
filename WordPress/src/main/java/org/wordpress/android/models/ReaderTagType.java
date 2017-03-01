package org.wordpress.android.models;

public enum ReaderTagType {
    FOLLOWED,
    DEFAULT,
    RECOMMENDED,
    CUSTOM_LIST,
    SEARCH;

    private static final int INT_DEFAULT     = 0;
    private static final int INT_FOLLOWED    = 1;
    private static final int INT_RECOMMENDED = 2;
    private static final int INT_CUSTOM_LIST = 3;
    private static final int INT_SEARCH      = 4;

    public static ReaderTagType fromInt(int value) {
        switch (value) {
            case INT_RECOMMENDED :
                return RECOMMENDED;
            case INT_FOLLOWED :
                return FOLLOWED;
            case INT_CUSTOM_LIST:
                return CUSTOM_LIST;
            case INT_SEARCH:
                return SEARCH;
            default :
                return DEFAULT;
        }
    }

    public int toInt() {
        switch (this) {
            case FOLLOWED:
                return INT_FOLLOWED;
            case RECOMMENDED:
                return INT_RECOMMENDED;
            case CUSTOM_LIST:
                return INT_CUSTOM_LIST;
            case SEARCH:
                return INT_SEARCH;
            default :
                return INT_DEFAULT;
        }
    }
}