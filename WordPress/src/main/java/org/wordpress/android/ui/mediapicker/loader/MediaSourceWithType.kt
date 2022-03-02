package org.wordpress.android.ui.mediapicker.loader

import org.wordpress.android.ui.mediapicker.MediaType

interface MediaSourceWithTypes {
    val mediaTypes: Set<MediaType>
}
