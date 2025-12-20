package org.wordpress.android.ui.posts

import android.app.Activity
import dagger.Reusable
import org.wordpress.android.BuildConfig
import org.wordpress.android.fluxc.model.SiteModel
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

    /**
     * Handles the camera permission result. Call this from onRequestPermissionsResult.
     *
     * @param requestCode The permission request code
     * @param allGranted Whether all requested permissions were granted
     * @param onLaunchCamera Callback to launch the camera if permission was granted
     * @return true if this was a camera permission request and it was handled, false otherwise
     */
    fun handleCameraPermissionResult(
        requestCode: Int,
        allGranted: Boolean,
        onLaunchCamera: () -> Unit
    ): Boolean {
        return if (requestCode == WPPermissionUtils.AZTEC_EDITOR_CAMERA_PERMISSION_REQUEST_CODE && allGranted) {
            onLaunchCamera()
            true
        } else {
            false
        }
    }

    /**
     * Checks if the user can upload media and launches the photo camera if allowed.
     *
     * @param site The site to check upload permissions for
     * @param onLaunchCamera Callback to launch the camera if upload is allowed
     * @param onNoPermission Callback when user doesn't have upload permission
     * @return true if user can upload and camera launch was initiated
     */
    fun capturePhotoIfAllowed(
        site: SiteModel,
        onLaunchCamera: () -> Unit,
        onNoPermission: () -> Unit
    ): Boolean {
        return if (WPMediaUtils.currentUserCanUploadMedia(site)) {
            onLaunchCamera()
            true
        } else {
            onNoPermission()
            false
        }
    }

    /**
     * Checks if the user can upload media and launches the video camera if allowed.
     *
     * @param activity The activity context
     * @param site The site to check upload permissions for
     * @param onNoPermission Callback when user doesn't have upload permission
     * @return true if user can upload and video camera was launched
     */
    fun captureVideoIfAllowed(
        activity: Activity,
        site: SiteModel,
        onNoPermission: () -> Unit
    ): Boolean {
        return if (WPMediaUtils.currentUserCanUploadMedia(site)) {
            launchVideoCamera(activity)
            true
        } else {
            onNoPermission()
            false
        }
    }
}
