package org.wordpress.android.ui.photopicker

import android.Manifest.permission
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import org.wordpress.android.util.PermissionUtils
import javax.inject.Inject

class PermissionsHandler
@Inject constructor(private val context: Context) {
    fun hasPermissionsToTakePhoto(): Boolean {
        return PermissionUtils.checkCameraAndStoragePermissions(context)
    }

    fun hasPhotosVideosPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            (hasReadMediaImagesPermission() && hasReadMediaVideoPermission()) ||
                    hasReadMediaVisualUserSelectedPermission()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasReadMediaImagesPermission() && hasReadMediaVideoPermission()
        } else {
            // For devices lower than API 33, storage permission is the equivalent of Photos and Videos permission
            hasReadStoragePermission()
        }
    }

    fun hasFullAccessPhotosVideosPermission(): Boolean {
        // UPSIDE_DOWN_CAKE and above the user can give partial access (READ_MEDIA_VISUAL_USER_SELECTED) but FULL access
        // follows the same rules as TIRAMISU, so no extra checks are needed
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasReadMediaImagesPermission() && hasReadMediaVideoPermission()
        } else {
            // For devices lower than API 33, storage permission is the equivalent of Photos and Videos permission
            hasReadStoragePermission()
        }
    }

    fun hasOnlyPartialAccessPhotosVideosPermission(): Boolean {
        // UPSIDE_DOWN_CAKE and above the user can give partial access (READ_MEDIA_VISUAL_USER_SELECTED) and PARTIAL
        // access rules are: does NOT have full access permissions BUT has READ_MEDIA_VISUAL_USER_SELECTED permission
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            !hasFullAccessPhotosVideosPermission() && hasReadMediaVisualUserSelectedPermission()
        } else false
    }

    fun hasMusicAudioPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasReadMediaAudioPermission()
        } else {
            // For devices lower than API 33, storage permission is the equivalent of Music and Audio permission
            hasReadStoragePermission()
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun hasReadMediaVisualUserSelectedPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, permission.READ_MEDIA_VISUAL_USER_SELECTED
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun hasReadMediaImagesPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun hasReadMediaVideoPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, permission.READ_MEDIA_VIDEO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun hasReadMediaAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, permission.READ_MEDIA_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasReadStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}
