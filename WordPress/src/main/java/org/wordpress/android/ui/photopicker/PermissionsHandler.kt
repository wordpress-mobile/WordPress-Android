package org.wordpress.android.ui.photopicker

import android.Manifest.permission
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import javax.inject.Inject

class PermissionsHandler
@Inject constructor(private val context: Context) {
    fun hasPermissionsToAccessPhotos(): Boolean {
        return hasCameraPermission() && hasStoragePermission()
    }

    fun hasStoragePermission(): Boolean {
        return hasReadStoragePermission() && hasWriteStoragePermission()
    }

    fun hasWriteStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasReadStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}
