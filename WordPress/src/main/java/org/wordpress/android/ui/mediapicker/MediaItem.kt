package org.wordpress.android.ui.mediapicker

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
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

        fun toParcel() = Parcel((this as? LocalUri)?.value?.uri, (this as? RemoteId)?.value)

        companion object {
            fun fromParcel(parcel: Parcel): Identifier {
                return when {
                    parcel.remoteId != null -> RemoteId(parcel.remoteId)
                    parcel.uri != null -> LocalUri(UriWrapper(parcel.uri))
                    else -> throw IllegalArgumentException("Parcel doesn't have URI or remote ID")
                }
            }
        }

        @Parcelize
        data class Parcel(val uri: Uri? = null, val remoteId: Long? = null) : Parcelable
    }
}
