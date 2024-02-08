package org.wordpress.android.fluxc.network.rest.wpcom.blaze

import android.annotation.SuppressLint
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.delay
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeAdForecast
import org.wordpress.android.fluxc.model.blaze.BlazeAdSuggestion
import org.wordpress.android.fluxc.model.blaze.BlazePaymentMethod
import org.wordpress.android.fluxc.model.blaze.BlazePaymentMethodUrls
import org.wordpress.android.fluxc.model.blaze.BlazePaymentMethods
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingDevice
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingLanguage
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingLocation
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingParameters
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingTopic
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComNetwork
import org.wordpress.android.fluxc.utils.extensions.filterNotNull
import java.text.SimpleDateFormat
import java.util.Date
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

    @Suppress("LongParameterList")
    @SuppressLint("SimpleDateFormat")
    suspend fun fetchAdForecast(
        site: SiteModel,
        startDate: Date,
        endDate: Date,
        totalBudget: Double,
        timeZoneId: String,
        targetingParameters: BlazeTargetingParameters?
    ): BlazePayload<BlazeAdForecast> {
        val url = WPCOMV2.sites.site(site.siteId).wordads.dsp.api.v1_1.forecast.url
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd")

        val response = wpComNetwork.executePostGsonRequest(
            url = url,
            body = mutableMapOf(
                "start_date" to dateFormatter.format(startDate),
                "end_date" to dateFormatter.format(endDate),
                "time_zone" to timeZoneId,
                "total_budget" to totalBudget.toString(),
                "targeting" to targetingParameters?.let {
                    mapOf(
                        "locations" to targetingParameters.locations?.map { it.id },
                        "languages" to targetingParameters.languages?.map { it.id },
                        "devices" to targetingParameters.devices?.map { it.id },
                        "page_topics" to targetingParameters.topics?.map { it.id }
                    ).filterNotNull()
                }
            ).filterNotNull(),
            clazz = BlazeAdForecastNetworkModel::class.java
        )

        return when (response) {
            is WPComGsonRequestBuilder.Response.Success -> BlazePayload(response.data.toDomainModel())

            is WPComGsonRequestBuilder.Response.Error -> BlazePayload(response.error)
        }
    }

    @Suppress("KotlinConstantConditions", "MagicNumber", "UNUSED_PARAMETER")
    suspend fun fetchPaymentMethods(site: SiteModel): BlazePayload<BlazePaymentMethods> {
        // TODO Use real API when it becomes ready

        fun generateFakePaymentMethods() = BlazePaymentMethodsResponse(
            savedPaymentMethods = listOf(
                BlazePaymentMethodsResponse.BlazePaymentMethodsNetworkModel(
                    id = "payment-method-id",
                    type = "credit_card",
                    name = "Visa **** 4689",
                    info = JsonObject().apply {
                        addProperty("last_digits", "4689")
                        add("expiring", JsonObject().apply {
                            addProperty("month", 2)
                            addProperty("year", 2025)
                        })
                        addProperty("type", "Visa")
                        addProperty("nickname", "")
                        addProperty("cardholder_name", "John Doe")
                    }
                ),
                BlazePaymentMethodsResponse.BlazePaymentMethodsNetworkModel(
                    id = "payment-method-id-2",
                    type = "credit_card",
                    name = "MasterCard **** 1234",
                    info = JsonObject().apply {
                        addProperty("last_digits", "1234")
                        add("expiring", JsonObject().apply {
                            addProperty("month", 3)
                            addProperty("year", 2026)
                        })
                        addProperty("type", "MasterCard")
                        addProperty("nickname", "")
                        addProperty("cardholder_name", "John Doe")
                    }
                )
            ),
            addPaymentMethodUrls = BlazePaymentMethodsResponse.BlazeAddPaymentMethodUrlsNetworkModel(
                formUrl = "https://example.com/blaze-pm-add",
                successUrl = "https://example.com/blaze-pm-success",
                idUrlParameter = "pmid"
            )
        )

        delay(500)
        val response: WPComGsonRequestBuilder.Response<BlazePaymentMethodsResponse> =
            WPComGsonRequestBuilder.Response.Success(generateFakePaymentMethods())

        return when (response) {
            is WPComGsonRequestBuilder.Response.Success -> BlazePayload(response.data.toDomainModel())

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

private class BlazeAdForecastNetworkModel(
    @SerializedName("total_impressions_min") val minImpressions: Int,
    @SerializedName("total_impressions_max") val maxImpressions: Int,
) {
    fun toDomainModel(): BlazeAdForecast = BlazeAdForecast(
        minImpressions = minImpressions,
        maxImpressions = maxImpressions
    )
}

private class BlazePaymentMethodsResponse(
    @SerializedName("saved_payment_methods")
    val savedPaymentMethods: List<BlazePaymentMethodsNetworkModel>,
    @SerializedName("add_payment_method")
    val addPaymentMethodUrls: BlazeAddPaymentMethodUrlsNetworkModel
) {
    fun toDomainModel(): BlazePaymentMethods {
        return BlazePaymentMethods(
            savedPaymentMethods = savedPaymentMethods.map { it.toDomainModel() },
            addPaymentMethodUrls = addPaymentMethodUrls.toDomainModel()
        )
    }

    class BlazePaymentMethodsNetworkModel(
        val id: String,
        val type: String,
        val name: String,
        val info: JsonObject
    ) {
        fun toDomainModel(): BlazePaymentMethod {
            return BlazePaymentMethod(
                id = id,
                type = when (type) {
                    "credit_card" -> BlazePaymentMethod.PaymentMethodType.CREDIT_CARD
                    else -> BlazePaymentMethod.PaymentMethodType.UNKNOWN
                },
                name = name,
                info = when (type) {
                    "credit_card" -> BlazePaymentMethod.PaymentMethodInfo.CreditCardInfo(
                        lastDigits = info.get("last_digits").asString,
                        expMonth = info.get("expiring").asJsonObject.get("month").asInt,
                        expYear = info.get("expiring").asJsonObject.get("year").asInt,
                        type = info.get("type").asString,
                        nickname = info.get("nickname").asString,
                        cardHolderName = info.get("cardholder_name").asString
                    )

                    else -> BlazePaymentMethod.PaymentMethodInfo.Unknown
                }
            )
        }
    }

    class BlazeAddPaymentMethodUrlsNetworkModel(
        @SerializedName("form_url")
        val formUrl: String,
        @SerializedName("success_url")
        val successUrl: String,
        @SerializedName("id_url_parameter")
        val idUrlParameter: String
    ) {
        fun toDomainModel(): BlazePaymentMethodUrls {
            return BlazePaymentMethodUrls(
                formUrl = formUrl,
                successUrl = successUrl,
                idUrlParameter = idUrlParameter
            )
        }
    }
}
