package org.wordpress.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.crashlytics.android.Crashlytics;

import org.wordpress.android.R;

import io.fabric.sdk.android.Fabric;

public class CrashlyticsUtils {
    private static final String TAG_KEY = "tag";
    private static final String MESSAGE_KEY = "message";

    public static boolean shouldEnableCrashlytics(@NonNull Context context) {
        if (PackageUtils.isDebugBuild()) {
            return false;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean hasUserOptedOut = !prefs.getBoolean(context.getString(R.string.pref_key_send_crash), true);
        return !hasUserOptedOut;
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
