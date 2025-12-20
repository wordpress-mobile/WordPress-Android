package org.wordpress.android.ui.posts

import android.app.Activity
import dagger.Reusable
import org.wordpress.android.BuildConfig
import org.wordpress.android.util.PermissionUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.WPPermissionUtils
import javax.inject.Inject

/**
 * Helper class that handles camera permission checks and launching the camera.
 * Extracted from EditPostActivity and GutenbergKitActivity to reduce code duplication.
 */
@Reusable
class EditorCameraHelper @Inject constructor() {
    /**
     * Callback interface for camera operations.
     */
    interface CameraCallback {
        fun onMediaCapturePathReady(mediaCapturePath: String?)
    }

    /**
     * Launches the camera for capturing photos.
     *
     * @param activity The activity context
     * @param callback Callback to receive the media capture path
     */
    fun launchCamera(activity: Activity, callback: CameraCallback) {
        WPMediaUtils.launchCamera(
            activity,
            BuildConfig.APPLICATION_ID,
            object : WPMediaUtils.LaunchCameraCallback {
                override fun onMediaCapturePathReady(mediaCapturePath: String?) {
                    callback.onMediaCapturePathReady(mediaCapturePath)
                }

                override fun onCameraError(errorMessage: String?) {
                    ToastUtils.showToast(
                        activity,
                        errorMessage,
                        ToastUtils.Duration.SHORT
                    )
                }
            }
        )
    }

    /**
     * Checks for camera permissions and launches the camera if granted.
     *
     * @param activity The activity context
     * @param callback Callback to receive the media capture path
     * @return true if permissions were already granted and camera was launched, false if permission
     *         request was initiated
     */
    fun checkCameraPermissionAndLaunch(activity: Activity, callback: CameraCallback): Boolean {
        return if (PermissionUtils.checkAndRequestCameraAndStoragePermissions(
                activity,
                WPPermissionUtils.AZTEC_EDITOR_CAMERA_PERMISSION_REQUEST_CODE
            )
        ) {
            launchCamera(activity, callback)
            true
        } else {
            false
        }
    }

    /**
     * Launches the video camera for capturing videos.
     *
     * @param activity The activity context
     */
    fun launchVideoCamera(activity: Activity) {
        WPMediaUtils.launchVideoCamera(activity)
    }
}
