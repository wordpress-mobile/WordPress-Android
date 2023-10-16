package org.wordpress.android.fluxc.store.blaze

import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignsModel
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsError
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsFetchedPayload
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsRestClient
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao
import org.wordpress.android.fluxc.store.Store
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlazeCampaignsStore @Inject constructor(
    private val restClient: BlazeCampaignsRestClient,
    private val campaignsDao: BlazeCampaignsDao,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchBlazeCampaigns(
        site: SiteModel,
        page: Int = 1
    ): BlazeCampaignsResult<BlazeCampaignsModel> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetch blaze campaigns") {
            val payload = restClient.fetchBlazeCampaigns(site, page)
            storeBlazeCampaigns(site, payload)
        }
    }

    suspend fun getBlazeCampaigns(site: SiteModel): BlazeCampaignsModel {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "get blaze campaigns") {
            campaignsDao.getCampaignsAndPaginationForSite(site.siteId)
        }
    }

    fun observeBlazeCampaigns(site: SiteModel) = campaignsDao.observeCampaigns(site.siteId)

    suspend fun getMostRecentBlazeCampaign(site: SiteModel): BlazeCampaignModel? {
        return coroutineEngine.withDefaultContext(
            AppLog.T.API,
            this,
            "get most recent blaze campaign"
        ) {
            campaignsDao.getMostRecentCampaignForSite(site.siteId)?.toDomainModel()
        }
    }

    fun observeMostRecentBlazeCampaign(site: SiteModel) = campaignsDao.observeMostRecentCampaignForSite(site.siteId)
        .map { it?.toDomainModel() }

    private suspend fun storeBlazeCampaigns(
        site: SiteModel,
        payload: BlazeCampaignsFetchedPayload<BlazeCampaignsResponse>
    ): BlazeCampaignsResult<BlazeCampaignsModel> = when {
        payload.isError -> handlePayloadError(site, payload.error)
        payload.response != null -> handlePayloadResponse(site, payload.response)
        else -> BlazeCampaignsResult(BlazeCampaignsError(INVALID_RESPONSE))
    }

    private fun handlePayloadError(
        site: SiteModel,
        error: BlazeCampaignsError
    ): BlazeCampaignsResult<BlazeCampaignsModel> = when (error.type) {
        AUTHORIZATION_REQUIRED -> {
            campaignsDao.clear(site.siteId)
            BlazeCampaignsResult()
        }

        else -> BlazeCampaignsResult(error)
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun handlePayloadResponse(
        site: SiteModel,
        response: BlazeCampaignsResponse
    ): BlazeCampaignsResult<BlazeCampaignsModel> = try {
        val blazeCampaignsModel = response.toCampaignsModel()
        campaignsDao.insertCampaignsAndPageInfoForSite(site.siteId, blazeCampaignsModel)
        BlazeCampaignsResult(blazeCampaignsModel)
    } catch (e: Exception) {
        AppLog.e(AppLog.T.API, "Error storing blaze campaigns", e)
        BlazeCampaignsResult(BlazeCampaignsError(INVALID_RESPONSE))
    }

    data class BlazeCampaignsResult<T>(
        val model: T? = null,
        val cached: Boolean = false
    ) : Store.OnChanged<BlazeCampaignsError>() {
        constructor(error: BlazeCampaignsError) : this() {
            this.error = error
        }
    }
}
