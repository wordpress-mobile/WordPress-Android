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
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignCreationRequest
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
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
import kotlin.time.Duration.Companion.days

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
                        "locations" to targetingParameters.locations,
                        "languages" to targetingParameters.languages,
                        "devices" to targetingParameters.devices,
                        "page_topics" to targetingParameters.topics
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

    @Suppress("UNREACHABLE_CODE")
    @SuppressLint("SimpleDateFormat")
    suspend fun createCampaign(
        site: SiteModel,
        request: BlazeCampaignCreationRequest
    ): BlazePayload<BlazeCampaignModel> {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd")

        delay(500)
        return BlazePayload(BlazeCampaignCreationNetworkResponse(
            id = "campaign-0",
            status = "pending",
            targetUrn = "urn:wpcom:post:${site.siteId}:${request.targetResourceId}",
            startTime = dateFormatter.format(request.startDate),
            durationDays = (request.endDate.time - request.startDate.time) / 1.days.inWholeMilliseconds,
            totalBudget = request.budget,
            siteName = request.tagLine,
            textSnippet = request.description,
            targetURL = request.targetUrl,
            mainImage = BlazeCampaignCreationNetworkResponse.BlazeImageNetworkModel(
                url = request.mainImage.url
            )
        ).toDomainModel())

        // TODO Use real API when it becomes ready
        val body = mutableMapOf(
            "origin" to request.origin,
            "origin_version" to request.originVersion,
            "target_urn" to "urn:wpcom:post:${site.siteId}:${request.targetResourceId}",
            "type" to request.type.value,
            "payment_method_id" to request.paymentMethodId,
            "start_date" to dateFormatter.format(request.startDate),
            "end_date" to dateFormatter.format(request.endDate),
            "time_zone" to request.timeZoneId,
            "total_budget" to request.budget,
            "site_name" to request.tagLine,
            "text_snippet" to request.description,
            "target_url" to request.targetUrl,
            "url_params" to request.urlParams.entries.joinToString(separator = "&") { "${it.key}=${it.value}" },
            "main_image" to JsonObject().apply {
                addProperty("url", request.mainImage.url)
                addProperty("mime_type", request.mainImage.mimeType)
            },
            "targeting" to request.targetingParameters?.let {
                mapOf(
                    "locations" to it.locations,
                    "languages" to it.languages,
                    "devices" to it.devices,
                    "page_topics" to it.topics
                ).filterNotNull()
            }
        ).filterNotNull()

        val response = wpComNetwork.executePostGsonRequest(
            url = WPCOMV2.sites.site(site.siteId).wordads.dsp.api.v1_1.campaigns.url,
            body = body,
            clazz = BlazeCampaignCreationNetworkResponse::class.java
        )

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

private data class BlazeCampaignCreationNetworkResponse(
    val id: String,
    val status: String,
    @SerializedName("target_urn")
    val targetUrn: String,
    @SerializedName("start_time")
    val startTime: String,
    @SerializedName("duration_days")
    val durationDays: Long,
    @SerializedName("total_budget")
    val totalBudget: Double,
    @SerializedName("site_name")
    val siteName: String,
    @SerializedName("text_snippet")
    val textSnippet: String,
    @SerializedName("target_url")
    val targetURL: String,
    @SerializedName("main_image")
    val mainImage: BlazeImageNetworkModel
) {
    data class BlazeImageNetworkModel(
        val url: String
    )

    fun toDomainModel(): BlazeCampaignModel = BlazeCampaignModel(
        targetUrn = targetUrn,
        createdAt = Date(), // Set to current date, as the API does not return the actual creation date
        endDate = Date(BlazeCampaignsUtils.stringToDate(startTime).time + durationDays.days.inWholeMilliseconds),
        imageUrl = mainImage.url,
        budgetCents = (totalBudget * 100).toLong(),
        uiStatus = status,
        // TODO revisit this when the API returns the actual values to confirm the format of IDs
        campaignId = id.substringAfter("campaign-").toIntOrNull() ?: 0,
        clicks = 0L,
        impressions = 0L,
        title = siteName
    )
}

