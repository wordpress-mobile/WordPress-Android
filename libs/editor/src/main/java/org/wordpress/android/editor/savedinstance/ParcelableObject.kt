package org.wordpress.android.editor.savedinstance

import android.os.Parcel
import android.os.Parcelable

class ParcelableObject {
    private val parcel = Parcel.obtain()

    constructor(parcelable: Parcelable) {
        parcelable.writeToParcel(parcel, 0)
    }

    constructor(data: ByteArray) {
        parcel.unmarshall(data, 0, data.size)
        parcel.setDataPosition(0)
    }

    fun toBytes(): ByteArray {
        return parcel.marshall()
    }

    fun getParcel(): Parcel {
        parcel.setDataPosition(0)
        return parcel
    }

    fun recycle() {
        parcel.recycle()
    }
}
