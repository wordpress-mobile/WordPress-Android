package org.wordpress.android.fluxc.network.rest.wpcom.planoffers

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.plans.PlanOfferModel
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.planoffers.PlanOffersRestClient.PlanOffersResponse.Feature
import org.wordpress.android.fluxc.network.rest.wpcom.planoffers.PlanOffersRestClient.PlanOffersResponse.Plan
import org.wordpress.android.fluxc.store.PlanOffersStore.PlanOffersFetchedPayload
import javax.inject.Singleton

@Singleton
class PlanOffersRestClient
constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchPlanOffers(): PlanOffersFetchedPayload {
        val url = WPCOMV2.plans.mobile.url

        val params = mapOf<String, String>()
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                PlanOffersResponse::class.java,
                enableCaching = true,
                forced = false
        )
        return when (response) {
            is Success -> {
                val plans = response.data.plans
                val features = response.data.features
                buildPlanOffersPayload(plans, features)
            }
            is WPComGsonRequestBuilder.Response.Error -> {
                val payload = PlanOffersFetchedPayload()
                payload.error = response.error
                payload
            }
        }
    }

    private fun buildPlanOffersPayload(
        planResponses: List<Plan>?,
        featureResponses: List<Feature>?
    ): PlanOffersFetchedPayload {
        return PlanOffersFetchedPayload(planResponses?.map { plan ->
            val featureDetails = featureResponses?.filter { feature -> plan.features!!.contains(feature.id) }!!
                    .map { filteredFeature ->
                        PlanOfferModel.Feature(filteredFeature.id, filteredFeature.name, filteredFeature.description)
                    }

            PlanOfferModel(
                    plan.products?.map { product -> product.plan_id },
                    featureDetails,
                    plan.name,
                    plan.short_name,
                    plan.tagline,
                    plan.description,
                    plan.icon
            )
        })
    }

    data class PlanOffersResponse(
        val groups: List<Group>?,
        val plans: List<Plan>?,
        val features: List<Feature>?
    ) : Response {
        data class Group(
            val slug: String?,
            val name: String
        )

        data class PlanId(val plan_id: Int)

        data class Feature(
            val id: String?,
            val name: String?,
            val description: String?
        )

        data class Plan(
            val groups: List<String>?,
            val products: List<PlanId>?,
            val features: List<String>?,
            val name: String?,
            val short_name: String?,
            val tagline: String?,
            val description: String?,
            val icon: String?
        )
    }
}
