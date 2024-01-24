package org.wordpress.android.fluxc.store.blaze

import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignsModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsError
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsFetchedPayload
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.FakeBlazeTargetingRestClient
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingDao
import org.wordpress.android.fluxc.store.Store
import org.wordpress.android.fluxc.store.Store.OnChangedError
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlazeCampaignsStore @Inject constructor(
    private val restClient: BlazeCampaignsRestClient,
    private val fakeTargetingRestClient: FakeBlazeTargetingRestClient,
    private val campaignsDao: BlazeCampaignsDao,
    private val targetingDao: BlazeTargetingDao,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchBlazeCampaigns(
        site: SiteModel,
        page: Int = 1
    ): BlazeCampaignsResult<BlazeCampaignsModel> {
        fun handlePayloadError(
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
        suspend fun handlePayloadResponse(
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

        suspend fun storeBlazeCampaigns(
            site: SiteModel,
            payload: BlazeCampaignsFetchedPayload<BlazeCampaignsResponse>
        ): BlazeCampaignsResult<BlazeCampaignsModel> = when {
            payload.isError -> handlePayloadError(site, payload.error)
            payload.response != null -> handlePayloadResponse(site, payload.response)
            else -> BlazeCampaignsResult(BlazeCampaignsError(INVALID_RESPONSE))
        }

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

    fun observeMostRecentBlazeCampaign(site: SiteModel) =
        campaignsDao.observeMostRecentCampaignForSite(site.siteId)
            .map { it?.toDomainModel() }

    suspend fun fetchBlazeTargetingLocations(
        query: String,
        locale: String = Locale.getDefault().language
    ) = coroutineEngine.withDefaultContext(
        AppLog.T.API,
        this,
        "fetch blaze locations"
    ) {
        fakeTargetingRestClient.fetchBlazeLocations(query, locale).let { payload ->
            when {
                payload.isError -> BlazeTargetingResult(BlazeTargetingError(payload.error))
                else -> BlazeTargetingResult(payload.data)
            }
        }
    }

    suspend fun fetchBlazeTargetingTopics(
        locale: String = Locale.getDefault().language
    ) = coroutineEngine.withDefaultContext(
        AppLog.T.API,
        this,
        "fetch blaze topics"
    ) {
        fakeTargetingRestClient.fetchBlazeTopics(locale).let { payload ->
            when {
                payload.isError -> BlazeTargetingResult(BlazeTargetingError(payload.error))
                else -> {
                    targetingDao.replaceTopics(payload.data)
                    BlazeTargetingResult(payload.data)
                }
            }
        }
    }

    fun observeBlazeTargetingTopics(
        locale: String = Locale.getDefault().language
    ) = targetingDao.observeTopics(locale)

    suspend fun fetchBlazeTargetingLanguages(
        locale: String = Locale.getDefault().language
    ) = coroutineEngine.withDefaultContext(
        AppLog.T.API,
        this,
        "fetch blaze languages"
    ) {
        fakeTargetingRestClient.fetchBlazeLanguages(locale).let { payload ->
            when {
                payload.isError -> BlazeTargetingResult(BlazeTargetingError(payload.error))
                else -> {
                    targetingDao.replaceLanguages(payload.data)
                    BlazeTargetingResult(payload.data)
                }
            }
        }
    }

    fun observeBlazeTargetingLanguages(
        locale: String = Locale.getDefault().language
    ) = targetingDao.observeLanguages(locale)

    suspend fun fetchBlazeTargetingDevices(
        locale: String = Locale.getDefault().language
    ) = coroutineEngine.withDefaultContext(
        AppLog.T.API,
        this,
        "fetch blaze devices"
    ) {
        fakeTargetingRestClient.fetchBlazeDevices(locale).let { payload ->
            when {
                payload.isError -> BlazeTargetingResult(BlazeTargetingError(payload.error))
                else -> {
                    targetingDao.replaceDevices(payload.data)
                    BlazeTargetingResult(payload.data)
                }
            }
        }
    }

    fun observeBlazeTargetingDevices(
        locale: String = Locale.getDefault().language
    ) = targetingDao.observeDevices(locale)

    suspend fun fetchBlazeAdSuggestions(
        siteModel: SiteModel,
        productId: Long
    ) = coroutineEngine.withDefaultContext(
        AppLog.T.API,
        this,
        "fetch blaze ad suggestions"
    ) {
        fakeTargetingRestClient.fetchBlazeAdSuggestions(siteModel.siteId, productId).let { payload ->
            when {
                payload.isError -> BlazeTargetingResult(BlazeTargetingError(payload.error))
                else -> {
                    campaignsDao.replaceAdSuggestions(payload.data)
                    BlazeTargetingResult(payload.data)
                }
            }
        }
    }

    suspend fun getBlazeAdSuggestions(
        siteModel: SiteModel,
        productId: Long
    ) = campaignsDao.getBlazeAdSuggestions(siteModel.siteId, productId)

    data class BlazeCampaignsResult<T>(
        val model: T? = null,
        val cached: Boolean = false
    ) : Store.OnChanged<BlazeCampaignsError>() {
        constructor(error: BlazeCampaignsError) : this() {
            this.error = error
        }
    }

    data class BlazeTargetingResult<T>(
        val model: T? = null,
    ) : Store.OnChanged<BlazeTargetingError>() {
        constructor(error: BlazeTargetingError) : this() {
            this.error = error
        }
    }

    data class BlazeTargetingError(
        val type: GenericErrorType,
        val apiError: String? = null,
        val message: String? = null
    ) : OnChangedError {
        constructor(wpComGsonNetworkError: WPComGsonNetworkError) : this(
            wpComGsonNetworkError.type,
            wpComGsonNetworkError.apiError,
            wpComGsonNetworkError.message
        )
    }
}
