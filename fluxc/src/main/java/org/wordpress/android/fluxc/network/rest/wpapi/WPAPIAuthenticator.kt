package org.wordpress.android.fluxc.network.rest.wpapi

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Available
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.FailedRequest
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Unknown
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.store.ReactNativeStore
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import javax.inject.Inject

class WPAPIAuthenticator @Inject constructor(
    private val nonceRestClient: NonceRestClient,
    private val discoveryWPAPIRestClient: DiscoveryWPAPIRestClient,
    private val siteSqlUtils: SiteSqlUtils,
    private val currentTimeProvider: CurrentTimeProvider
) {
    suspend fun <T : Payload<BaseNetworkError?>> makeAuthenticatedWPAPIRequest(
        site: SiteModel,
        fetchMethod: suspend (Nonce?) -> T
    ): T {
        val usingSavedRestUrl = site.wpApiRestUrl != null
        if (!usingSavedRestUrl) {
            site.wpApiRestUrl = discoveryWPAPIRestClient.discoverWPAPIBaseURL(site.url) // discover rest api endpoint
                ?: ReactNativeStore.slashJoin(
                    site.url,
                    "wp-json/"
                ) // fallback to ".../wp-json/" default if discovery fails
            (siteSqlUtils::insertOrUpdateSite)(site)
        }
        var nonce = nonceRestClient.getNonce(site)
        val usingSavedNonce = nonce is Available
        val failedRecently = nonce.failedRecently()
        if (nonce is Unknown || !(usingSavedNonce || failedRecently)) {
            nonce = nonceRestClient.requestNonce(site)
        }

        val response = fetchMethod(nonce)

        if (!response.isError) return response
        val statusCode = response.error?.volleyError?.networkResponse?.statusCode
        val errorCode = (response.error as? WPAPINetworkError)?.errorCode
        return when  {
            statusCode == STATUS_CODE_UNAUTHORIZED ||
                    (statusCode == STATUS_CODE_FORBIDDEN && errorCode == "rest_cookie_invalid_nonce") -> {
                if (usingSavedNonce) {
                    // Call with saved nonce failed, so try getting a new one
                    val previousNonce = nonce
                    val newNonce = nonceRestClient.requestNonce(site)

                    // Try original call again if we have a new nonce
                    val nonceIsUpdated = newNonce != null && newNonce != previousNonce
                    if (nonceIsUpdated) {
                        fetchMethod(newNonce)
                    } else {
                        response
                    }
                } else {
                    response
                }
            }
            statusCode == STATUS_CODE_NOT_FOUND -> {
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
            }
            // For all other failures just return the error response
            else -> response
        }
    }

    private fun Nonce?.failedRecently() = (this as? FailedRequest)?.timeOfResponse?.let {
        it + FIVE_MIN_MILLIS > currentTimeProvider.currentDate().time
    } == true

    companion object {
        private const val FIVE_MIN_MILLIS: Long = 5 * 60 * 1000
        private const val STATUS_CODE_NOT_FOUND = 404
        private const val STATUS_CODE_FORBIDDEN = 403
        private const val STATUS_CODE_UNAUTHORIZED = 401
    }
}
