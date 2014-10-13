package org.wordpress.android.ui;

import android.content.Context;

import org.wordpress.android.ui.prefs.AppPrefs;

public enum ActivityId {
    UNKNOWN("Unknown"),
    READER("Reader"),
    NOTIFICATIONS("Notifications"),
    POSTS("Post List"),
    MEDIA("Media Library"),
    PAGES("Page List"),
    COMMENTS("Comments"),
    THEMES("Themes"),
    STATS("Stats"),
    VIEW_SITE("View Site"),
    POST_EDITOR("Post Editor"),
    LOGIN("Login Screen");

    private final String mStringValue;

    private ActivityId(final String stringValue) {
        mStringValue = stringValue;
    }

    public String toString() {
        return mStringValue;
    }

    public static void trackLastActivity(Context context, ActivityId activityId) {
        if (activityId != null) {
            AppPrefs.setLastActivityStr(activityId.name());
        }
    }

    /**
     * Map special cases of activities that can't be restored
     */
    public ActivityId autoRestoreMapper() {
        switch (this) {
            // Login screen can't be restored
            case LOGIN:
                return UNKNOWN;

            // In case the post editor was selected, restore the post list instead.
            case POST_EDITOR:
                return POSTS;

            // All other screen can be restored
            default:
                return this;
        }
    }

    public static ActivityId getActivityIdFromName(String activityString) {
        if (activityString == null) {
            return ActivityId.UNKNOWN;
        }
        try {
            return ActivityId.valueOf(activityString);
        } catch (IllegalArgumentException e) {
            // default to UNKNOWN in case the activityString is bogus
            return ActivityId.UNKNOWN;
        }
    }
}
