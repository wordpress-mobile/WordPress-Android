package org.wordpress.android.ui.mediapicker

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import androidx.core.os.ParcelCompat
import org.wordpress.android.ui.mediapicker.MediaItem.IdentifierType.GIF_MEDIA_IDENTIFIER
import org.wordpress.android.ui.mediapicker.MediaItem.IdentifierType.LOCAL_ID
import org.wordpress.android.ui.mediapicker.MediaItem.IdentifierType.LOCAL_URI
import org.wordpress.android.ui.mediapicker.MediaItem.IdentifierType.REMOTE_ID
import org.wordpress.android.ui.mediapicker.MediaItem.IdentifierType.STOCK_MEDIA_IDENTIFIER
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
        LOCAL_URI,
        REMOTE_ID,
        LOCAL_ID,
        STOCK_MEDIA_IDENTIFIER,
        GIF_MEDIA_IDENTIFIER
    }

    sealed class Identifier(val type: IdentifierType) : Parcelable {
        data class LocalUri(val value: UriWrapper, val queued: Boolean = false) : Identifier(LOCAL_URI)

        data class RemoteId(val value: Long) : Identifier(REMOTE_ID)

        data class LocalId(val value: Int) : Identifier(LOCAL_ID)

        data class StockMediaIdentifier(
            val url: String?,
            val name: String?,
            val title: String?
        ) : Identifier(STOCK_MEDIA_IDENTIFIER)

        data class GifMediaIdentifier(
            val largeImageUri: UriWrapper,
            val title: String?
        ) : Identifier(GIF_MEDIA_IDENTIFIER)

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(this.type.name)
            when (this) {
                is LocalUri -> {
                    parcel.writeParcelable(this.value.uri, flags)
                    parcel.writeInt(if (this.queued) 1 else 0)
                }
                is RemoteId -> {
                    parcel.writeLong(this.value)
                }
                is LocalId -> {
                    parcel.writeInt(this.value)
                }
                is StockMediaIdentifier -> {
                    parcel.writeString(this.url)
                    parcel.writeString(this.name)
                    parcel.writeString(this.title)
                }
                is GifMediaIdentifier -> {
                    parcel.writeParcelable(this.largeImageUri.uri, flags)
                    parcel.writeString(this.title)
                }
            }
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object {
            @JvmField
            val CREATOR: Creator<Identifier> = object : Creator<Identifier> {
                override fun createFromParcel(parcel: Parcel): Identifier {
                    val type = IdentifierType.valueOf(requireNotNull(parcel.readString()))
                    return when (type) {
                        LOCAL_URI -> {
                            LocalUri(
                                UriWrapper(
                                    requireNotNull(
                                        ParcelCompat.readParcelable(
                                            parcel,
                                            Uri::class.java.classLoader,
                                            Uri::class.java
                                        )
                                    )
                                ),
                                parcel.readInt() != 0
                            )
                        }
                        REMOTE_ID -> {
                            RemoteId(parcel.readLong())
                        }
                        LOCAL_ID -> {
                            LocalId(parcel.readInt())
                        }
                        STOCK_MEDIA_IDENTIFIER -> {
                            StockMediaIdentifier(parcel.readString(), parcel.readString(), parcel.readString())
                        }
                        GIF_MEDIA_IDENTIFIER -> {
                            GifMediaIdentifier(
                                UriWrapper(
                                    requireNotNull(
                                        ParcelCompat.readParcelable(
                                            parcel,
                                            Uri::class.java.classLoader,
                                            Uri::class.java
                                        )
                                    )
                                ),
                                parcel.readString()
                            )
                        }
                    }
                }

                override fun newArray(size: Int): Array<Identifier?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}
