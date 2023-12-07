package org.wordpress.android.editor.savedinstance

import android.os.Parcel
import android.os.Parcelable

data class TestParcelable(val data: String) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readString()!!)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(data)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TestParcelable> {
        override fun createFromParcel(parcel: Parcel): TestParcelable {
            return TestParcelable(parcel)
        }

        override fun newArray(size: Int): Array<TestParcelable?> {
            return arrayOfNulls(size)
        }
    }
}
