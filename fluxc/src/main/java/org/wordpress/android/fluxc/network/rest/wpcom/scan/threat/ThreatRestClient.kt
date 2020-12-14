package org.wordpress.android.fluxc.network.rest.wpcom.scan.threat

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatMapper
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.ThreatStore.FetchedThreatPayload
import org.wordpress.android.fluxc.store.ThreatStore.ThreatError
import org.wordpress.android.fluxc.store.ThreatStore.ThreatErrorType
import org.wordpress.android.fluxc.utils.NetworkErrorMapper
import javax.inject.Singleton

@Singleton
class ThreatRestClient(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    private val threatMapper: ThreatMapper,
    dispatcher: Dispatcher,
    appContext: Context?,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchThreat(site: SiteModel, threatId: Long): FetchedThreatPayload {
        val url = WPCOMV2.sites.site(site.siteId).scan.threat.item(threatId).url

        val response = wpComGsonRequestBuilder.syncGetRequest(this, url, mapOf(), ThreatResponse::class.java)
        return when (response) {
            is Success -> {
                if (response.data.success == false) {
                    val error = ThreatError(ThreatErrorType.API_ERROR)
                    FetchedThreatPayload(error, site)
                } else {
                    buildThreatPayload(response.data, site)
                }
            }
            is Error -> {
                val errorType = NetworkErrorMapper.map(
                    response.error,
                    ThreatErrorType.GENERIC_ERROR,
                    ThreatErrorType.INVALID_RESPONSE,
                    ThreatErrorType.AUTHORIZATION_REQUIRED
                )
                val error = ThreatError(errorType, response.error.message)
                FetchedThreatPayload(error, site = site)
            }
        }
    }

    private fun buildThreatPayload(response: ThreatResponse, site: SiteModel): FetchedThreatPayload {
        val threatStatus = ThreatStatus.fromValue(response.threat?.status)
        if (threatStatus != ThreatStatus.UNKNOWN) {
            response.threat?.let { threat ->
                var error: ThreatErrorType? = null
                val threatModel = when {
                    threat.id == null -> {
                        error = ThreatErrorType.MISSING_THREAT_ID
                        null
                    }
                    threat.signature == null -> {
                        error = ThreatErrorType.MISSING_SIGNATURE
                        null
                    }
                    threat.firstDetected == null -> {
                        error = ThreatErrorType.MISSING_FIRST_DETECTED
                        null
                    }
                    else -> {
                        threatMapper.map(threat)
                    }
                }
                error?.let {
                    return buildThreatErrorPayload(site, it)
                }
                return FetchedThreatPayload(threatModel = threatModel, site = site)
            }
        }
        return buildThreatErrorPayload(site, ThreatErrorType.INVALID_RESPONSE)
    }

    private fun buildThreatErrorPayload(site: SiteModel, errorType: ThreatErrorType) =
        FetchedThreatPayload(ThreatError(errorType), site)
}
