package org.wordpress.android.ui.photopicker

import android.net.Uri

data class PhotoPickerItem(
    val id: Long = 0,
    val uri: Uri? = null,
    val isVideo: Boolean = false
)