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
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallError
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType.SITE_IS_JETPACK
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstallErrorType.USERNAME_OR_PASSWORD_MISSING
import org.wordpress.android.fluxc.store.JetpackStore.JetpackInstalledPayload
import java.net.URLEncoder
import javax.inject.Singleton
import kotlin.coroutines.experimental.suspendCoroutine

@Singleton
class JetpackRestClient
constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun installJetpack(site: SiteModel) = suspendCoroutine<JetpackInstalledPayload> { cont ->
        val url = WPCOMREST.jetpack_install.site(URLEncoder.encode(site.url, "UTF-8")).urlV1
        val params = mapOf("user" to site.username, "password" to site.password)
        val request = wpComGsonRequestBuilder.buildPostRequest(
                url, params, JetpackInstallResponse::class.java,
                { response ->
                    cont.resume(JetpackInstalledPayload(site, response.status))
                },
                { networkError ->
                    val error = when {
                        networkError.apiError == "SITE_IS_JETPACK" -> SITE_IS_JETPACK
                        networkError.isGeneric &&
                                networkError.type == BaseRequest.GenericErrorType.INVALID_RESPONSE -> INVALID_RESPONSE
                        networkError.apiError == "unauthorized" -> AUTHORIZATION_REQUIRED
                        networkError.apiError == "INVALID_INPUT" -> USERNAME_OR_PASSWORD_MISSING
                        else -> GENERIC_ERROR
                    }
                    cont.resume(JetpackInstalledPayload(JetpackInstallError(error, networkError.message), site))
                })
        add(request)
    }

    data class JetpackInstallResponse(val status: Boolean)
}
