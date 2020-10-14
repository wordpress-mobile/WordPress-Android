package org.wordpress.android.ui.mediapicker

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.ui.mediapicker.MediaItem.IdentifierType.GifMediaIdentifier
import org.wordpress.android.ui.mediapicker.MediaItem.IdentifierType.LocalUri
import org.wordpress.android.ui.mediapicker.MediaItem.IdentifierType.RemoteId
import org.wordpress.android.ui.mediapicker.MediaItem.IdentifierType.StockMediaIdentifier
import org.wordpress.android.util.UriWrapper

data class MediaItem(
    val identifier: Identifier,
    val url: String,
    val name: String? = null,
    val type: MediaType,
    val mimeType: String? = null,
    val dataModified: Long
) {
    enum class IdentifierType {
        LocalUri,
        RemoteId,
        StockMediaIdentifier,
        GifMediaIdentifier
    }

    sealed class Identifier(val type: IdentifierType) : Parcelable {
        data class LocalUri(val value: UriWrapper) : Identifier(LocalUri)

        data class RemoteId(val value: Long) : Identifier(RemoteId)

        data class StockMediaIdentifier(val url: String?, val name: String?, val title: String?) : Identifier(StockMediaIdentifier)

        data class GifMediaIdentifier(
            val mediaModel: MediaModel?,
            val largeImageUri: UriWrapper,
            val title: String?
        ) : Identifier(GifMediaIdentifier)

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(this.type.name)
            when (this) {
                is LocalUri -> {
                    parcel.writeParcelable(this.value.uri, flags)
                }
                is RemoteId -> {
                    parcel.writeLong(this.value)
                }
                is StockMediaIdentifier -> {
                    parcel.writeString(this.url)
                    parcel.writeString(this.name)
                    parcel.writeString(this.title)
                }
                is GifMediaIdentifier -> {
                    parcel.writeSerializable(this.mediaModel)
                    parcel.writeParcelable(this.largeImageUri.uri, flags)
                    parcel.writeString(this.title)
                }
            }
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Creator<Identifier> {
            override fun createFromParcel(parcel: Parcel): Identifier {
                val type = IdentifierType.valueOf(requireNotNull(parcel.readString()))
                return when(type) {
                    LocalUri -> {
                        LocalUri(UriWrapper(requireNotNull(parcel.readParcelable(Uri::class.java.classLoader))))
                    }
                    RemoteId -> {
                        RemoteId(parcel.readLong())
                    }
                    StockMediaIdentifier -> {
                        StockMediaIdentifier(parcel.readString(), parcel.readString(), parcel.readString())
                    }
                    GifMediaIdentifier -> {
                        val model = parcel.readSerializable()
                        GifMediaIdentifier(
                                model?.let { it as MediaModel },
                                UriWrapper(requireNotNull(parcel.readParcelable(Uri::class.java.classLoader))),
                                parcel.readString())
                    }
                }
            }

            override fun newArray(size: Int): Array<Identifier?> {
                return arrayOfNulls(size)
            }
        }
    }
}
