package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import android.util.Base64
import com.android.volley.Request.Method
import com.android.volley.RequestQueue
import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val AUTHORIZATION_HEADER = "Authorization"

@Singleton
class ApplicationPasswordRestClient @Inject constructor(
    private val requestQueue: RequestQueue,
    private val userAgent: UserAgent,
    private val applicationPasswordsStore: ApplicationPasswordsStore,
    private val jetpackApplicationPasswordGenerator: JetpackApplicationPasswordGenerator
) {
    suspend fun <T> executeGsonRequest(
        site: SiteModel,
        username: String,
        method: Int,
        path: String,
        clazz: Class<T>,
        params: Map<String, String> = emptyMap(),
        body: Map<String, Any> = emptyMap()
    ): WPAPIResponse<T> {
        val password = applicationPasswordsStore.getApplicationPassword(site.domainName)
            ?: run {
                when (val result = createApplicationPassword(site)) {
                    is ApplicationPasswordCreationResult.Success -> result.password
                    is ApplicationPasswordCreationResult.Failure ->
                        return WPAPIResponse.Error(result.error)
                    ApplicationPasswordCreationResult.NotSupported -> TODO()
                }
            }

        val authorizationHeader = "Basic " + Base64.encodeToString(
            "$username:$password".toByteArray(),
            Base64.NO_WRAP
        )

        val response = suspendCancellableCoroutine<WPAPIResponse<T>> { continuation ->
            val request = WPAPIGsonRequest(
                method,
                (site.wpApiRestUrl ?: site.url.slashJoin("wp-json")).slashJoin(path),
                params,
                body,
                clazz,
                {
                    continuation.resume(WPAPIResponse.Success(it))
                },
                {
                    continuation.resume(WPAPIResponse.Error(it))
                }
            )

            request.addHeader(AUTHORIZATION_HEADER, authorizationHeader)
            request.setUserAgent(userAgent.userAgent)

            requestQueue.add(request)

            continuation.invokeOnCancellation {
                request.cancel()
            }
        }

        return if (response is WPAPIResponse.Error && response.error.volleyError?.networkResponse?.statusCode == 401) {
            AppLog.w(
                AppLog.T.MAIN,
                "Authentication failure using application password, maybe revoked?" +
                    " Delete the saved one then retry"
            )
            applicationPasswordsStore.deleteApplicationPassword(site.domainName)
            executeGsonRequest(site, username, method, path, clazz, params, body)
        } else {
            response
        }
    }

    private suspend fun createApplicationPassword(
        site: SiteModel
    ): ApplicationPasswordCreationResult {
        val result = if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            jetpackApplicationPasswordGenerator.createApplicationPassword(
                site = site,
                applicationName = applicationPasswordsStore.applicationName
            )
        } else {
            TODO()
        }

        if (result is ApplicationPasswordCreationResult.Success) {
            applicationPasswordsStore.saveApplicationPassword(
                host = site.domainName,
                password = result.password
            )
        }
        return result
    }

    private val SiteModel.domainName
        get() = UrlUtils.removeScheme(url)

    suspend fun <T> executeGetGsonRequest(
        site: SiteModel,
        username: String,
        path: String,
        clazz: Class<T>,
        params: Map<String, String> = emptyMap()
    ) = executeGsonRequest(site, username, Method.GET, path, clazz, params)

    suspend fun <T> executePostGsonRequest(
        site: SiteModel,
        username: String,
        path: String,
        clazz: Class<T>,
        params: Map<String, String> = emptyMap(),
        body: Map<String, Any> = emptyMap()
    ) = executeGsonRequest(site, username, Method.POST, path, clazz, params, body)
}
