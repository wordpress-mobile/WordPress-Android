package org.wordpress.android.util;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

public class CrashlyticsUtils {
    final private static String EXCEPTION_KEY = "exception";
    final private static String TAG_KEY = "tag";
    final private static String MESSAGE_KEY = "message";
    public enum ExceptionType {USUAL, SPECIFIC}
    public enum ExtraKey {IMAGE_ANGLE, IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_RESIZE_SCALE, NOTE_HTMLDATA, ENTERED_URL}

    public static void logException(Throwable tr, ExceptionType exceptionType, AppLog.T tag, String message) {
        if (!Fabric.isInitialized()) {
            return;
        }
        if (tag != null) {
            Crashlytics.setString(TAG_KEY, tag.name());
        }
        if (message != null) {
            Crashlytics.setString(MESSAGE_KEY, message);
        }
        Crashlytics.setString(EXCEPTION_KEY, exceptionType.name());
        Crashlytics.logException(tr);
    }

    public static void logException(Throwable tr, ExceptionType exceptionType, AppLog.T tag) {
        logException(tr, exceptionType, tag, null);
    }

    public static void logException(Throwable tr, ExceptionType exceptionType) {
        logException(tr, exceptionType, null, null);
    }

    // Utility functions to force us to use and reuse a limited set of keys

    public static void setInt(ExtraKey key, int value) {
        Crashlytics.setInt(key.name(), value);
    }

    public static void setFloat(ExtraKey key, float value) {
        Crashlytics.setFloat(key.name(), value);
    }

    public static void setString(ExtraKey key, String value) {
        Crashlytics.setString(key.name(), value);
    }

    public static void setBool(ExtraKey key, boolean value) {
        Crashlytics.setBool(key.name(), value);
    }
}
