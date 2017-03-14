package org.wordpress.android.util;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

public class CrashlyticsUtils {
    final private static String TAG_KEY = "tag";
    final private static String MESSAGE_KEY = "message";

    public static void logException(Throwable tr, AppLog.T tag, String message) {
        if (!Fabric.isInitialized()) {
            return;
        }
        if (tag != null) {
            Crashlytics.setString(TAG_KEY, tag.name());
        }
        if (message != null) {
            Crashlytics.setString(MESSAGE_KEY, message);
        }
        Crashlytics.logException(tr);
    }

    public static void logException(Throwable tr, AppLog.T tag) {
        logException(tr, tag, null);
    }

    public static void logException(Throwable tr) {
        logException(tr, null, null);
    }

    // Utility functions to force us to use and reuse a limited set of keys

    public static void setInt(String key, int value) {
        if (!Fabric.isInitialized()) {
            return;
        }
        Crashlytics.setInt(key, value);
    }

    public static void setFloat(String key, float value) {
        if (!Fabric.isInitialized()) {
            return;
        }
        Crashlytics.setFloat(key, value);
    }

    public static void setString(String key, String value) {
        if (!Fabric.isInitialized()) {
            return;
        }
        Crashlytics.setString(key, value);
    }

    public static void setBool(String key, boolean value) {
        if (!Fabric.isInitialized()) {
            return;
        }
        Crashlytics.setBool(key, value);
    }
}
