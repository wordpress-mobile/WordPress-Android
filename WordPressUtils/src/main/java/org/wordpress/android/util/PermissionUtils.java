package org.wordpress.android.util;

import android.Manifest.permission;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {
    /**
     * Check for permissions, request them if they're not granted.
     *
     * @return true if permissions are already granted, else request them and return false.
     */
    private static boolean checkAndRequestPermissions(Activity activity, int requestCode, String[] permissionList) {
        List<String> toRequest = new ArrayList<>();
        for (String permission : permissionList) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                toRequest.add(permission);
            }
        }
        if (toRequest.size() > 0) {
            String[] requestedPermissions = toRequest.toArray(new String[toRequest.size()]);
            ActivityCompat.requestPermissions(activity, requestedPermissions, requestCode);
            return false;
        }
        return true;
    }

    public static boolean checkAndRequestCameraAndStoragePermissions(Activity activity, int requestCode) {
        return checkAndRequestPermissions(activity, requestCode, new String[]{
                permission.WRITE_EXTERNAL_STORAGE,
                permission.CAMERA
        });
    }

    public static boolean checkAndRequestStoragePermission(Activity activity, int requestCode) {
        return checkAndRequestPermissions(activity, requestCode, new String[]{
                permission.WRITE_EXTERNAL_STORAGE
        });
    }

    public static boolean checkLocationPermissions(Activity activity, int requestCode) {
        return checkAndRequestPermissions(activity, requestCode, new String[]{
                permission.ACCESS_FINE_LOCATION,
                permission.ACCESS_COARSE_LOCATION
        });
    }
}
