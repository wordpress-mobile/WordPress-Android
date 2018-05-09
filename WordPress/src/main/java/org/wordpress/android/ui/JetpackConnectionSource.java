package org.wordpress.android.ui;

import android.support.annotation.Nullable;

public enum JetpackConnectionSource {
    NOTIFICATIONS("notifications"),
    STATS("stats");

    private final String mValue;

    JetpackConnectionSource(String value) {
        mValue = value;
    }

    @Nullable
    public static JetpackConnectionSource fromString(String value) {
        if (NOTIFICATIONS.mValue.equals(value)) {
            return NOTIFICATIONS;
        } else if (STATS.mValue.equals(value)) {
            return STATS;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return mValue;
    }
}
