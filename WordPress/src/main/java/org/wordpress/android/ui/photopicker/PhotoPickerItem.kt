package org.wordpress.android.ui.photopicker

import org.wordpress.android.util.UriWrapper

data class PhotoPickerItem(
    val id: Long = 0,
    val uri: UriWrapper? = null,
    val isVideo: Boolean = false
)
