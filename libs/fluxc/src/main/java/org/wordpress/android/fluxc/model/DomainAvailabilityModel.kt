package org.wordpress.android.fluxc.model

class DomainAvailabilityModel(
    val productId: Int?,
    val productSlug: String?,
    val domainName: String,
    val status: AvailabilityStatus,
    val mappable: Mappability,
    val cost: String?,
    val supportsPrivacy: Boolean = false
) {
    enum class AvailabilityStatus {
        BLACKLISTED_DOMAIN,
        INVALID_TLD,
        INVALID_DOMAIN,
        TLD_NOT_SUPPORTED,
        TRANSFERRABLE_DOMAIN,
        AVAILABLE
    }

    enum class Mappability {
        BLACKLISTED_DOMAIN,
        INVALID_TLD,
        INVALID_DOMAIN,
        MAPPABLE_DOMAIN
    }
}
