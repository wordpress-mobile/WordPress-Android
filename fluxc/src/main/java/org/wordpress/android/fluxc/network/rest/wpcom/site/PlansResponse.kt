package org.wordpress.android.fluxc.network.rest.wpcom.site

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

@JsonAdapter(PlansDeserializer::class)
class PlansResponse {
    var plansMap: Map<Long, Plan>? = emptyMap()

    inner class Plan {
        var id: String? = null
        var interval: Long = 0
        @SerializedName("formatted_original_price")
        var formattedOriginalPrice: String? = null
        @SerializedName("raw_price")
        var rawPrice: Long = 0
        @SerializedName("formatted_price")
        var formattedPrice: String? = null
        @SerializedName("raw_discount")
        var rawDiscount: Long = 0
        @SerializedName("formatted_discount")
        var formattedDiscount: String? = null
        @SerializedName("product_slug")
        var productSlug: String? = null
        @SerializedName("product_name")
        var productName: String? = null
        @SerializedName("discount_reason")
        var discountReason: String? = null
        @SerializedName("is_domain_upgrade")
        var isDomainUpgrade: String? = null
        @SerializedName("currency_code")
        var currencyCode: String? = null
        @SerializedName("user_is_owner")
        var userIsOwner: String? = null
        @SerializedName("current_plan")
        var currentPlan: Boolean = false
        @SerializedName("has_domain_credit")
        var hasDomainCredit: Boolean = false
    }
}
internal class PlansDeserializer : JsonDeserializer<PlansResponse> {
    @Throws(JsonParseException::class)
    override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext): PlansResponse {
        val response = PlansResponse()
        val tokenType = object : TypeToken<Map<Long, PlansResponse.Plan>>() {}.type
        response.plansMap = Gson().fromJson<Map<Long, PlansResponse.Plan>>(json, tokenType)
        return response
    }
}
