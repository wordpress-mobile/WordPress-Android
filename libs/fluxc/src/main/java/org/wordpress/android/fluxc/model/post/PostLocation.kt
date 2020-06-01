package org.wordpress.android.fluxc.model.post

import java.io.Serializable

data class PostLocation(
    val latitude: Double = INVALID_LATITUDE,
    val longitude: Double = INVALID_LONGITUDE
) : Serializable {
    val isValid: Boolean
        get() = isValidLatitude(latitude) && isValidLongitude(longitude)

    private fun isValidLatitude(latitude: Double): Boolean {
        return latitude in MIN_LATITUDE..MAX_LATITUDE
    }

    private fun isValidLongitude(longitude: Double): Boolean {
        return longitude in MIN_LONGITUDE..MAX_LONGITUDE
    }

    companion object {
        private const val serialVersionUID: Long = 771468329640601473L
        const val INVALID_LATITUDE = 9999.0
        const val INVALID_LONGITUDE = 9999.0
        private const val MIN_LATITUDE = -90.0
        private const val MAX_LATITUDE = 90.0
        private const val MIN_LONGITUDE = -180.0
        private const val MAX_LONGITUDE = 180.0

        fun equals(a: Any?, b: Any?): Boolean {
            return if (a === b) {
                true
            } else if (a == null || b == null) {
                false
            } else {
                a == b
            }
        }
    }
}
