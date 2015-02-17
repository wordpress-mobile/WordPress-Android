package org.wordpress.android.ui;

public enum EmptyViewMessageType {
    LOADING, NO_CONTENT, NETWORK_ERROR, PERMISSION_ERROR, GENERIC_ERROR, NO_CONTENT_CUSTOM_DATE;

    public static EmptyViewMessageType getEnumFromString(String value) {
        for (EmptyViewMessageType id : values()) {
            if (id.name().equals(value)) {
                return id;
            }
        }
        return NO_CONTENT;
    }
}
