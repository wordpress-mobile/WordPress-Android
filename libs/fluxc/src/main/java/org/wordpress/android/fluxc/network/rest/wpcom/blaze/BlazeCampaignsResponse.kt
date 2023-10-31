package org.wordpress.android.fluxc.network.rest.wpcom.blaze

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignsModel

data class ContentConfig(
    val imageUrl: String? = null,
    val title: String,
)

data class CampaignStats(
    @SerializedName("impressions_total") val impressionsTotal: Long? = null,
    @SerializedName("clicks_total") val clicksTotal: Long? = null
)

data class Campaign(
    @SerializedName("budget_cents") val budgetCents: Long? = null,
    @SerializedName("campaign_id") val campaignId: Int? = null,
    @SerializedName("content_config") val contentConfig: ContentConfig,
    @SerializedName("content_image") val contentImage: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("ui_status") val uiStatus: String,
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
