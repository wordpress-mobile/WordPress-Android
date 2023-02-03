package org.wordpress.android.fluxc.network.rest.wpcom.site

data class DomainPriceResponse(
    val is_premium: Boolean = false,
    val product_id: Int?,
    val product_slug: String?,
    val cost: String?,
    val raw_price: Double?,
    val currency_code: String?
)
