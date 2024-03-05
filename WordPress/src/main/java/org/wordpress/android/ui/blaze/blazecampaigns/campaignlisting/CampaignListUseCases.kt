package org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting

import org.wordpress.android.Result
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import javax.inject.Inject

class FetchCampaignListUseCase @Inject constructor(
    private val store: BlazeCampaignsStore,
    private val mapper: CampaignListingUIModelMapper
) {
    companion object {
        const val PAGE_SIZE = 10
    }

    @Suppress("ReturnCount")
    suspend fun execute(
        site: SiteModel,
        offset: Int,
        pageSize: Int = PAGE_SIZE
    ): Result<NetworkResult, FetchedCampaignsResult> {
        val result = store.fetchBlazeCampaigns(site = site, offset = offset, perPage = pageSize)
        if (result.isError || result.model == null) return Result.Failure(GenericResult)
        val campaigns = result.model!!.campaigns
        if (campaigns.isEmpty()) return Result.Failure(NoCampaigns)
        return Result.Success(
            FetchedCampaignsResult(
                campaigns = mapper.mapToCampaignModels(campaigns),
                totalItems = result.model!!.totalItems
            )
        )
    }
}

class GetCampaignListFromDbUseCase @Inject constructor(
    private val store: BlazeCampaignsStore,
    private val mapper: CampaignListingUIModelMapper
) {
    suspend fun execute(site: SiteModel): Result<NoCampaigns, List<CampaignModel>> {
        val campaigns = store.getBlazeCampaigns(site)
        if (campaigns.isEmpty()) return Result.Failure(NoCampaigns)
        return Result.Success(mapper.mapToCampaignModels(campaigns))
    }
}

data class FetchedCampaignsResult(
    val campaigns: List<CampaignModel>,
    val totalItems: Int
)

sealed interface NetworkResult

object GenericResult : NetworkResult

object NoCampaigns : NetworkResult
