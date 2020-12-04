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
import org.wordpress.android.fluxc.model.scan.threat.ThreatMapper
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.ScanStore.FetchedScanStatePayload
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

    private fun buildScanStatePayload(response: ScanStateResponse, site: SiteModel): FetchedScanStatePayload {
        val scanStateModel = mapResponseToScanStateModel(response)
        scanStateModel?.let {
            return FetchedScanStatePayload(scanStateModel, site)
        }
        return buildErrorPayload(site, ScanStateErrorType.INVALID_RESPONSE)
    }

    private fun mapResponseToScanStateModel(response: ScanStateResponse): ScanStateModel? {
        val state = State.fromValue(response.state) ?: return null

        return ScanStateModel(
                state = state,
                reason = response.reason,
                threats = response.threats?.map { threatMapper.map(it) },
                hasCloud = response.hasCloud,
                credentials = response.credentials?.map {
                    Credentials(it.type, it.role, it.host, it.port, it.user, it.path, it.stillValid)
                },
                mostRecentStatus = response.mostRecentStatus?.let {
                    ScanProgressStatus(
                            startDate = it.startDate,
                            duration = it.duration,
                            progress = it.progress,
                            error = it.error,
                            isInitial = it.isInitial
                    )
                },
                currentStatus = response.currentStatus?.let {
                    ScanProgressStatus(
                            startDate = it.startDate,
                            progress = it.progress,
                            isInitial = it.isInitial
                    )
                }
        )
    }

    private fun buildErrorPayload(site: SiteModel, errorType: ScanStateErrorType) =
            FetchedScanStatePayload(ScanStateError(errorType), site)
}
