package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import com.android.volley.Request.Method
import com.android.volley.RequestQueue
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Credentials
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val AUTHORIZATION_HEADER = "Authorization"

@Singleton
class ApplicationPasswordRestClient @Inject constructor(
    @Named("no-cookies") private val requestQueue: RequestQueue,
    private val userAgent: UserAgent,
    private val applicationPasswordManager: ApplicationPasswordManager
) {
    suspend fun <T> executeGsonRequest(
        site: SiteModel,
        method: Int,
        path: String,
        clazz: Class<T>,
        params: Map<String, String> = emptyMap(),
        body: Map<String, Any> = emptyMap()
    ): WPAPIResponse<T> {
        val credentials = when (val result = applicationPasswordManager.getApplicationCredentials(site)) {
            is ApplicationPasswordCreationResult.Success -> result.credentials
            is ApplicationPasswordCreationResult.Failure ->
                return WPAPIResponse.Error(result.error)
            ApplicationPasswordCreationResult.NotSupported -> TODO()
        }

        val authorizationHeader = Credentials.basic(credentials.userName, credentials.password)

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
            applicationPasswordManager.deleteLocalApplicationPassword(site)
            executeGsonRequest(site, method, path, clazz, params, body)
        } else {
            response
        }
    }

    suspend fun <T> executeGetGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        params: Map<String, String> = emptyMap()
    ) = executeGsonRequest(site, Method.GET, path, clazz, params)

    suspend fun <T> executePostGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        body: Map<String, Any> = emptyMap(),
        params: Map<String, String> = emptyMap()
    ) = executeGsonRequest(site, Method.POST, path, clazz, params, body)

    suspend fun <T> executePutGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        body: Map<String, Any> = emptyMap(),
        params: Map<String, String> = emptyMap()
    ) = executeGsonRequest(site, Method.PUT, path, clazz, params, body)

    suspend fun <T> executeDeleteGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        params: Map<String, String> = emptyMap(),
        body: Map<String, Any> = emptyMap()
    ) = executeGsonRequest(site, Method.DELETE, path, clazz, params, body)

}
