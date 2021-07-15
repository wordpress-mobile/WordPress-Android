package org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.JPAPI
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModuleError
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModuleErrorType
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModuleErrorType.API_ERROR
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModulePayload
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModuleResultPayload
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallError
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType.SITE_IS_JETPACK
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType.USERNAME_OR_PASSWORD_MISSING
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstalledPayload
import org.wordpress.android.fluxc.utils.NetworkErrorMapper
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class JetpackRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun installJetpack(site: SiteModel): JetpackInstalledPayload {
        val url = WPCOMREST.jetpack_install.site(URLEncoder.encode(site.url, "UTF-8")).urlV1
        val body = mapOf("user" to site.username, "password" to site.password)
        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                null,
                body,
                JetpackInstallResponse::class.java
        )
        return when (response) {
            is Success -> JetpackInstalledPayload(site, response.data.status)
            is WPComGsonRequestBuilder.Response.Error -> {
                val error = when {
                    response.error.apiError == "SITE_IS_JETPACK" -> SITE_IS_JETPACK
                    response.error.isGeneric &&
                            response.error.type == BaseRequest.GenericErrorType.INVALID_RESPONSE -> INVALID_RESPONSE
                    response.error.apiError == "unauthorized" -> AUTHORIZATION_REQUIRED
                    response.error.apiError == "INVALID_INPUT" -> USERNAME_OR_PASSWORD_MISSING
                    else -> GENERIC_ERROR
                }
                JetpackInstalledPayload(
                        JetpackInstallError(error, response.error.apiError, response.error.message),
                        site
                )
            }
        }
    }

    data class JetpackInstallResponse(val status: Boolean)

    /**
     * Makes a POST request to `POST /jetpack/v4/module/stats/active/` to activate
     * the jetpack stats module
     *
     * Dispatches a post to activate action with the result
     * url = "/jetpack/v4/module/stats/active/"
     *
     * Response{"path":"/jetpack/v4/module/stats/active/","body":"{\"active\":true}"}
     *
     * @param [payload] The payload to activate the stats module
     */
    suspend fun activateStatsModule(payload: ActivateStatsModulePayload): ActivateStatsModuleResultPayload {
        val url = JPAPI.module.stats.active.pathV4
        val params = mutableMapOf("active" to true)
        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
                this,
                payload.site,
                url,
                params,
                StatsModuleActivatedApiResponse::class.java
        )
        return when (response) {
            is JetpackSuccess -> {
                if (response.data?.code == "success") {
                    ActivateStatsModuleResultPayload(true, payload.site)
                } else {
                    val error = ActivateStatsModuleError(API_ERROR)
                    ActivateStatsModuleResultPayload(error, payload.site)
                }
            }
            is JetpackError -> {
                val errorType = NetworkErrorMapper.map(
                        response.error,
                        ActivateStatsModuleErrorType.GENERIC_ERROR,
                        ActivateStatsModuleErrorType.INVALID_RESPONSE,
                        ActivateStatsModuleErrorType.AUTHORIZATION_REQUIRED
                )
                val error = ActivateStatsModuleError(errorType, response.error.message)
                ActivateStatsModuleResultPayload(error, payload.site)
            }
        }
    }

    class StatsModuleActivatedApiResponse : Response {
        val code: String? = null
        val message: String? = null
    }
}
