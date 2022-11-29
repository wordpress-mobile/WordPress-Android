package org.wordpress.android.fluxc.network.rest.wpapi

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Available
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.FailedRequest
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Unknown
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

class WPAPIAuthenticator @Inject constructor(
    private val nonceRestClient: NonceRestClient,
    private val discoveryWPAPIRestClient: DiscoveryWPAPIRestClient,
    private val siteSqlUtils: SiteSqlUtils,
    private val currentTimeProvider: CurrentTimeProvider
) {
    suspend fun <T : Payload<BaseNetworkError?>> makeAuthenticatedWPAPIRequest(
        siteUrl: String,
        username: String,
        password: String,
        fetchMethod: suspend (wpApiUrl: String, nonce: Nonce?) -> T
    ): T {
        val wpApiUrl = discoverApiEndpoint(UrlUtils.addUrlSchemeIfNeeded(siteUrl, false))
        val scheme = wpApiUrl.toHttpUrl().scheme

        return makeAuthenticatedWPAPIRequest(
            // Use the scheme from the discovered URL to avoid redirects
            siteUrl = UrlUtils.addUrlSchemeIfNeeded(UrlUtils.removeScheme(siteUrl), scheme == "https"),
            wpApiUrl = wpApiUrl,
            username = username,
            password = password,
            fetchMethod = fetchMethod
        )
    }

    suspend fun <T : Payload<BaseNetworkError?>> makeAuthenticatedWPAPIRequest(
        site: SiteModel,
        fetchMethod: suspend (Nonce?) -> T
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

        return if (response.error?.volleyError?.networkResponse?.statusCode == STATUS_CODE_NOT_FOUND) {
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

    private suspend fun <T : Payload<BaseNetworkError?>> makeAuthenticatedWPAPIRequest(
        siteUrl: String,
        wpApiUrl: String,
        username: String,
        password: String,
        fetchMethod: suspend (wpApiUrl: String, nonce: Nonce?) -> T
    ): T {
        var nonce = nonceRestClient.getNonce(siteUrl)
        val usingSavedNonce = nonce is Available
        val failedRecently = nonce.failedRecently()
        if (nonce is Unknown || !(usingSavedNonce || failedRecently)) {
            nonce = nonceRestClient.requestNonce(siteUrl, username, password)
        }

        val response = fetchMethod(wpApiUrl, nonce)

        if (!response.isError) return response
        return when (response.error?.volleyError?.networkResponse?.statusCode) {
            STATUS_CODE_UNAUTHORIZED -> {
                if (usingSavedNonce) {
                    // Call with saved nonce failed, so try getting a new one
                    val previousNonce = nonce
                    val newNonce = nonceRestClient.requestNonce(siteUrl, username, password)

                    // Try original call again if we have a new nonce
                    val nonceIsUpdated = newNonce != null && newNonce != previousNonce
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

    private fun Nonce?.failedRecently() = (this as? FailedRequest)?.timeOfResponse?.let {
        it + FIVE_MIN_MILLIS > currentTimeProvider.currentDate().time
    } == true

    companion object {
        private const val FIVE_MIN_MILLIS: Long = 5 * 60 * 1000
        private const val STATUS_CODE_NOT_FOUND = 404
        private const val STATUS_CODE_UNAUTHORIZED = 401
    }
}
