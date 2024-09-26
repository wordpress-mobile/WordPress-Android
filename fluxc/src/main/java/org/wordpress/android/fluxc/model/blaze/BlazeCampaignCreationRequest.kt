package org.wordpress.android.fluxc.model.blaze

import org.wordpress.android.fluxc.model.MediaModel
import java.util.Date
import java.util.TimeZone

data class BlazeCampaignCreationRequest(
    val origin: String,
    val originVersion: String,
    val targetResourceId: Long,
    val type: BlazeCampaignType,
    val paymentMethodId: String,
    val tagLine: String,
    val description: String,
    val startDate: Date,
    val endDate: Date,
    val budget: BlazeCampaignCreationRequestBudget,
    val targetUrl: String,
    val urlParams: Map<String, String>,
    val mainImage: MediaModel,
    val targetingParameters: BlazeTargetingParameters?,
    val timeZoneId: String = TimeZone.getDefault().id,
    val isEndlessCampaign: Boolean
)

data class BlazeCampaignCreationRequestBudget(
    val mode: String,
    val amount: Double,
    val currency: String
)
