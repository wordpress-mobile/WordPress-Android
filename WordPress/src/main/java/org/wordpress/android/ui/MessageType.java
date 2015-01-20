package org.wordpress.android.ui;

public enum MessageType {
    LOADING, NO_CONTENT, NETWORK_ERROR, PERMISSION_ERROR, GENERIC_ERROR, NO_CONTENT_CUSTOM_DATE;

    public static MessageType getEnumFromString(String value) {
        for (MessageType id : values()) {
            if (id.name().equals(value)) {
                return id;
            }
        }
        return NO_CONTENT;
    }
}
