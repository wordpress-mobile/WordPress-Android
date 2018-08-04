package org.wordpress.android.fluxc.model

import android.text.TextUtils

class DomainAvailabilityModel(
    val productId: Int?,
    val productSlug: String?,
    val domainName: String?,
    val status: AvailabilityStatus?,
    val mappable: Mappability?,
    val cost: String?,
    val supportsPrivacy: Boolean = false
) {
    enum class AvailabilityStatus {
        BLACKLISTED_DOMAIN,
        INVALID_TLD,
        INVALID_DOMAIN,
        TLD_NOT_SUPPORTED,
        TRANSFERRABLE_DOMAIN,
        AVAILABLE,
        UNKNOWN_STATUS;

        companion object {
            fun fromString(string: String?): AvailabilityStatus {
                if (!TextUtils.isEmpty(string)) {
                    for (v in AvailabilityStatus.values()) {
                        if (string.equals(v.name, ignoreCase = true)) {
                            return v
                        }
                    }
                }
                return UNKNOWN_STATUS
            }
        }
    }

    enum class Mappability {
        BLACKLISTED_DOMAIN,
        INVALID_TLD,
        INVALID_DOMAIN,
        MAPPABLE_DOMAIN,
        UNKNOWN_STATUS;

        companion object {
            fun fromString(string: String?): Mappability {
                if (!TextUtils.isEmpty(string)) {
                    for (v in Mappability.values()) {
                        if (string.equals(v.name, ignoreCase = true)) {
                            return v
                        }
                    }
                }
                return UNKNOWN_STATUS
            }
        }
    }
}
