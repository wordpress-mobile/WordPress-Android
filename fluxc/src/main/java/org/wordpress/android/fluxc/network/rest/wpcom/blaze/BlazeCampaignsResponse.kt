package org.wordpress.android.fluxc.network.rest.wpcom.blaze

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignsModel
import java.util.Date
import kotlin.time.Duration.Companion.days

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
    @SerializedName("campaign_stats") val campaignStats: CampaignStats,
    @SerializedName("target_urn") val targetUrn: String,
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
            clicks = campaignStats.clicksTotal ?: 0L,
            targetUrn = targetUrn,
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

data class BlazeCampaignListResponse(
    @SerializedName("campaigns") val campaigns: List<BlazeCampaign>,
    @SerializedName("skipped") val skipped: Int,
    @SerializedName("total_count") val totalCount_: Int,
) {
    fun toCampaignsModel() = BlazeCampaignsModel(
        campaigns = campaigns.map { it.toCampaignsModel() },
        page = page,
        totalItems = totalItems,
        totalPages = totalPages
    )
}

data class BlazeCampaign(
    @SerializedName("id") val id: String,
    @SerializedName("main_image") val image: CampaignImage = null,
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

    ) {
    fun toCampaignsModel(): BlazeCampaignModel {
        val startDate = BlazeCampaignsUtils.stringToDate(startTime)
        return BlazeCampaignModel(
            campaignId = id.toInt(),
            title = textSnippet,
            imageUrl = image.url,
            createdAt = startDate,
            endDate = Date(startDate.time + durationDays.days.inWholeMilliseconds),
            uiStatus = status,
            budgetCents = -1,
            impressions = impressions,
            clicks = clicks,
            targetUrn = targetUrn,
            totalBudget = totalBudget,
            spentBudget = spentBudget,
        )
    }
}

data class CampaignImage(
    @SerializedName("height") val height: Float,
    @SerializedName("width") val width: Float,
    @SerializedName("mime_type") val mimeType: String,
    @SerializedName("url") val url: String,
)
