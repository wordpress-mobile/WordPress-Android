package org.wordpress.android.fluxc.network.rest.wpcom.scan

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel.Credentials
import org.wordpress.android.fluxc.model.scan.ScanStateModel.ScanProgressStatus
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State
import org.wordpress.android.fluxc.model.scan.threat.FixThreatStatusModel
import org.wordpress.android.fluxc.model.scan.threat.FixThreatStatusModel.FixStatus
import org.wordpress.android.fluxc.model.scan.threat.ThreatMapper
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.FixThreatsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.FixThreatsStatusResponse
import org.wordpress.android.fluxc.store.ScanStore.FetchFixThreatsStatusResultPayload
import org.wordpress.android.fluxc.store.ScanStore.FetchedScanStatePayload
import org.wordpress.android.fluxc.store.ScanStore.FixThreatsError
import org.wordpress.android.fluxc.store.ScanStore.FixThreatsErrorType
import org.wordpress.android.fluxc.store.ScanStore.FixThreatsResultPayload
import org.wordpress.android.fluxc.store.ScanStore.FixThreatsStatusError
import org.wordpress.android.fluxc.store.ScanStore.FixThreatsStatusErrorType
import org.wordpress.android.fluxc.store.ScanStore.IgnoreThreatError
import org.wordpress.android.fluxc.store.ScanStore.IgnoreThreatErrorType
import org.wordpress.android.fluxc.store.ScanStore.IgnoreThreatResultPayload
import org.wordpress.android.fluxc.store.ScanStore.ScanStartError
import org.wordpress.android.fluxc.store.ScanStore.ScanStartErrorType
import org.wordpress.android.fluxc.store.ScanStore.ScanStartResultPayload
import org.wordpress.android.fluxc.store.ScanStore.ScanStateError
import org.wordpress.android.fluxc.store.ScanStore.ScanStateErrorType
import org.wordpress.android.fluxc.utils.NetworkErrorMapper
import javax.inject.Singleton

