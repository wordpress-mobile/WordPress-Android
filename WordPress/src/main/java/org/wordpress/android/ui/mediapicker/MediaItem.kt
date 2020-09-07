package org.wordpress.android.ui.mediapicker

import org.wordpress.android.util.UriWrapper

data class MediaItem(
    val identifier: Identifier,
    val url: String,
    val name: String? = null,
    val type: MediaType,
    val mimeType: String? = null,
    val dataModified: Long
) {
    sealed class Identifier {
        class LocalUri(val value: UriWrapper): Identifier()
        class RemoteId(val value: Long): Identifier()
    }
}
