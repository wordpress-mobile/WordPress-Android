package org.wordpress.android.ui.mediapicker

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
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
    sealed class Identifier {
        data class LocalUri(val value: UriWrapper) : Identifier()
        data class RemoteId(val value: Long) : Identifier()
        data class StockMediaIdentifier(val url: String?, val name: String?, val title: String?) : Identifier()
        data class GifMediaIdentifier(
            val remoteId: String?,
            val mediaModel: MediaModel?,
            val thumbnailUri: Uri?,
            val previewImageUri: Uri?,
            val largeImageUri: Uri?,
            val title: String?
        ) : Identifier()

        fun toParcel() = Parcel(
                (this as? LocalUri)?.value?.uri,
                (this as? RemoteId)?.value,
                (this as? StockMediaIdentifier)?.url,
                (this as? StockMediaIdentifier)?.name,
                (this as? StockMediaIdentifier)?.title,
                (this as? GifMediaIdentifier)?.remoteId,
                (this as? GifMediaIdentifier)?.mediaModel,
                (this as? GifMediaIdentifier)?.thumbnailUri,
                (this as? GifMediaIdentifier)?.previewImageUri,
                (this as? GifMediaIdentifier)?.largeImageUri,
                (this as? GifMediaIdentifier)?.title
        )

        companion object {
            fun fromParcel(parcel: Parcel): Identifier {
                return when {
                    parcel.remoteId != null -> RemoteId(parcel.remoteId)
                    parcel.uri != null -> LocalUri(UriWrapper(parcel.uri))
                    parcel.url != null -> StockMediaIdentifier(parcel.url, parcel.name, parcel.title)
                    parcel.gifRemoteId != null -> GifMediaIdentifier(
                            parcel.gifRemoteId,
                            parcel.gifMediaModel,
                            parcel.gifThumbnailUri,
                            parcel.gifPreviewImageUri,
                            parcel.gifLargeImageUri,
                            parcel.gifTitle
                    )
                    else -> throw IllegalArgumentException("Parcel doesn't have URI or remote ID")
                }
            }
        }

        @Parcelize
        data class Parcel(
            val uri: Uri? = null,
            val remoteId: Long? = null,
            val url: String? = null,
            val name: String? = null,
            val title: String? = null,
            val gifRemoteId: String? = null,
            val gifMediaModel: MediaModel? = null,
            val gifThumbnailUri: Uri? = null,
            val gifPreviewImageUri: Uri? = null,
            val gifLargeImageUri: Uri? = null,
            val gifTitle: String? = null
        ) : Parcelable
    }
}
