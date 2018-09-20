package org.wordpress.android.fluxc.network.rest.wpcom.site

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.annotations.JsonAdapter
import org.wordpress.android.fluxc.model.PlanModel
import org.wordpress.android.fluxc.network.utils.getBoolean
import org.wordpress.android.fluxc.network.utils.getString
import java.lang.reflect.Type

@JsonAdapter(PlansDeserializer::class)
class PlansResponse(val plansList: List<PlanModel>)

class PlansDeserializer : JsonDeserializer<PlansResponse> {
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): PlansResponse {
        val jsonObject = json.asJsonObject
        val planModels = ArrayList<PlanModel>()
        jsonObject.entrySet().forEach { (key, value) ->
            val planJsonObj = value.asJsonObject
            val productSlug = planJsonObj.getString("product_slug")
            val productName = planJsonObj.getString("product_name")
            val productId = key.toIntOrNull()

            // 'current_plan' and 'has_domain_credit' attributes might be missing,
            // consider them as false, if they are missing
            val isCurrentPlan = planJsonObj.getBoolean("current_plan")
            val hasDomainCredit = planJsonObj.getBoolean("has_domain_credit")
            planModels.add(PlanModel(productId, productSlug, productName, isCurrentPlan, hasDomainCredit))
        }
        return PlansResponse(planModels)
    }
}
