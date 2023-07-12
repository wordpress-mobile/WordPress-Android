package org.wordpress.android.fluxc.model.blaze

import java.util.Date

data class BlazeCampaignsModel(
    val campaigns: List<BlazeCampaignModel> = emptyList(),
    val page: Int,
    val totalItems: Int,
    val totalPages: Int
)

data class BlazeCampaignModel(
    val campaignId: Long,
    val title: String,
    val imageUrl: String?,
    val startDate: Date,
    val endDate: Date?,
    val uiStatus: String,
    val budgetCents: Long,
    val impressions: Long,
    val clicks: Long
)
