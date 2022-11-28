package org.wordpress.android.fluxc.model.plans.full

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
data class Plan(
    @SerializedName("product_id") var productId: Int? = null,
    @SerializedName("product_name") var productName: String? = null,
    @SerializedName("bd_slug") var bdSlug: String? = null,
    @SerializedName("bd_variation_slug") var bdVariationSlug: String? = null,
    @SerializedName("sale_coupon_applied") var saleCouponApplied: Boolean = false,
    @SerializedName("sale_coupon") var saleCoupon: SaleCoupon? = null,
    @SerializedName("multi") var multi: Int? = null,
    @SerializedName("cost") var cost: BigDecimal? = null,
    @SerializedName("orig_cost") var originalCost: String? = null,
    @SerializedName("is_cost_from_introductory_offer") var isCostFromIntroductoryOffer: Boolean = false,
    @SerializedName("product_slug") var productSlug: String? = null,
    @SerializedName("description") var description: String? = null,
    @SerializedName("bill_period") var billPeriod: Int? = null,
    @SerializedName("product_type") var productType: String? = null,
    @SerializedName("available") var available: String? = null,
    @SerializedName("outer_slug") var outerSlug: String? = null,
    @SerializedName("capability") var capability: String? = null,
    @SerializedName("product_name_short") var productNameShort: String? = null,
    @SerializedName("icon") var iconUrl: String? = null,
    @SerializedName("icon_active") var iconActiveUrl: String? = null,
    @SerializedName("bill_period_label") var billPeriodLabel: String? = null,
    @SerializedName("price") var price: String? = null,
    @SerializedName("formatted_price") var formattedPrice: String? = null,
    @SerializedName("raw_price") var rawPrice: BigDecimal? = null,
    @SerializedName("product_display_price") var productDisplayPrice: String? = null,
    @SerializedName("tagline") var tagline: String? = null,
    @SerializedName("currency_code") var currencyCode: String? = null
) : Parcelable {
    @Parcelize
    data class SaleCoupon(
        @SerializedName("expires") var expires: String? = null,
        @SerializedName("code") var code: String? = null,
        @SerializedName("discount") var discount: BigDecimal? = null,
        @SerializedName("single_use") var isSingleUse: Boolean? = null,
        @SerializedName("start_date") var startDate: String? = null,
        @SerializedName("created_by") var createdBy: String? = null,
        @SerializedName("note") var note: String? = null
    ) : Parcelable
}
