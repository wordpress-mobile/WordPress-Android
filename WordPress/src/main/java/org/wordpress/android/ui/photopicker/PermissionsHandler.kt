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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasReadMediaImagesPermission() && hasReadMediaVideoPermission()
        } else {
            // For devices lower than API 33, storage permission is the equivalent of Photos and Videos permission
            hasReadStoragePermission()
        }
    }

    fun hasMusicAudioPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasReadMediaAudioPermission()
        } else {
            // For devices lower than API 33, storage permission is the equivalent of Music and Audio permission
            hasReadStoragePermission()
        }
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
