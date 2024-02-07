package org.wordpress.android.fluxc.network.rest.wpcom.blaze

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeAdSuggestion
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingDevice
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingLanguage
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingLocation
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingTopic
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComNetwork
import javax.inject.Inject

class BlazeCreationRestClient @Inject constructor(
    private val wpComNetwork: WPComNetwork
) {
    suspend fun fetchTargetingLocations(
        site: SiteModel,
        query: String,
        locale: String
    ): BlazePayload<List<BlazeTargetingLocation>> {
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
            is WPComGsonRequestBuilder.Response.Success -> BlazePayload(
                response.data.locations.map { it.toDomainModel() }
            )

            is WPComGsonRequestBuilder.Response.Error -> BlazePayload(response.error)
        }
    }

    suspend fun fetchTargetingTopics(
        site: SiteModel,
        locale: String
    ): BlazePayload<List<BlazeTargetingTopic>> {
        val url = WPCOMV2.sites.site(site.siteId).wordads.dsp.api.v1_1.targeting.page_topics.url

        val response = wpComNetwork.executeGetGsonRequest(
            url = url,
            params = mapOf("locale" to locale),
            clazz = BlazeTargetingTopicListResponse::class.java
        )

        return when (response) {
            is WPComGsonRequestBuilder.Response.Success -> BlazePayload(
                response.data.topics.map { it.toDomainModel() }
            )

            is WPComGsonRequestBuilder.Response.Error -> BlazePayload(response.error)
        }
    }

    suspend fun fetchTargetingDevices(
        site: SiteModel,
        locale: String
    ): BlazePayload<List<BlazeTargetingDevice>> {
        val url = WPCOMV2.sites.site(site.siteId).wordads.dsp.api.v1_1.targeting.devices.url

        val response = wpComNetwork.executeGetGsonRequest(
            url = url,
            params = mapOf("locale" to locale),
            clazz = BlazeTargetingDeviceListResponse::class.java
        )

        return when (response) {
            is WPComGsonRequestBuilder.Response.Success -> BlazePayload(
                response.data.devices.map { it.toDomainModel() }
            )

            is WPComGsonRequestBuilder.Response.Error -> BlazePayload(response.error)
        }
    }

    suspend fun fetchTargetingLanguages(
        site: SiteModel,
        locale: String
    ): BlazePayload<List<BlazeTargetingLanguage>> {
        val url = WPCOMV2.sites.site(site.siteId).wordads.dsp.api.v1_1.targeting.languages.url

        val response = wpComNetwork.executeGetGsonRequest(
            url = url,
            params = mapOf("locale" to locale),
            clazz = BlazeTargetingLanguageListResponse::class.java
        )

        return when (response) {
            is WPComGsonRequestBuilder.Response.Success -> BlazePayload(
                response.data.languages.map { it.toDomainModel() }
            )

            is WPComGsonRequestBuilder.Response.Error -> BlazePayload(response.error)
        }
    }

    suspend fun fetchAdSuggestions(
        site: SiteModel,
        productId: Long
    ): BlazePayload<List<BlazeAdSuggestion>> {
        val url = WPCOMV2.sites.site(site.siteId).wordads.dsp.api.v1_1.suggestions.url

        val response = wpComNetwork.executePostGsonRequest(
            url = url,
            body = mapOf(
                "urn" to "urn:wpcom:post:${site.siteId}:$productId"
            ),
            clazz = BlazeAdSuggestionListResponse::class.java
        )

        return when (response) {
            is WPComGsonRequestBuilder.Response.Success -> BlazePayload(
                response.data.creatives.map { it.toDomainModel() }
            )

            is WPComGsonRequestBuilder.Response.Error -> BlazePayload(response.error)
        }
    }

    data class BlazePayload<T>(
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
        fun toDomainModel(): BlazeTargetingLocation {
            return BlazeTargetingLocation(
                id = id,
                name = name,
                type = type,
                parent = parent?.toDomainModel()
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
        fun toDomainModel(): BlazeTargetingTopic {
            return BlazeTargetingTopic(
                id = id,
                description = name
            )
        }
    }
}

private class BlazeTargetingDeviceListResponse(
    val devices: List<BlazeTargetingDeviceNetworkModel>
) {
    class BlazeTargetingDeviceNetworkModel(
        val id: String,
        val name: String
    ) {
        fun toDomainModel(): BlazeTargetingDevice {
            return BlazeTargetingDevice(
                id = id,
                name = name
            )
        }
    }
}

private class BlazeTargetingLanguageListResponse(
    val languages: List<BlazeTargetingLanguageNetworkModel>
) {
    class BlazeTargetingLanguageNetworkModel(
        val id: String,
        val name: String
    ) {
        fun toDomainModel(): BlazeTargetingLanguage {
            return BlazeTargetingLanguage(
                id = id,
                name = name
            )
        }
    }
}

private class BlazeAdSuggestionListResponse(
    val creatives: List<BlazeAdSuggestionNetworkModel>
) {
    class BlazeAdSuggestionNetworkModel(
        @SerializedName("site_name")
        val siteName: String,
        @SerializedName("text_snippet")
        val textSnippet: String,
    ) {
        fun toDomainModel(): BlazeAdSuggestion {
            return BlazeAdSuggestion(
                tagLine = siteName,
                description = textSnippet
            )
        }
    }
}
