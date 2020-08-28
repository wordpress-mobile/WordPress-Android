package org.wordpress.android.ui.mediapicker

import org.wordpress.android.util.UriWrapper

data class MediaItem(
    val uri: UriWrapper,
    val name: String? = null,
    val type: MediaType,
    val mimeType: String? = null,
    val dataModified: Long
)
