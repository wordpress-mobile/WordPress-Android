package org.wordpress.android.fluxc.network.rest.wpcom.blaze

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingLocation
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingTopic
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComNetwork
import javax.inject.Inject

class BlazeTargetingRestClient @Inject constructor(
    private val wpComNetwork: WPComNetwork
) {
    suspend fun fetchBlazeLocations(
        site: SiteModel,
        query: String,
        locale: String
    ): BlazeTargetingPayload<List<BlazeTargetingLocation>> {
        val url = WPCOMV2.sites.site(site.siteId).wordads.dsp.api.v1_1.targeting.locations.url

        val response = wpComNetwork.executeGetGsonRequest(
            url = url,
            params = mapOf(
                "query" to query,
                "locale" to locale
            ),
            clazz = BlazeTargetingLocationListResponse::class.java
        )

        return when (response) {
            is WPComGsonRequestBuilder.Response.Success -> BlazeTargetingPayload(
                response.data.locations
                    .map { it.toBlazeTargetingLocation() }
            )

            is WPComGsonRequestBuilder.Response.Error -> BlazeTargetingPayload(response.error)
        }
    }

    suspend fun fetchBlazeTopics(
        site: SiteModel,
        locale: String
    ): BlazeTargetingPayload<List<BlazeTargetingTopic>> {
        val url = WPCOMV2.sites.site(site.siteId).wordads.dsp.api.v1_1.targeting.page_topics.url

        val response = wpComNetwork.executeGetGsonRequest(
            url = url,
            params = mapOf("locale" to locale),
            clazz = BlazeTargetingTopicListResponse::class.java
        )

        return when (response) {
            is WPComGsonRequestBuilder.Response.Success -> BlazeTargetingPayload(
                response.data.topics
                    .map { it.toBlazeTargetingTopic() }
            )

            is WPComGsonRequestBuilder.Response.Error -> BlazeTargetingPayload(response.error)
        }
    }

    data class BlazeTargetingPayload<T>(
        val data: T?
    ) : Payload<WPComGsonNetworkError>() {
        constructor(error: WPComGsonNetworkError) : this(null) {
            this.error = error
        }
    }
}

private class BlazeTargetingLocationListResponse(
    val locations: List<BlazeTargetingLocationNetworkModel>
) {
    class BlazeTargetingLocationNetworkModel(
        val id: Long,
        val name: String,
        val type: String,
        @SerializedName("parent_location")
        val parent: BlazeTargetingLocationNetworkModel?
    ) {
        fun toBlazeTargetingLocation(): BlazeTargetingLocation {
            return BlazeTargetingLocation(
                id = id,
                name = name,
                type = type,
                parent = parent?.toBlazeTargetingLocation()
            )
        }
    }
}

private class BlazeTargetingTopicListResponse(
    @SerializedName("page_topics")
    val topics: List<BlazeTargetingTopicNetworkModel>
) {
    class BlazeTargetingTopicNetworkModel(
        val id: String,
        val name: String
    ) {
        fun toBlazeTargetingTopic(): BlazeTargetingTopic {
            return BlazeTargetingTopic(
                id = id,
                description = name
            )
        }
    }
}
