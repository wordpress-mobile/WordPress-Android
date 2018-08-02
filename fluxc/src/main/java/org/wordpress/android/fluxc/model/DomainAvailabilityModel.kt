package org.wordpress.android.fluxc.model

import android.support.annotation.StringDef

const val BLACKLISTED_DOMAIN = "blacklisted_domain"
const val INVALID_TLD = "invalid_tld"
const val INVALID_DOMAIN = "invalid_domain"
const val TLD_NOT_SUPPORTED = "tld_not_supported"
const val TRANSFERRABLE_DOMAIN = "transferrable"
const val AVAILABLE = "available"
const val MAPPABLE_DOMAIN = "mappable"

@StringDef(BLACKLISTED_DOMAIN, INVALID_TLD, INVALID_DOMAIN, TLD_NOT_SUPPORTED, TRANSFERRABLE_DOMAIN, AVAILABLE)
annotation class AvailabilityStatus

@StringDef(BLACKLISTED_DOMAIN, INVALID_TLD, INVALID_DOMAIN, MAPPABLE_DOMAIN)
annotation class Mappability

class DomainAvailabilityModel(
    val productId: Int?,
    val productSlug: String?,
    val domainName: String?,
    @AvailabilityStatus val status: String?,
    @Mappability val mappable: String?,
    val cost: String?,
    val supportsPrivacy: Boolean = false
)