@Singleton
class ScanRestClient(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    private val threatMapper: ThreatMapper,
    dispatcher: Dispatcher,
    appContext: Context?,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchScanState(site: SiteModel): FetchedScanStatePayload {
        val url = WPCOMV2.sites.site(site.siteId).scan.url
        val response = wpComGsonRequestBuilder.syncGetRequest(this, url, mapOf(), ScanStateResponse::class.java)
        return when (response) {
            is Success -> {
                buildScanStatePayload(response.data, site)
            }
            is Error -> {
                val errorType = NetworkErrorMapper.map(
                    response.error,
                    ScanStateErrorType.GENERIC_ERROR,
                    ScanStateErrorType.INVALID_RESPONSE,
                    ScanStateErrorType.AUTHORIZATION_REQUIRED
                )
                val error = ScanStateError(errorType, response.error.message)
                FetchedScanStatePayload(error, site)
            }
        }
    }

    suspend fun startScan(site: SiteModel): ScanStartResultPayload {
        val url = WPCOMV2.sites.site(site.siteId).scan.enqueue.url

        val response = wpComGsonRequestBuilder.syncPostRequest(this, url, mapOf(), null, ScanStartResponse::class.java)
        return when (response) {
            is Success -> {
                if (response.data.success == false && response.data.error != null) {
                    val error = ScanStartError(ScanStartErrorType.API_ERROR)
                    ScanStartResultPayload(error, site)
                } else {
                    ScanStartResultPayload(site)
                }
            }
            is Error -> {
                val errorType = NetworkErrorMapper.map(
                    response.error,
                    ScanStartErrorType.GENERIC_ERROR,
                    ScanStartErrorType.INVALID_RESPONSE,
                    ScanStartErrorType.AUTHORIZATION_REQUIRED
                )
                val error = ScanStartError(errorType, response.error.message)
                ScanStartResultPayload(error, site)
            }
        }
    }

    suspend fun fixThreats(remoteSiteId: Long, threatIds: List<Long>): FixThreatsResultPayload {
        val url = WPCOMV2.sites.site(remoteSiteId).alerts.fix.url
        val params = buildFixThreatsRequestParams(threatIds)
        val response = wpComGsonRequestBuilder.syncPostRequest(this, url, params, null, FixThreatsResponse::class.java)
        return when (response) {
            is Success -> {
                if (response.data.ok == true) {
                    FixThreatsResultPayload(remoteSiteId)
                } else {
                    val error = FixThreatsError(FixThreatsErrorType.API_ERROR)
                    FixThreatsResultPayload(error, remoteSiteId)
                }
            }
            is Error -> {
                val errorType = NetworkErrorMapper.map(
                    response.error,
                    FixThreatsErrorType.GENERIC_ERROR,
                    FixThreatsErrorType.INVALID_RESPONSE,
                    FixThreatsErrorType.AUTHORIZATION_REQUIRED
                )
                val error = FixThreatsError(errorType, response.error.message)
                FixThreatsResultPayload(error, remoteSiteId)
            }
        }
    }

    private fun buildFixThreatsRequestParams(threatIds: List<Long>) = mutableMapOf<String, String>().apply {
        threatIds.forEachIndexed { index, value ->
            put("threat_ids[$index]", value.toString())
        }
    }

    suspend fun ignoreThreat(remoteSiteId: Long, threatId: Long): IgnoreThreatResultPayload {
        val url = WPCOMV2.sites.site(remoteSiteId).alerts.threat(threatId).url
        val params = mapOf("ignore" to "true")
        val response = wpComGsonRequestBuilder.syncPostRequest(this, url, params, null, Any::class.java)
        return when (response) {
            is Success -> IgnoreThreatResultPayload(remoteSiteId)
            is Error -> {
                val errorType = NetworkErrorMapper.map(
                    response.error,
                    IgnoreThreatErrorType.GENERIC_ERROR,
                    IgnoreThreatErrorType.INVALID_RESPONSE,
                    IgnoreThreatErrorType.AUTHORIZATION_REQUIRED
                )
                val error = IgnoreThreatError(errorType, response.error.message)
                IgnoreThreatResultPayload(error, remoteSiteId)
            }
        }
    }

    suspend fun fetchFixThreatsStatus(remoteSiteId: Long, threatIds: List<Long>): FetchFixThreatsStatusResultPayload {
        val url = WPCOMV2.sites.site(remoteSiteId).alerts.fix.url
        val params = buildFixThreatsRequestParams(threatIds)
        val response = wpComGsonRequestBuilder.syncGetRequest(this, url, params, FixThreatsStatusResponse::class.java)
        return when (response) {
            is Success -> {
                if (response.data.ok == true) {
                    buildFixThreatsStatusPayload(response.data, remoteSiteId)
                } else {
                    val error = FixThreatsStatusError(FixThreatsStatusErrorType.API_ERROR)
                    FetchFixThreatsStatusResultPayload(remoteSiteId = remoteSiteId, error = error)
                }
            }
            is Error -> {
                val errorType = NetworkErrorMapper.map(
                    response.error,
                    FixThreatsStatusErrorType.GENERIC_ERROR,
                    FixThreatsStatusErrorType.INVALID_RESPONSE,
                    FixThreatsStatusErrorType.AUTHORIZATION_REQUIRED
                )
                val error = FixThreatsStatusError(errorType, response.error.message)
                FetchFixThreatsStatusResultPayload(remoteSiteId = remoteSiteId, error = error)
            }
        }
    }

    private fun buildScanStatePayload(response: ScanStateResponse, site: SiteModel): FetchedScanStatePayload {
        val state = State.fromValue(response.state) ?: return buildScanStateErrorPayload(
            site,
            ScanStateErrorType.INVALID_RESPONSE
        )
        var error: ScanStateErrorType? = null
        val threatModels = response.threats?.mapNotNull { threat ->
            val threatModel = when {
                threat.id == null -> {
                    error = ScanStateErrorType.MISSING_THREAT_ID
                    null
                }
                threat.signature == null -> {
                    error = ScanStateErrorType.MISSING_THREAT_SIGNATURE
                    null
                }
                threat.firstDetected == null -> {
                    error = ScanStateErrorType.MISSING_THREAT_FIRST_DETECTED
                    null
                }
                else -> {
                    val threatStatus = ThreatStatus.fromValue(threat.status)
                    if (threatStatus != ThreatStatus.UNKNOWN) {
                        threatMapper.map(threat)
                    } else {
                        error = ScanStateErrorType.INVALID_RESPONSE
                        null
                    }
                }
            }
            threatModel
        }
        error?.let {
            return buildScanStateErrorPayload(site, it)
        }
        val scanStateModel = ScanStateModel(
            state = state,
            reason = response.reason,
            threats = threatModels,
            hasCloud = response.hasCloud ?: false,
            credentials = response.credentials?.map {
                Credentials(it.type, it.role, it.host, it.port, it.user, it.path, it.stillValid)
            },
            mostRecentStatus = response.mostRecentStatus?.let {
                ScanProgressStatus(
                    startDate = it.startDate,
                    duration = it.duration ?: 0,
                    progress = it.progress ?: 0,
                    error = it.error ?: false,
                    isInitial = it.isInitial ?: false
                )
            },
            currentStatus = response.currentStatus?.let {
                ScanProgressStatus(
                    startDate = it.startDate,
                    progress = it.progress ?: 0,
                    isInitial = it.isInitial ?: false
                )
            }
        )
        return FetchedScanStatePayload(scanStateModel, site)
    }

    private fun buildFixThreatsStatusPayload(
        response: FixThreatsStatusResponse,
        remoteSiteId: Long
    ): FetchFixThreatsStatusResultPayload {
        var error: FixThreatsStatusErrorType? = null
        val fixThreatStatusModels = response.fixThreatsStatus?.mapNotNull {
            if (it.id == null) {
                error = FixThreatsStatusErrorType.MISSING_THREAT_ID
                null
            } else {
                FixThreatStatusModel(id = it.id, status = FixStatus.fromValue(it.status))
            }
        } ?: emptyList()

        return error?.let {
            FetchFixThreatsStatusResultPayload(remoteSiteId = remoteSiteId, error = FixThreatsStatusError(it))
        } ?: FetchFixThreatsStatusResultPayload(
            remoteSiteId = remoteSiteId,
            fixThreatStatusModels = fixThreatStatusModels
        )
    }

    private fun buildScanStateErrorPayload(site: SiteModel, errorType: ScanStateErrorType) =
        FetchedScanStatePayload(ScanStateError(errorType), site)
}
