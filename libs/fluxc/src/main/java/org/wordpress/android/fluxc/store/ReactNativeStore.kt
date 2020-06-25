package org.wordpress.android.fluxc.store

import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.google.gson.JsonElement
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.reactnative.Nonce
import org.wordpress.android.fluxc.network.rest.wpapi.reactnative.ReactNativeWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.reactnative.ReactNativeWPComRestClient
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Error
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Success
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import org.wordpress.android.fluxc.network.rest.wpapi.reactnative.Nonce.Available
import org.wordpress.android.fluxc.network.rest.wpapi.reactnative.Nonce.Unknown
import org.wordpress.android.fluxc.network.rest.wpapi.reactnative.Nonce.FailedRequest
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.SiteSqlUtils.DuplicateSiteException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This store is for use making calls that originate from React Native. It does not use
 * a higher-level api for the requests and responses because of the unique requirements
 * around React Native. Calls originating from native code should not use this class.
 */
@Singleton
class ReactNativeStore
@VisibleForTesting constructor(
    private val wpComRestClient: ReactNativeWPComRestClient,
    private val wpAPIRestClient: ReactNativeWPAPIRestClient,
    private val discoveryWPAPIRestClient: DiscoveryWPAPIRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val nonceMap: MutableMap<SiteModel, Nonce> = mutableMapOf(),
    private val currentTimeMillis: () -> Long,
    private val sitePersistanceFunction: (site: SiteModel) -> Int
) {
    @Inject constructor(
        wpComRestClient: ReactNativeWPComRestClient,
        wpAPIRestClient: ReactNativeWPAPIRestClient,
        discoveryWPAPIRestClient: DiscoveryWPAPIRestClient,
        coroutineEngine: CoroutineEngine
    ) : this(
            wpComRestClient,
            wpAPIRestClient,
            discoveryWPAPIRestClient,
            coroutineEngine,
            mutableMapOf(),
            System::currentTimeMillis,
            SiteSqlUtils::insertOrUpdateSite
    )

    private val WPCOM_ENDPOINT = "https://public-api.wordpress.com"

    suspend fun executeRequest(
        site: SiteModel,
        pathWithParams: String,
        enableCaching: Boolean = true
    ): ReactNativeFetchResponse =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "executeRequest") {
                return@withDefaultContext if (site.isUsingWpComRestApi) {
                    executeWPComRequest(site, pathWithParams, enableCaching)
                } else {
                    executeWPAPIRequest(site, pathWithParams, enableCaching)
                }
            }

    private suspend fun executeWPComRequest(
        site: SiteModel,
        path: String,
        enableCaching: Boolean
    ): ReactNativeFetchResponse {
        val (url, params) = parseUrlAndParamsForWPCom(path, site.siteId)
        return wpComRestClient.fetch(url, params, ::Success, ::Error, enableCaching)
    }

    private suspend fun executeWPAPIRequest(
        site: SiteModel,
        pathWithParams: String,
        enableCaching: Boolean
    ): ReactNativeFetchResponse {
        val (path, params) = parsePathAndParams(pathWithParams)
        return executeWPAPIRequest(site, path, params, enableCaching)
    }

    private suspend fun executeWPAPIRequest(
        site: SiteModel,
        path: String,
        params: Map<String, String>,
        enableCaching: Boolean
    ): ReactNativeFetchResponse {
        val usingSavedRestUrl = site.wpApiRestUrl != null
        if (!usingSavedRestUrl) {
            site.wpApiRestUrl = discoveryWPAPIRestClient.discoverWPAPIBaseURL(site.url) // discover rest api endpoint
                    ?: slashJoin(site.url, "wp-json/") // fallback to ".../wp-json/" default if discovery fails
            persistSiteSafely(site)
        }
        val fullRestUrl = slashJoin(site.wpApiRestUrl, path)

        val usingSavedNonce = nonceMap[site] is Available
        val failedRecently = true == (nonceMap[site] as? FailedRequest)?.timeOfResponse?.let {
            it + FIVE_MIN_MILLIS > currentTimeMillis()
        }
        if (nonceMap[site] is Unknown || !(usingSavedNonce || failedRecently)) {
            nonceMap[site] = wpAPIRestClient.requestNonce(site)
        }

        val response = executeFetch(fullRestUrl, params, nonceMap[site]?.value, enableCaching)
        return when (response) {
            is Success -> response

            is Error -> when (response.statusCode()) {
                401 -> {
                    if (usingSavedNonce) {
                        // Call with saved nonce failed, so try getting a new one
                        val previousNonce = nonceMap[site]?.value
                        nonceMap[site] = wpAPIRestClient.requestNonce(site)
                        val newNonce = nonceMap[site]?.value

                        // Try original call again if we have a new nonce
                        val nonceIsUpdated = newNonce != null && newNonce != previousNonce
                        if (nonceIsUpdated) {
                            return executeFetch(fullRestUrl, params, nonceMap[site]?.value, enableCaching)
                        }
                    }
                    response
                }

                404 -> {
                    // call failed with 'not found' so clear the (failing) rest url
                    site.wpApiRestUrl = null
                    persistSiteSafely(site)

                    if (usingSavedRestUrl) {
                        // If we did the previous call with a saved rest url, try again by making
                        // recursive call. This time there is no saved rest url to use
                        // so the rest url will be retrieved using discovery
                        executeWPAPIRequest(site, path, params, enableCaching)
                    } else {
                        // Already used discovery to fetch the rest base url and still got 'not found', so
                        // just return the error response
                        response
                    }

                    // For all other failures just return the error response
                }

                else -> response
            }
        }
    }

    private suspend fun executeFetch(
        fullRestApiUrl: String,
        params: Map<String, String>,
        nonce: String?,
        enableCaching: Boolean
    ): ReactNativeFetchResponse =
            wpAPIRestClient.fetch(fullRestApiUrl, params, ::Success, ::Error, nonce, enableCaching)

    private fun parseUrlAndParamsForWPCom(
        pathWithParams: String,
        wpComSiteId: Long
    ): Pair<String, Map<String, String>> =
            parsePathAndParams(pathWithParams).let { (path, params) ->
                val newPath = path
                        .replace("wp/v2".toRegex(), "wp/v2/sites/$wpComSiteId")
                val url = slashJoin(WPCOM_ENDPOINT, newPath)
                Pair(url, params)
            }

    private fun parsePathAndParams(pathWithParams: String): Pair<String, Map<String, String>> {
        val uri = Uri.parse(pathWithParams)
        val paramMap = uri.queryParameterNames.map { name ->
            name to requireNotNull(uri.getQueryParameter(name))
        }.toMap()
        return Pair(requireNotNull(uri.path), paramMap)
    }

    private fun persistSiteSafely(site: SiteModel) {
        try {
            sitePersistanceFunction.invoke(site)
        } catch (e: DuplicateSiteException) {
            // persistance failed, which is not a big deal because it just means we may need to re-discover the
            // rest url later.
            AppLog.d(AppLog.T.DB, "Error when persisting site: $e")
        }
    }

    companion object {
        private const val FIVE_MIN_MILLIS: Long = 5 * 60 * 1000

        /**
         * @param begin beginning which may end with a /
         * @param end ending which may begin with a /
         * @return a string with only a single slash "between" [begin] and [end],
         *          i.e. slashJoin("begin/", "/end") and slashJoin("begin", "end") both
         *          return "begin/end".
         *
         */
        fun slashJoin(begin: String, end: String): String {
            val noSlashBegin = begin.replace("/$".toRegex(), "")
            val noSlashEnd = end.replace("^/".toRegex(), "")
            return "$noSlashBegin/$noSlashEnd"
        }
    }
}

private fun Error.statusCode() = error.volleyError?.networkResponse?.statusCode

sealed class ReactNativeFetchResponse {
    class Success(val result: JsonElement?) : ReactNativeFetchResponse()
    class Error(val error: BaseNetworkError) : ReactNativeFetchResponse()
}
