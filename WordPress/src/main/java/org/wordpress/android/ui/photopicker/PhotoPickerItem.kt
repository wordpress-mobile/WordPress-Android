package org.wordpress.android.ui.photopicker

import org.wordpress.android.util.UriWrapper

@Deprecated("This class is being refactored, if you implement any change, please also update " +
        "{@link org.wordpress.android.ui.photopicker.mediapicker.MediaItem}")
data class PhotoPickerItem(
    val id: Long = 0,
    val uri: UriWrapper? = null,
    val isVideo: Boolean = false
)
