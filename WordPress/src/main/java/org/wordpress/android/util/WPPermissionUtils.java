package org.wordpress.android.util;

import android.support.annotation.NonNull;

import org.wordpress.android.ui.prefs.AppPrefs;

public class WPPermissionUtils {

    /*
     * returns true if the app has ever asked for the passed permission
     */
    public static boolean isPermissionAsked(@NonNull String permission, boolean isAsked) {
        AppPrefs.PrefKey key = getPermissionKey(permission);
        return key != null ? AppPrefs.getBoolean(key, false) : false;
    }

    /*
     * remember that the passed permissions has been asked
     */
    public static void setPermissionAsked(@NonNull String permission) {
        AppPrefs.PrefKey key = getPermissionKey(permission);
        if (key != null) {
            AppPrefs.setBoolean(key, true);
        }
    }

    /*
     * remember that the list of permissions has been asked
     */
    public static void setPermissionListAsked(@NonNull String[] permissions) {
        for (int i = 0; i < permissions.length; i++) {
            setPermissionAsked(permissions[i]);
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
                return null;
        }
    }
}
