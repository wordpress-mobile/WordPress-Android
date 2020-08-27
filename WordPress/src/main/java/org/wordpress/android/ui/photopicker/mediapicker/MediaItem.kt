package org.wordpress.android.ui.photopicker.mediapicker

import org.wordpress.android.util.UriWrapper

data class MediaItem(
    val id: Long = 0,
    val uri: UriWrapper? = null,
    val isVideo: Boolean = false
)
