package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import com.android.volley.RequestQueue
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Credentials
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.HttpMethod
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.toVolleyMethod
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import org.wordpress.android.util.AppLog
import java.util.Optional
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val AUTHORIZATION_HEADER = "Authorization"
private const val UNAUTHORIZED = 401

@Singleton
class ApplicationPasswordsNetwork @Inject constructor(
    @Named("no-cookies") private val requestQueue: RequestQueue,
    private val userAgent: UserAgent,
    private val listener: Optional<ApplicationPasswordsListener>
) {
    // We can't use construction injection for this variable, as its class is internal
    @Inject internal lateinit var mApplicationPasswordsManager: ApplicationPasswordsManager

    @Suppress("ReturnCount")
    suspend fun <T> executeGsonRequest(
        site: SiteModel,
        method: HttpMethod,
        path: String,
        clazz: Class<T>,
        params: Map<String, String> = emptyMap(),
        body: Map<String, Any> = emptyMap()
    ): WPAPIResponse<T> {
        val credentialsResult = mApplicationPasswordsManager.getApplicationCredentials(site)
        val credentials = when (credentialsResult) {
            is ApplicationPasswordCreationResult.Existing -> credentialsResult.credentials
            is ApplicationPasswordCreationResult.Created -> {
                if (listener.isPresent) {
                    listener.get().onNewPasswordCreated()
                }
                credentialsResult.credentials
            }
            is ApplicationPasswordCreationResult.Failure ->
                return WPAPIResponse.Error(credentialsResult.error.toWPAPINetworkError())
            is ApplicationPasswordCreationResult.NotSupported -> {
                val networkError = credentialsResult.originalError.toWPAPINetworkError()
                if (listener.isPresent) {
                    listener.get().onFeatureUnavailable(site, networkError)
                }
                return WPAPIResponse.Error(networkError)
            }
        }

        val authorizationHeader = Credentials.basic(credentials.userName, credentials.password)

        val response = suspendCancellableCoroutine<WPAPIResponse<T>> { continuation ->
            val request = WPAPIGsonRequest(
                method.toVolleyMethod(),
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

        return if (credentialsResult is ApplicationPasswordCreationResult.Existing &&
            response is WPAPIResponse.Error &&
            response.error.volleyError?.networkResponse?.statusCode == UNAUTHORIZED) {
            AppLog.w(
                AppLog.T.MAIN,
                "Authentication failure using application password, maybe revoked?" +
                    " Delete the saved one then retry"
            )
            mApplicationPasswordsManager.deleteLocalApplicationPassword(site)
            executeGsonRequest(site, method, path, clazz, params, body)
        } else {
            response
        }
    }

    suspend fun deleteApplicationPassword(site: SiteModel) {
        mApplicationPasswordsManager.deleteApplicationCredentials(site)
    }

    suspend fun <T> executeGetGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        params: Map<String, String> = emptyMap()
    ) = executeGsonRequest(site, HttpMethod.GET, path, clazz, params)

    suspend fun <T> executePostGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        body: Map<String, Any> = emptyMap(),
        params: Map<String, String> = emptyMap()
    ) = executeGsonRequest(site, HttpMethod.POST, path, clazz, params, body)

    suspend fun <T> executePutGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        body: Map<String, Any> = emptyMap(),
        params: Map<String, String> = emptyMap()
    ) = executeGsonRequest(site, HttpMethod.PUT, path, clazz, params, body)

    suspend fun <T> executeDeleteGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        params: Map<String, String> = emptyMap(),
        body: Map<String, Any> = emptyMap()
    ) = executeGsonRequest(site, HttpMethod.DELETE, path, clazz, params, body)
}

private fun BaseNetworkError.toWPAPINetworkError(): WPAPINetworkError {
    return when (this) {
        is WPAPINetworkError -> this
        is WPComGsonNetworkError -> WPAPINetworkError(
            baseError = this,
            errorCode = this.apiError.orEmpty()
        )
        else -> WPAPINetworkError(this)
    }
}
