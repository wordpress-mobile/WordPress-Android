package org.wordpress.android.fluxc.network.rest.wpapi

import android.webkit.URLUtil
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Available
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.FailedRequest
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Unknown
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

class CookieNonceAuthenticator @Inject constructor(
    private val nonceRestClient: NonceRestClient,
    private val discoveryWPAPIRestClient: DiscoveryWPAPIRestClient,
    private val siteSqlUtils: SiteSqlUtils,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun authenticate(
        siteUrl: String,
        username: String,
        password: String
    ): CookieNonceAuthenticationResult {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "authenticate") {
            val siteUrlWithScheme = if (!URLUtil.isNetworkUrl(siteUrl)) {
                // If the URL is missing a scheme, try inferring it from the API endpoint
                val wpApiUrl = discoverApiEndpoint(UrlUtils.addUrlSchemeIfNeeded(siteUrl, false))
                val scheme = wpApiUrl.toHttpUrl().scheme
                UrlUtils.addUrlSchemeIfNeeded(UrlUtils.removeScheme(siteUrl), scheme == "https")
            } else siteUrl

            when (val nonce = nonceRestClient.requestNonce(siteUrlWithScheme, username, password)) {
                is Available -> CookieNonceAuthenticationResult.Success
                is FailedRequest -> {
                    CookieNonceAuthenticationResult.Error(
                        type = nonce.type,
                        message = nonce.errorMessage,
                        networkError = nonce.networkError
                    )
                }

                is Unknown -> CookieNonceAuthenticationResult.Error(type = Nonce.CookieNonceErrorType.UNKNOWN)
            }
        }
    }

    suspend fun <T : WPAPIResponse<*>> makeAuthenticatedWPAPIRequest(
        site: SiteModel,
        fetchMethod: suspend (Nonce) -> T
    ): T {
        val usingSavedRestUrl = site.wpApiRestUrl != null
        if (!usingSavedRestUrl) {
            site.wpApiRestUrl = discoverApiEndpoint(site.url)
            (siteSqlUtils::insertOrUpdateSite)(site)
        }

        val response = makeAuthenticatedWPAPIRequest(
            siteUrl = site.url,
            wpApiUrl = site.wpApiRestUrl,
            username = site.username,
            password = site.password
        ) { _, nonce ->
            fetchMethod(nonce)
        }

        return if (response is WPAPIResponse.Error<*> &&
            response.error.volleyError?.networkResponse?.statusCode == STATUS_CODE_NOT_FOUND) {
            // call failed with 'not found' so clear the (failing) rest url
            site.wpApiRestUrl = null
            (siteSqlUtils::insertOrUpdateSite)(site)

            if (usingSavedRestUrl) {
                // If we did the previous call with a saved rest url, try again by making
                // recursive call. This time there is no saved rest url to use
                // so the rest url will be retrieved using discovery
                makeAuthenticatedWPAPIRequest(site, fetchMethod)
            } else {
                // Already used discovery to fetch the rest base url and still got 'not found', so
                // just return the error response
                response
            }
        } else response
    }

    private suspend fun <T : WPAPIResponse<*>> makeAuthenticatedWPAPIRequest(
        siteUrl: String,
        wpApiUrl: String,
        username: String,
        password: String,
        fetchMethod: suspend (wpApiUrl: String, nonce: Nonce) -> T
    ): T {
        var nonce = nonceRestClient.getNonce(siteUrl, username)
        val usingSavedNonce = nonce is Available
        if (nonce !is Available) {
            nonce = nonceRestClient.requestNonce(siteUrl, username, password)
        }

        val response = fetchMethod(wpApiUrl, nonce)

        if (response is WPAPIResponse.Success<*>) return response

        val error = (response as WPAPIResponse.Error<*>).error
        val statusCode = error.volleyError?.networkResponse?.statusCode
        val errorCode = (error as? WPAPINetworkError)?.errorCode
        return when {
            statusCode == STATUS_CODE_UNAUTHORIZED ||
                (statusCode == STATUS_CODE_FORBIDDEN && errorCode == "rest_cookie_invalid_nonce") -> {
                if (usingSavedNonce) {
                    // Call with saved nonce failed, so try getting a new one
                    val previousNonce = nonce
                    val newNonce = nonceRestClient.requestNonce(siteUrl, username, password)

                    // Try original call again if we have a new nonce
                    val nonceIsUpdated = newNonce != previousNonce
                    if (nonceIsUpdated) {
                        fetchMethod(wpApiUrl, newNonce)
                    } else {
                        response
                    }
                } else {
                    response
                }
            }
            // For all other failures just return the error response
            else -> response
        }
    }

    private fun discoverApiEndpoint(
        url: String
    ): String {
        return discoveryWPAPIRestClient.discoverWPAPIBaseURL(url) // discover rest api endpoint
            ?: url.slashJoin("wp-json/") // fallback to ".../wp-json/" if discovery fails
    }

    sealed interface CookieNonceAuthenticationResult {
        object Success : CookieNonceAuthenticationResult
        data class Error(
            val type: Nonce.CookieNonceErrorType,
            val message: String? = null,
            val networkError: BaseNetworkError? = null,
        ) : CookieNonceAuthenticationResult
    }

    companion object {
        private const val STATUS_CODE_NOT_FOUND = 404
        private const val STATUS_CODE_FORBIDDEN = 403
        private const val STATUS_CODE_UNAUTHORIZED = 401
    }
}
