package org.wordpress.android.util;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import org.wordpress.android.WordPress;

import java.lang.reflect.Field;

import io.fabric.sdk.android.Fabric;

public class CrashlyticsUtils {
    private static final String TAG_KEY = "tag";
    private static final String MESSAGE_KEY = "message";

    /**
     * Disables Crashlytics if it's already enabled
     */
    public static void disableCrashlytics() {
        if (!Fabric.isInitialized()) {
            return;
        }

        /*
         * first we use reflection to set the Fabric singleton to null because otherwise the call
         * to Fabric.with() below will do nothing)
         */
        try {
            Class<?> clazz = Class.forName(Fabric.class.getName());
            Field field = clazz.getDeclaredField("singleton");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            AppLog.e(AppLog.T.MAIN, e.getMessage());
            return;
        }

        /*
         * then we create a new instance that disables logging - ideally this wouldn't be necessary
         * since we just nulled the existing instance above, but it seems more future-proof to
         * explicitly disable Crashlytics
         */
        CrashlyticsCore crashlytics = new CrashlyticsCore.Builder().disabled(true).build();
        Fabric.with(WordPress.getContext(), crashlytics);
    }

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

    public static void log(String message) {
        if (!Fabric.isInitialized() || message == null) {
            return;
        }
        Crashlytics.log(message);
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
