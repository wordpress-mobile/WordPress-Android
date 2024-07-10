package org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting

import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.ui.mysite.cards.blaze.CampaignStatus
import org.wordpress.android.ui.stats.refresh.utils.ONE_THOUSAND
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.utils.UiString
import javax.inject.Inject
import kotlin.math.roundToInt

class CampaignListingUIModelMapper @Inject constructor(
    private val statsUtils: StatsUtils
){
    fun mapToCampaignModels(list: List<BlazeCampaignModel>) : List<CampaignModel> {
        return list.map { mapToCampaignModel(it) }
    }

    private fun mapToCampaignModel(campaignModel:BlazeCampaignModel): CampaignModel {
        return CampaignModel(
            id = campaignModel.campaignId.toString(),
            title = UiString.UiStringText(campaignModel.title),
            status = CampaignStatus.fromString(campaignModel.uiStatus),
            featureImageUrl = campaignModel.imageUrl,
            impressions = mapToStatsStringIfNeeded(campaignModel.impressions),
            clicks = mapToStatsStringIfNeeded(campaignModel.clicks),
            budget = UiString.UiStringText("$${campaignModel.totalBudget.roundToInt()}")
        )
    }

    private fun mapToStatsStringIfNeeded(value: Long): UiString? {
        return if (value != 0L) {
            val formattedString = statsUtils.toFormattedString(value, ONE_THOUSAND)
            UiString.UiStringText(formattedString)
        } else {
            null
        }
    }
}
