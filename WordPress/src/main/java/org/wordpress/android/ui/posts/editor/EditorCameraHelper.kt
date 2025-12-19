package org.wordpress.android.ui.posts.editor

import android.app.Activity
import org.wordpress.android.util.PermissionUtils
import org.wordpress.android.util.WPPermissionUtils

/**
 * Helper object for camera-related permission handling in editor activities.
 * This centralizes the camera permission logic to avoid duplication between
 * EditPostActivity and GutenbergKitActivity.
 */
object EditorCameraHelper {
    /**
     * Checks for camera permissions and launches the camera if granted.
     * If permissions are not granted, requests them from the user.
     *
     * @param activity The activity requesting the permission
     * @param launchCamera Callback to launch the camera when permission is granted
     */
    fun checkCameraPermissionAndLaunch(activity: Activity, launchCamera: () -> Unit) {
        if (PermissionUtils.checkAndRequestCameraAndStoragePermissions(
                activity,
                WPPermissionUtils.AZTEC_EDITOR_CAMERA_PERMISSION_REQUEST_CODE
            )
        ) {
            launchCamera()
        }
    }

    /**
     * Handles the camera permission result in onRequestPermissionsResult.
     * Should be called when requestCode matches AZTEC_EDITOR_CAMERA_PERMISSION_REQUEST_CODE.
     *
     * @param requestCode The permission request code
     * @param allGranted Whether all requested permissions were granted
     * @param launchCamera Callback to launch the camera when permission is granted
     * @return true if this helper handled the request code, false otherwise
     */
    fun handlePermissionResult(
        requestCode: Int,
        allGranted: Boolean,
        launchCamera: () -> Unit
    ): Boolean {
        if (requestCode == WPPermissionUtils.AZTEC_EDITOR_CAMERA_PERMISSION_REQUEST_CODE) {
            if (allGranted) {
                launchCamera()
            }
            return true
        }
        return false
    }
}
