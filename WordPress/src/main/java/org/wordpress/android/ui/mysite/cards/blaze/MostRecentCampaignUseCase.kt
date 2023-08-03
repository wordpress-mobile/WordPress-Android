package org.wordpress.android.ui.mysite.cards.blaze

import org.wordpress.android.Result
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import javax.inject.Inject

class MostRecentCampaignUseCase @Inject constructor(
    private val store: BlazeCampaignsStore,
) {
    suspend fun execute(site: SiteModel): Result<NoCampaigns, BlazeCampaignModel> {
        val result = store.getMostRecentBlazeCampaign(site)
        result?.let { return Result.Success(it) } ?: return Result.Failure(NoCampaigns)
    }
}

sealed interface Error
object NoCampaigns : Error
