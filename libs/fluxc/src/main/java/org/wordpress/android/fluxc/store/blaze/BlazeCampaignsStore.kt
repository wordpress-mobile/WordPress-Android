package org.wordpress.android.fluxc.store.blaze

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignsModel
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignListResponse
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsError
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsFetchedPayload
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsRestClient.Companion.DEFAULT_PER_PAGE
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao
import org.wordpress.android.fluxc.store.Store
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlazeCampaignsStore @Inject constructor(
    private val campaignsRestClient: BlazeCampaignsRestClient,
    private val campaignsDao: BlazeCampaignsDao,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchBlazeCampaigns(
        site: SiteModel,
        offset: Int = 0,
        perPage: Int = DEFAULT_PER_PAGE,
        locale: String = Locale.getDefault().language,
        status: String? = null,
    ): BlazeCampaignsResult<BlazeCampaignsModel> {
        fun handlePayloadError(
            site: SiteModel,
            error: BlazeCampaignsError
        ): BlazeCampaignsResult<BlazeCampaignsModel> = when (error.type) {
            AUTHORIZATION_REQUIRED -> {
                campaignsDao.clearBlazeCampaigns(site.siteId)
                BlazeCampaignsResult()
            }

            else -> BlazeCampaignsResult(error)
        }

        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        suspend fun handlePayloadResponse(
            site: SiteModel,
            response: BlazeCampaignListResponse
        ): BlazeCampaignsResult<BlazeCampaignsModel> = try {
            val blazeCampaignsModel = response.toCampaignsModel()
            campaignsDao.insertCampaigns(site.siteId, blazeCampaignsModel)
            BlazeCampaignsResult(blazeCampaignsModel)
        } catch (e: Exception) {
            AppLog.e(AppLog.T.API, "Error storing blaze campaigns", e)
            BlazeCampaignsResult(BlazeCampaignsError(INVALID_RESPONSE))
        }

        suspend fun storeBlazeCampaigns(
            site: SiteModel,
            payload: BlazeCampaignsFetchedPayload<BlazeCampaignListResponse>
        ): BlazeCampaignsResult<BlazeCampaignsModel> = when {
            payload.isError -> handlePayloadError(site, payload.error)
            payload.response != null -> handlePayloadResponse(site, payload.response)
            else -> BlazeCampaignsResult(BlazeCampaignsError(INVALID_RESPONSE))
        }

        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetch blaze campaigns") {
            val payload = campaignsRestClient.fetchBlazeCampaigns(
                site.siteId,
                offset,
                perPage,
                locale,
                status
            )
            storeBlazeCampaigns(site, payload)
        }
    }

    suspend fun getBlazeCampaigns(site: SiteModel): List<BlazeCampaignModel> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "get blaze campaigns") {
            campaignsDao.getCachedCampaigns(site.siteId)
        }
    }

    suspend fun getMostRecentBlazeCampaign(site: SiteModel): BlazeCampaignModel? {
        return coroutineEngine.withDefaultContext(
            AppLog.T.API,
            this,
            "get most recent blaze campaign"
        ) {
            campaignsDao.getMostRecentCampaignForSite(site.siteId)?.toDomainModel()
        }
    }

    data class BlazeCampaignsResult<T>(
        val model: T? = null
    ) : Store.OnChanged<BlazeCampaignsError>() {
        constructor(error: BlazeCampaignsError) : this() {
            this.error = error
        }
    }
}
