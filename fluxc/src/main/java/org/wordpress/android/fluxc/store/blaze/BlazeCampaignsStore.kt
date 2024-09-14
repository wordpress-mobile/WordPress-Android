package org.wordpress.android.fluxc.store.blaze

import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignCreationRequest
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignsModel
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingParameters
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignListResponse
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsError
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsFetchedPayload
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsRestClient.Companion.DEFAULT_PER_PAGE
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCreationRestClient
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao.BlazeCampaignEntity
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao.BlazeCampaignObjectiveEntity
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingDeviceEntity
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingLanguageEntity
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingTopicEntity
import org.wordpress.android.fluxc.store.Store
import org.wordpress.android.fluxc.store.Store.OnChangedError
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlazeCampaignsStore @Inject constructor(
    private val creationRestClient: BlazeCreationRestClient,
    private val campaignsRestClient: BlazeCampaignsRestClient,
    private val campaignsDao: BlazeCampaignsDao,
    private val targetingDao: BlazeTargetingDao,
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

    fun observeBlazeCampaigns(site: SiteModel) = campaignsDao
        .observeCampaigns(site.siteId)
        .map { campaigns -> campaigns.map { it.toDomainModel() } }

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

    suspend fun fetchBlazeCampaignObjectives(
        site: SiteModel,
        locale: String = Locale.getDefault().language
    ) = coroutineEngine.withDefaultContext(
        tag = AppLog.T.API,
        caller = this,
        loggedMessage = "fetch blaze objectives"
    ) {
        creationRestClient.fetchCampaignObjectives(site, locale).let { payload ->
            when {
                payload.isError -> BlazeResult(BlazeError(payload.error))
                else -> {
                    campaignsDao.replaceObjectives(payload.data?.map {
                        BlazeCampaignObjectiveEntity(
                            id = it.id,
                            title = it.title,
                            description = it.description,
                            locale = locale
                        )
                    }.orEmpty())
                    BlazeResult(payload.data)
                }
            }
        }
    }

    suspend fun fetchBlazeTargetingLocations(
        site: SiteModel,
        query: String,
        locale: String = Locale.getDefault().language
    ) = coroutineEngine.withDefaultContext(
        AppLog.T.API,
        this,
        "fetch blaze locations"
    ) {
        creationRestClient.fetchTargetingLocations(site, query, locale).let { payload ->
            when {
                payload.isError -> BlazeResult(BlazeError(payload.error))
                else -> BlazeResult(payload.data)
            }
        }
    }

    suspend fun fetchBlazeTargetingTopics(
        site: SiteModel,
        locale: String = Locale.getDefault().language
    ) = coroutineEngine.withDefaultContext(
        AppLog.T.API,
        this,
        "fetch blaze topics"
    ) {
        creationRestClient.fetchTargetingTopics(site, locale).let { payload ->
            when {
                payload.isError -> BlazeResult(BlazeError(payload.error))
                else -> {
                    targetingDao.replaceTopics(payload.data?.map {
                        BlazeTargetingTopicEntity(
                            id = it.id,
                            description = it.description,
                            locale = locale
                        )
                    }.orEmpty())
                    BlazeResult(payload.data)
                }
            }
        }
    }

    fun observeBlazeTargetingTopics(
        locale: String = Locale.getDefault().language
    ) = targetingDao.observeTopics(locale).map { topics -> topics.map { it.toDomainModel() } }

    suspend fun fetchBlazeTargetingLanguages(
        site: SiteModel,
        locale: String = Locale.getDefault().language
    ) = coroutineEngine.withDefaultContext(
        AppLog.T.API,
        this,
        "fetch blaze languages"
    ) {
        creationRestClient.fetchTargetingLanguages(site, locale).let { payload ->
            when {
                payload.isError -> BlazeResult(BlazeError(payload.error))
                else -> {
                    targetingDao.replaceLanguages(payload.data?.map {
                        BlazeTargetingLanguageEntity(
                            id = it.id,
                            name = it.name,
                            locale = locale
                        )
                    }.orEmpty())
                    BlazeResult(payload.data)
                }
            }
        }
    }

    fun observeBlazeTargetingLanguages(
        locale: String = Locale.getDefault().language
    ) = targetingDao.observeLanguages(locale)
        .map { languages -> languages.map { it.toDomainModel() } }

    suspend fun fetchBlazeTargetingDevices(
        site: SiteModel,
        locale: String = Locale.getDefault().language
    ) = coroutineEngine.withDefaultContext(
        AppLog.T.API,
        this,
        "fetch blaze devices"
    ) {
        creationRestClient.fetchTargetingDevices(site, locale).let { payload ->
            when {
                payload.isError -> BlazeResult(BlazeError(payload.error))
                else -> {
                    targetingDao.replaceDevices(payload.data?.map { device ->
                        BlazeTargetingDeviceEntity(
                            id = device.id,
                            name = device.name,
                            locale = locale
                        )
                    }.orEmpty())
                    BlazeResult(payload.data)
                }
            }
        }
    }

    fun observeBlazeTargetingDevices(
        locale: String = Locale.getDefault().language
    ) = targetingDao.observeDevices(locale).map { devices -> devices.map { it.toDomainModel() } }

    suspend fun fetchBlazeAdSuggestions(
        siteModel: SiteModel,
        productId: Long
    ) = coroutineEngine.withDefaultContext(
        AppLog.T.API,
        this,
        "fetch blaze ad suggestions"
    ) {
        creationRestClient.fetchAdSuggestions(siteModel, productId)
            .let { payload ->
                when {
                    payload.isError -> BlazeResult(BlazeError(payload.error))
                    else -> {
                        BlazeResult(payload.data)
                    }
                }
            }
    }

    suspend fun fetchBlazeAdForecast(
        siteModel: SiteModel,
        startDate: Date,
        endDate: Date,
        totalBudget: Double,
        timeZoneId: String = TimeZone.getDefault().id,
        targetingParameters: BlazeTargetingParameters? = null
    ) = coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetch blaze ad forecast") {
        creationRestClient.fetchAdForecast(
            site = siteModel,
            startDate = startDate,
            endDate = endDate,
            totalBudget = totalBudget,
            timeZoneId = timeZoneId,
            targetingParameters = targetingParameters
        ).let { payload ->
            when {
                payload.isError -> BlazeResult(BlazeError(payload.error))
                else -> {
                    BlazeResult(payload.data)
                }
            }
        }
    }

    suspend fun fetchBlazePaymentMethods(
        site: SiteModel
    ) = coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetch blaze payment methods") {
        creationRestClient.fetchPaymentMethods(site).let { payload ->
            when {
                payload.isError -> BlazeResult(BlazeError(payload.error))
                else -> {
                    BlazeResult(payload.data)
                }
            }
        }
    }

    suspend fun createCampaign(
        site: SiteModel,
        request: BlazeCampaignCreationRequest
    ) = coroutineEngine.withDefaultContext(AppLog.T.API, this, "create blaze campaign") {
        creationRestClient.createCampaign(site, request).let { payload ->
            when {
                payload.isError -> BlazeResult(BlazeError(payload.error))
                payload.data == null -> BlazeResult(BlazeError(type = GenericErrorType.UNKNOWN))
                else -> {
                    campaignsDao.insert(
                        listOf(
                            BlazeCampaignEntity.fromDomainModel(
                                site.siteId,
                                payload.data
                            )
                        )
                    )
                    BlazeResult(payload.data)
                }
            }
        }
    }

    data class BlazeCampaignsResult<T>(
        val model: T? = null,
        val cached: Boolean = false
    ) : Store.OnChanged<BlazeCampaignsError>() {
        constructor(error: BlazeCampaignsError) : this() {
            this.error = error
        }
    }

    data class BlazeResult<T>(
        val model: T? = null,
    ) : Store.OnChanged<BlazeError>() {
        constructor(error: BlazeError) : this() {
            this.error = error
        }
    }

    data class BlazeError(
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
