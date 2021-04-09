package org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallError
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType.SITE_IS_JETPACK
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType.USERNAME_OR_PASSWORD_MISSING
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstalledPayload
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
    userAgent: UserAgent
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
}
