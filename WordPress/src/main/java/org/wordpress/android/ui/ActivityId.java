package org.wordpress.android.ui;

import android.content.Context;

import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

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

    public static void trackLastActivity(ActivityId activityId) {
        AppLog.v(T.UTILS, "trackLastActivity, activityId: " + activityId);
        if (activityId != null) {
            AppPrefs.setLastActivityStr(activityId.name());
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
