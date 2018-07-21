package org.wordpress.android.fluxc.network.rest.wpcom.site

import com.google.gson.annotations.JsonAdapter
import org.wordpress.android.fluxc.model.PlanModel

@JsonAdapter(PlansDeserializer::class)
class PlansResponse {
    var plansList: List<PlanModel>? = emptyList()
}
