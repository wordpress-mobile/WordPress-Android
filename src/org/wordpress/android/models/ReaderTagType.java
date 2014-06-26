package org.wordpress.android.models;

public enum ReaderTagType {
    FOLLOWED,
    DEFAULT,
    RECOMMENDED;

    private static final int INT_DEFAULT = 0;
    private static final int INT_FOLLOWED = 1;
    private static final int INT_RECOMMENDED = 2;

    public static ReaderTagType fromInt(int value) {
        switch (value) {
            case INT_RECOMMENDED :
                return RECOMMENDED;
            case INT_FOLLOWED :
                return FOLLOWED;
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
            default :
                return INT_DEFAULT;
        }
    }
}