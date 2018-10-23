package org.wordpress.android.fluxc.model.revisions

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class Diff(val operation: DiffOperations, val value: String?) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other == null || other !is Diff) {
            return false
        }

        return operation == other.operation && value == other.value
    }

    override fun hashCode(): Int {
        var result = operation.hashCode()
        result = 31 * result + (value?.hashCode() ?: 0)
        return result
    }
}
