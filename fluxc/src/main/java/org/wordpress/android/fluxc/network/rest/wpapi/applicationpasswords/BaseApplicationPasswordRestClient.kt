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
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val AUTHORIZATION_HEADER = "Authorization"

@Singleton
class ApplicationPasswordRestClient @Inject constructor(
    private val requestQueue: RequestQueue,
    private val userAgent: UserAgent,
    private val applicationPasswordsStore: ApplicationPasswordsStore
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
        val password = applicationPasswordsStore.getApplicationPassword(UrlUtils.removeScheme(site.url))
            ?: run {
                TODO("Implement fetching password")
            }

        val authorizationHeader = "Basic " + Base64.encodeToString(
            "$username:$password".toByteArray(),
            Base64.NO_WRAP
        )

        return suspendCancellableCoroutine { continuation ->
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
    }

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
