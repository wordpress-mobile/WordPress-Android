package org.wordpress.android.util;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {
    /**
     * Check for permissions, request them if they're not granted.
     *
     * @return true if permissions are already granted, else request them and return false.
     */
    public static boolean checkAndRequestPermissions(Activity activity, int requestCode, String[] permissionList) {
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

    /**
     * Check for permissions, request them if they're not granted.
     *
     * @return true if permissions are already granted, else request them and return false.
     */
    private static boolean checkAndRequestPermissions(Fragment fragment, int requestCode, String[] permissionList) {
        List<String> toRequest = new ArrayList<>();
        for (String permission : permissionList) {
            Context context = fragment.getActivity();
            if (context != null && ContextCompat.checkSelfPermission(context, permission) != PackageManager
                    .PERMISSION_GRANTED) {
                toRequest.add(permission);
            }
        }
        if (toRequest.size() > 0) {
            String[] requestedPermissions = toRequest.toArray(new String[toRequest.size()]);
            fragment.requestPermissions(requestedPermissions, requestCode);
            return false;
        }
        return true;
    }

    /**
     * Check for permissions without requesting them
     *
     * @return true if all permissions are granted
     */
    public static boolean checkPermissions(Activity activity, String[] permissionList) {
        for (String permission : permissionList) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkCameraAndStoragePermissions(Activity activity) {
        return checkPermissions(activity,
                                new String[]{
                                        permission.WRITE_EXTERNAL_STORAGE,
                                        permission.CAMERA});
    }

    public static boolean checkAndRequestCameraAndStoragePermissions(Fragment fragment, int requestCode) {
        return checkAndRequestPermissions(fragment, requestCode, new String[]{
                permission.WRITE_EXTERNAL_STORAGE,
                permission.CAMERA
        });
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

    public static boolean checkAndRequestStoragePermission(Fragment fragment, int requestCode) {
        return checkAndRequestPermissions(fragment, requestCode, new String[]{
                permission.WRITE_EXTERNAL_STORAGE
        });
    }
}
