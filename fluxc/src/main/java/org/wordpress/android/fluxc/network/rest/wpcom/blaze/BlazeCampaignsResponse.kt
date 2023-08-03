package org.wordpress.android.fluxc.network.rest.wpcom.blaze

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignsModel

data class AudienceList(
    @SerializedName("OSs") val oSS: String?,
    val countries: String? = null,
    val devices: String? = null,
    val languages: String? = null,
    val topics: String? = null,
)

data class ContentConfig(
    val clickUrl: String? = null,
    val imageUrl: String? = null,
    val snippet: String? = null,
    val title: String,
)

data class CampaignStats(
    @SerializedName("impressions_total") val impressionsTotal: Long? = null,
    @SerializedName("clicks_total") val clicksTotal: Long? = null
)

data class Campaign(
    @SerializedName("alt_text") val altText: String? = null,
    @SerializedName("audience_list") val audienceList: AudienceList? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("budget_cents") val budgetCents: Long? = null,
    @SerializedName("campaign_id") val campaignId: Int? = null,
    @SerializedName("content_config") val contentConfig: ContentConfig,
    @SerializedName("content_image") val contentImage: String? = null,
    @SerializedName("content_target_iab_category") val contentTargetIabCategory: String? = null,
    @SerializedName("content_target_language") val contentTargetLanguage: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("creative_asset_id") val creativeAssetId: Int? = null,
    @SerializedName("creative_html") val creativeHtml: String? = null,
    @SerializedName("delivery_percent") val deliveryPercent: Int? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("device_target_type") val deviceTargetType: String? = null,
    @SerializedName("display_delivery_estimate") val displayDeliveryEstimate: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("file_name") val fileName: String? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("image_mime_type") val imageMimeType: String? = null,
    @SerializedName("keyword_target_ids") val keywordTargetIds: String? = null,
    @SerializedName("keyword_target_kvs") val keywordTargetKvs: String? = null,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("moderation_reason") val moderationReason: String? = null,
    @SerializedName("moderation_status") val moderationStatus: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("os_target_type") val osTargetType: String? = null,
    @SerializedName("owner_id") val ownerId: Int? = null,
    @SerializedName("page_names") val pageNames: String? = null,
    @SerializedName("placement") val placement: String? = null,
    @SerializedName("revenue") val revenue: String? = null,
    @SerializedName("site_names") val siteNames: String? = null,
    @SerializedName("smart_delivery_estimate") val smartDeliveryEstimate: String? = null,
    @SerializedName("smart_id") val smartId: String? = null,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("status_smart") val statusSmart: Int? = null,
    @SerializedName("subscription_id") val subscriptionId: Int? = null,
    @SerializedName("target_url") val targetUrl: String? = null,
    @SerializedName("target_urn") val targetUrn: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("ui_status") val uiStatus: String,
    @SerializedName("user_target_geo") val userTargetGeo: String? = null,
    @SerializedName("user_target_geo2") val userTargetGeo2: String? = null,
    @SerializedName("user_target_language") val userTargetLanguage: String? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("campaign_stats") val campaignStats: CampaignStats
) {
    fun toCampaignsModel(): BlazeCampaignModel {
        return BlazeCampaignModel(
            campaignId = campaignId ?: 0,
            title = contentConfig.title,
            imageUrl = contentConfig.imageUrl,
            createdAt = BlazeCampaignsUtils.stringToDate(createdAt),
            endDate = endDate?.let { BlazeCampaignsUtils.stringToDate(it) },
            uiStatus = uiStatus,
            budgetCents = budgetCents ?: 0,
            impressions = campaignStats.impressionsTotal ?: 0L,
            clicks = campaignStats.clicksTotal ?: 0L
        )
    }
}

data class BlazeCampaignsResponse(
    @SerializedName("campaigns") val campaigns: List<Campaign>,
    @SerializedName("page") val page: Int,
    @SerializedName("total_items") val totalItems: Int,
    @SerializedName("total_pages") val totalPages: Int
) {
    fun toCampaignsModel() = BlazeCampaignsModel(
        campaigns = campaigns.map { it.toCampaignsModel() },
        page = page,
        totalItems = totalItems,
        totalPages = totalPages
    )
}
