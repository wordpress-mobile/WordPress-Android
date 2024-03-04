package org.wordpress.android.fluxc.model.blaze

import java.util.Date

data class BlazeCampaignsModel(
    val campaigns: List<BlazeCampaignModel> = emptyList(),
    val skipped: Int,
    val totalItems: Int,
)

data class BlazeCampaignModel(
    val campaignId: String,
    val title: String,
    val imageUrl: String?,
    val createdAt: Date,
    val endDate: Date?,
    val uiStatus: String,
    val impressions: Long,
    val clicks: Long,
    val targetUrn: String?,
    val totalBudget: Double,
    val spentBudget: Double,
)
