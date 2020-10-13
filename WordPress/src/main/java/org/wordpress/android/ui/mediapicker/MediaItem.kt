package org.wordpress.android.ui.mediapicker

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.util.UriWrapper

data class MediaItem(
    val identifier: Identifier,
    val url: String,
    val name: String? = null,
    val type: MediaType,
    val mimeType: String? = null,
    val dataModified: Long
) {
    sealed class Identifier : Parcelable {
        @Parcelize
        data class LocalUri(val value: @RawValue UriWrapper) : Identifier()

        @Parcelize
        data class RemoteId(val value: Long) : Identifier()

        @Parcelize
        data class StockMediaIdentifier(val url: String?, val name: String?, val title: String?) : Identifier()

        @Parcelize
        data class GifMediaIdentifier(
            val mediaModel: MediaModel?,
            val largeImageUri: @RawValue UriWrapper,
            val title: String?
        ) : Identifier()
    }
}
