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
    ): Result<NetworkError, List<CampaignModel>> {
        val result = store.fetchBlazeCampaigns(site = site, offset = offset, perPage = pageSize)
        if (result.isError || result.model == null) return Result.Failure(GenericError)
        val campaigns = result.model!!.campaigns
        if (campaigns.isEmpty()) return Result.Failure(NoCampaigns)
        return Result.Success(mapper.mapToCampaignModels(campaigns))
    }
}

class GetCampaignListFromDbUseCase @Inject constructor(
    private val store: BlazeCampaignsStore,
    private val mapper: CampaignListingUIModelMapper
) {
    suspend fun execute(site: SiteModel): Result<NoCampaigns, List<CampaignModel>> {
        val result = store.getBlazeCampaigns(site)
        if (result.campaigns.isEmpty()) return Result.Failure(NoCampaigns)
        return Result.Success(mapper.mapToCampaignModels(result.campaigns))
    }
}

sealed interface NetworkError

object GenericError : NetworkError

object NoCampaigns : NetworkError
