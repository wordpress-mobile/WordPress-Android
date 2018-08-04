package org.wordpress.android.fluxc.network.rest.wpcom.site

class DomainAvailabilityResponse(
    val product_id: Int?,
    val product_slug: String?,
    val domain_name: String?,
    val status: String?,
    val mappable: String?,
    val cost: String?,
    val supports_privacy: Boolean = false
)
