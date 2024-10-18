package org.wordpress.android.fluxc.network.rest.wpcom.blaze

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignsModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.store.Store.OnChangedError

data class BlazeCampaignsFetchedPayload<T>(
    val response: T? = null
) : Payload<BlazeCampaignsError>() {
    constructor(error: BlazeCampaignsError) : this() {
        this.error = error
    }
}

class BlazeCampaignsError
@JvmOverloads constructor(
    val type: BlazeCampaignsErrorType,
    val message: String? = null
) : OnChangedError

enum class BlazeCampaignsErrorType {
    GENERIC_ERROR,
    AUTHORIZATION_REQUIRED,
    INVALID_RESPONSE,
    API_ERROR,
    TIMEOUT
}

fun WPComGsonNetworkError.toBlazeCampaignsError(): BlazeCampaignsError {
    val type = when (type) {
        GenericErrorType.TIMEOUT -> BlazeCampaignsErrorType.TIMEOUT
        GenericErrorType.NO_CONNECTION,
        GenericErrorType.SERVER_ERROR,
        GenericErrorType.INVALID_SSL_CERTIFICATE,
        GenericErrorType.NETWORK_ERROR -> BlazeCampaignsErrorType.API_ERROR

        GenericErrorType.PARSE_ERROR,
        GenericErrorType.NOT_FOUND,
        GenericErrorType.CENSORED,
        GenericErrorType.INVALID_RESPONSE -> BlazeCampaignsErrorType.INVALID_RESPONSE

        GenericErrorType.HTTP_AUTH_ERROR,
        GenericErrorType.AUTHORIZATION_REQUIRED,
        GenericErrorType.NOT_AUTHENTICATED -> BlazeCampaignsErrorType.AUTHORIZATION_REQUIRED

        GenericErrorType.UNKNOWN,
        null -> BlazeCampaignsErrorType.GENERIC_ERROR
    }
    return BlazeCampaignsError(type, message)
}

data class CampaignStats(
    @SerializedName("impressions_total") val impressionsTotal: Long? = null,
    @SerializedName("clicks_total") val clicksTotal: Long? = null
)

data class BlazeCampaignListResponse(
    @SerializedName("campaigns") val campaigns: List<BlazeCampaign>,
    @SerializedName("skipped") val skipped: Int,
    @SerializedName("total_count") val totalCount: Int,
) {
    fun toCampaignsModel() = BlazeCampaignsModel(
        campaigns = campaigns.map { it.toCampaignsModel() },
        skipped = skipped,
        totalItems = totalCount,
    )
}

data class BlazeCampaign(
    @SerializedName("id") val id: String,
    @SerializedName("main_image") val image: CampaignImage,
    @SerializedName("target_url") val targetUrl: String,
    @SerializedName("text_snippet") val textSnippet: String,
    @SerializedName("site_name") val siteName: String,
    @SerializedName("clicks") val clicks: Long,
    @SerializedName("impressions") val impressions: Long,
    @SerializedName("spent_budget") val spentBudget: Double,
    @SerializedName("total_budget") val totalBudget: Double,
    @SerializedName("duration_days") val durationDays: Int,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("target_urn") val targetUrn: String,
    @SerializedName("status") val status: String,
    @SerializedName("is_evergreen") val isEvergreen: Boolean, // If the campaign duration is unlimited
) {
    fun toCampaignsModel(): BlazeCampaignModel {
        val startDate = BlazeCampaignsUtils.stringToDate(startTime)
        return BlazeCampaignModel(
            campaignId = id,
            title = siteName,
            imageUrl = image.url,
            startTime = startDate,
            durationInDays = durationDays,
            uiStatus = status,
            impressions = impressions,
            clicks = clicks,
            targetUrn = targetUrn,
            totalBudget = totalBudget,
            spentBudget = spentBudget,
            isEndlessCampaign = isEvergreen
        )
    }
}

data class CampaignImage(
    @SerializedName("height") val height: Float,
    @SerializedName("width") val width: Float,
    @SerializedName("mime_type") val mimeType: String,
    @SerializedName("url") val url: String,
)
