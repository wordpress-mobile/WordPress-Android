package org.wordpress.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.prefs.AppPrefs;

import java.util.HashMap;
import java.util.Map;

public class WPPermissionUtils {

    // permission request codes
    public static final int SHARE_MEDIA_PERMISSION_REQUEST_CODE      = 10;
    public static final int MEDIA_BROWSER_PERMISSION_REQUEST_CODE    = 20;
    public static final int MEDIA_PREVIEW_PERMISSION_REQUEST_CODE    = 30;
    public static final int PHOTO_PICKER_PERMISSION_REQUEST_CODE     = 40;
    public static final int EDITOR_LOCATION_PERMISSION_REQUEST_CODE  = 50;
    public static final int EDITOR_MEDIA_PERMISSION_REQUEST_CODE     = 60;
    public static final int EDITOR_DRAG_DROP_PERMISSION_REQUEST_CODE = 70;

    /*
     * returns true if we know the app has asked for the passed permission
     */
    private static boolean isPermissionAsked(@NonNull Context context, @NonNull String permission) {
        AppPrefs.PrefKey key = getPermissionKey(permission);
        if (key == null) {
            return false;
        }

        // if the key exists, we've already stored whether this permission has been asked for
        if (AppPrefs.keyExists(key)) {
            return AppPrefs.getBoolean(key, false);
        }

        // otherwise, check whether permission has already been granted - if so we know it has been asked
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            AppPrefs.setBoolean(key, true);
            return true;
        }

        return false;
    }

    /*
     * returns true if the passed permission has been denied AND the user checked "never show again"
     * in the native permission dialog
     */
    public static boolean isPermissionAlwaysDenied(@NonNull Activity activity, @NonNull String permission) {
        // shouldShowRequestPermissionRationale returns false if the permission has been permanently
        // denied, but it also returns false if the app has never requested that permission - so we
        // check it only if we know we've asked for this permission
        if (isPermissionAsked(activity, permission)
                && ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_DENIED) {
            boolean shouldShow = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
            return !shouldShow;
        }

        return false;
    }

    /*
     * called by the onRequestPermissionsResult() of various activities/fragments - tracks the
     * passed list of permissions and remembers that they've been asked
     */
    public static void setPermissionListAsked(int requestCode,
                                              @NonNull String permissions[],
                                              @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            AppPrefs.PrefKey key = getPermissionKey(permissions[i]);
            if (key != null) {
                boolean isFirstTime = !AppPrefs.keyExists(key);
                track(requestCode, permissions[i], grantResults[i], isFirstTime);
                AppPrefs.setBoolean(key, true);
            }
        }

    }

    private static void track(int requestCode,
                              @NonNull String permission,
                              int result,
                              boolean isFirstTime) {
        Map<String, String> props = new HashMap<>();
        props.put("permission", permission);
        props.put("request_code", Integer.toString(requestCode));
        props.put("is_first_time", Boolean.toString(isFirstTime));

        if (result == PackageManager.PERMISSION_GRANTED) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.APP_PERMISSION_GRANTED, props);
        } else if (result == PackageManager.PERMISSION_DENIED) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.APP_PERMISSION_DENIED, props);
        }
    }

    private static AppPrefs.PrefKey getPermissionKey(@NonNull String permission) {
        switch (permission) {
            case android.Manifest.permission.WRITE_EXTERNAL_STORAGE:
                return AppPrefs.UndeletablePrefKey.ASKED_PERMISSION_STORAGE_WRITE;
            case android.Manifest.permission.READ_EXTERNAL_STORAGE:
                return AppPrefs.UndeletablePrefKey.ASKED_PERMISSION_STORAGE_READ;
            case android.Manifest.permission.CAMERA:
                return AppPrefs.UndeletablePrefKey.ASKED_PERMISSION_CAMERA;
            case android.Manifest.permission.ACCESS_COARSE_LOCATION:
                return AppPrefs.UndeletablePrefKey.ASKED_PERMISSION_LOCATION_COURSE;
            case android.Manifest.permission.ACCESS_FINE_LOCATION:
                return AppPrefs.UndeletablePrefKey.ASKED_PERMISSION_LOCATION_FINE;
            default:
                AppLog.w(AppLog.T.UTILS, "No key for requested permission");
                return null;
        }
    }
}
