package org.wordpress.android.fluxc.model.plans.full

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
data class Plan(
    @SerializedName("product_id") val productId: Int? = null,
    @SerializedName("product_name") val productName: String? = null,
    @SerializedName("bd_slug") val bdSlug: String? = null,
    @SerializedName("bd_variation_slug") val bdVariationSlug: String? = null,
    @SerializedName("sale_coupon_applied") val saleCouponApplied: Boolean = false,
    @SerializedName("sale_coupon") val saleCoupon: SaleCoupon? = null,
    @SerializedName("multi") val multi: Int? = null,
    @SerializedName("cost") val cost: BigDecimal? = null,
    @SerializedName("orig_cost") val originalCost: String? = null,
    @SerializedName("is_cost_from_introductory_offer") val isCostFromIntroductoryOffer: Boolean = false,
    @SerializedName("product_slug") val productSlug: String? = null,
    @SerializedName("path_slug") val pathSlug: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("bill_period") val billPeriod: Int? = null,
    @SerializedName("product_type") val productType: String? = null,
    @SerializedName("available") val available: String? = null,
    @SerializedName("outer_slug") val outerSlug: String? = null,
    @SerializedName("capability") val capability: String? = null,
    @SerializedName("product_name_short") val productShortName: String? = null,
    @SerializedName("icon") val iconUrl: String? = null,
    @SerializedName("icon_active") val iconActiveUrl: String? = null,
    @SerializedName("bill_period_label") val billPeriodLabel: String? = null,
    @SerializedName("price") val price: String? = null,
    @SerializedName("formatted_price") val formattedPrice: String? = null,
    @SerializedName("raw_price") val rawPrice: BigDecimal? = null,
    @SerializedName("product_display_price") val productDisplayPrice: String? = null,
    @SerializedName("tagline") val tagline: String? = null,
    @SerializedName("currency_code") val currencyCode: String? = null
) : Parcelable {
    @Parcelize
    data class SaleCoupon(
        @SerializedName("expires") val expires: String? = null,
        @SerializedName("code") val code: String? = null,
        @SerializedName("discount") val discount: BigDecimal? = null,
        @SerializedName("single_use") val isSingleUse: Boolean? = null,
        @SerializedName("start_date") val startDate: String? = null,
        @SerializedName("created_by") val createdBy: String? = null,
        @SerializedName("note") val note: String? = null
    ) : Parcelable
}
