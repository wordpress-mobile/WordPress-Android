package org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting

import org.wordpress.android.Either
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import javax.inject.Inject

class FetchCampaignListUseCase @Inject constructor(
    private val store: BlazeCampaignsStore,
    private val mapper: CampaignListingUIModelMapper
) {
    suspend fun execute(site: SiteModel, page: Int): Either<NetworkError, List<CampaignModel>> {
        val result = store.fetchBlazeCampaigns(site, page)
        if (result.isError || result.model == null) return Either.Left(GenericError)
        if (result.model!!.campaigns.isEmpty()) return Either.Left(NoCampaigns)
        val campaigns = result.model!!.campaigns.map { mapper.mapToCampaignModel(it) }
        return Either.Right(campaigns)
    }
}

class GetCampaignListFromDbUseCase @Inject constructor(
    private val store: BlazeCampaignsStore,
    private val mapper: CampaignListingUIModelMapper
) {
    suspend fun execute(site: SiteModel): Either<NoCampaigns, List<CampaignModel>> {
        val result = store.getBlazeCampaigns(site)
        if (result.campaigns.isEmpty()) return  Either.Left(NoCampaigns)
        val campaigns = result.campaigns.map { mapper.mapToCampaignModel(it) }
        return  Either.Right(campaigns)
    }
}

sealed interface NetworkError

object GenericError : NetworkError

object NoCampaigns : NetworkError
