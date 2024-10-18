package org.wordpress.android.fluxc.model.products

import com.google.gson.annotations.SerializedName

data class Product(
    @SerializedName("available")
    val available: Boolean? = false,
    @SerializedName("cost")
    val cost: Double? = 0.0,
    @SerializedName("sale_cost")
    val saleCost: Double? = 0.0,
    @SerializedName("combined_sale_cost_display")
    val combinedSaleCostDisplay: String? = "",
    @SerializedName("cost_display")
    val costDisplay: String? = "",
    @SerializedName("currency_code")
    val currencyCode: String? = "",
    @SerializedName("description")
    val description: String? = "",
    @SerializedName("is_domain_registration")
    val isDomainRegistration: Boolean? = false,
    @SerializedName("price_tier_list")
    val priceTierList: List<Any>? = listOf(),
    @SerializedName("price_tier_slug")
    val priceTierSlug: String? = "",
    @SerializedName("price_tier_usage_quantity")
    val priceTierUsageQuantity: Any? = Any(),
    @SerializedName("price_tiers")
    val priceTiers: List<Any>? = listOf(),
    @SerializedName("product_id")
    val productId: Int? = 0,
    @SerializedName("product_name")
    val productName: String? = "",
    @SerializedName("product_slug")
    val productSlug: String? = "",
    @SerializedName("product_type")
    val productType: String? = ""
)
