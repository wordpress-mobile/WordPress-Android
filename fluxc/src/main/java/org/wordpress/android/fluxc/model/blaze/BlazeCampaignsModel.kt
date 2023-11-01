package org.wordpress.android.fluxc.model.blaze

import java.util.Date

data class BlazeCampaignsModel(
    val campaigns: List<BlazeCampaignModel> = emptyList(),
    val page: Int,
    val totalItems: Int,
    val totalPages: Int
)

data class BlazeCampaignModel(
    val campaignId: Int,
    val title: String,
    val imageUrl: String?,
    val createdAt: Date,
    val endDate: Date?,
    val uiStatus: String,
    val budgetCents: Long,
    val impressions: Long,
    val clicks: Long,
    val targetUrn: String,
)
