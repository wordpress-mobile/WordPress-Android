package org.wordpress.android.fluxc.network.rest.wpcom.site

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.annotations.JsonAdapter
import org.wordpress.android.fluxc.model.PlanModel
import java.lang.reflect.Type

@JsonAdapter(PlansDeserializer::class)
class PlansResponse (val plansList: List<PlanModel>)


class PlansDeserializer : JsonDeserializer<PlansResponse> {
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): PlansResponse {
        val jsonObject = json.asJsonObject
        val planModels = ArrayList<PlanModel>()
        val entries = jsonObject.entrySet()
        for ((_, value) in entries) {
            val planJsonObj = value.asJsonObject
            val productSlug = planJsonObj.get("product_slug").asString
            val productName = planJsonObj.get("product_name").asString

            // 'id' attribute is sometimes null, even though the it should be a long value
            // Check if it is JsonObject (i.e. not null and not JsonNull), before casting it to long
            val idJsonObj = planJsonObj.get("id")
            val productId = (idJsonObj as? JsonObject)?.asLong ?: 0

            // 'current_plan' and 'has_domain_credit' attributes might be missing,
            // consider them as false, if they are missing
            val isCurrentPlan =
                    planJsonObj.has("current_plan") && planJsonObj.get("current_plan").asBoolean
            val hasDomainCredit =
                    planJsonObj.has("has_domain_credit") && planJsonObj.get("has_domain_credit").asBoolean
            planModels.add(PlanModel(productId, productSlug, productName, isCurrentPlan, hasDomainCredit))
        }
        return PlansResponse(planModels)
    }
}
