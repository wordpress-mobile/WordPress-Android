package org.wordpress.android.fluxc.store

import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.google.gson.JsonElement
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Available
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.FailedRequest
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Unknown
import org.wordpress.android.fluxc.network.rest.wpapi.NonceRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.reactnative.ReactNativeWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.reactnative.ReactNativeWPComRestClient
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.SiteSqlUtils.DuplicateSiteException
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Error
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Success
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton

private const val WPCOM_ENDPOINT = "https://public-api.wordpress.com"

/**
 * This store is for use making calls that originate from React Native. It does not use
 * a higher-level api for the requests and responses because of the unique requirements
 * around React Native. Calls originating from native code should not use this class.
 */
@Singleton
class ReactNativeStore @VisibleForTesting constructor(
    private val wpComRestClient: ReactNativeWPComRestClient,
    private val wpAPIRestClient: ReactNativeWPAPIRestClient,
    private val nonceRestClient: NonceRestClient,
    private val discoveryWPAPIRestClient: DiscoveryWPAPIRestClient,
    private val siteSqlUtils: SiteSqlUtils,
    private val coroutineEngine: CoroutineEngine,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val sitePersistanceFunction: (site: SiteModel) -> Int = siteSqlUtils::insertOrUpdateSite,
    private val uriParser: (string: String) -> Uri = Uri::parse
) {
    @Inject constructor(
        wpComRestClient: ReactNativeWPComRestClient,
        wpAPIRestClient: ReactNativeWPAPIRestClient,
        nonceRestClient: NonceRestClient,
        discoveryWPAPIRestClient: DiscoveryWPAPIRestClient,
        siteSqlUtils: SiteSqlUtils,
        coroutineEngine: CoroutineEngine
    ) : this(
            wpComRestClient,
            wpAPIRestClient,
            nonceRestClient,
            discoveryWPAPIRestClient,
            siteSqlUtils,
            coroutineEngine,
            System::currentTimeMillis,
            siteSqlUtils::insertOrUpdateSite,
            Uri::parse
    )

    private enum class RequestMethod {
        GET,
        POST
    }

    suspend fun executeGetRequest(
        site: SiteModel,
        pathWithParams: String,
        enableCaching: Boolean = true
    ): ReactNativeFetchResponse =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "executeGetRequest") {
                return@withDefaultContext if (site.isUsingWpComRestApi) {
                    executeWPComGetRequest(site, pathWithParams, enableCaching)
                } else {
                    executeWPAPIGetRequest(site, pathWithParams, enableCaching)
                }
            }

    suspend fun executePostRequest(
        site: SiteModel,
        pathWithParams: String,
        body: Map<String, Any> = emptyMap(),
    ): ReactNativeFetchResponse =
        coroutineEngine.withDefaultContext(AppLog.T.API, this, "executePostRequest") {
            return@withDefaultContext if (site.isUsingWpComRestApi) {
                executeWPComPostRequest(site, pathWithParams, body)
            } else {
                executeWPAPIPostRequest(site, pathWithParams, body)
            }
        }

    /**
     * WPCOM REST API
     */
    private suspend fun executeWPComGetRequest(
        site: SiteModel,
        path: String,
        enableCaching: Boolean
    ): ReactNativeFetchResponse {
        val (url, params) = parseUrlAndParamsForWPCom(path, site.siteId)
        return if (url != null) {
            wpComRestClient.getRequest(url, params, ::Success, ::Error, enableCaching)
        } else {
            urlParseError(path)
        }
    }

    private suspend fun executeWPComPostRequest(
        site: SiteModel,
        path: String,
        body: Map<String, Any>,
    ): ReactNativeFetchResponse {
        val (url, params) = parseUrlAndParamsForWPCom(path, site.siteId)
        return if (url != null) {
            wpComRestClient.postRequest(url, params, body, ::Success, ::Error)
        } else {
            urlParseError(path)
        }
    }

    /**
     * WP REST PI
     */
    private suspend fun executeWPAPIGetRequest(
        site: SiteModel,
        pathWithParams: String,
        enableCaching: Boolean
    ): ReactNativeFetchResponse {
        val (path, params) = parsePathAndParams(pathWithParams)
        return if (path != null) {
            // Omit `body` parameters as it's only supported in POST requests
            executeWPAPIRequest(site, path, RequestMethod.GET, params, emptyMap(), enableCaching)
        } else {
            urlParseError(pathWithParams)
        }
    }

    private suspend fun executeWPAPIPostRequest(
        site: SiteModel,
        pathWithParams: String,
        body: Map<String, Any>,
    ): ReactNativeFetchResponse {
        val (path, params) = parsePathAndParams(pathWithParams)
        return if (path != null) {
            // Omit `params` and `enableCaching` parameters as they are only supported in GET requests
            executeWPAPIRequest(site, path, RequestMethod.POST, emptyMap(), body, false)
        } else {
            urlParseError(pathWithParams)
        }
    }

    private fun urlParseError(path: String): Error {
        val error = BaseNetworkError(GenericErrorType.UNKNOWN).apply {
            message = "Failed to parse URI from $path"
        }
        return Error(error)
    }

    @Suppress("ComplexMethod", "NestedBlockDepth", "LongParameterList")
    private suspend fun executeWPAPIRequest(
        site: SiteModel,
        path: String,
        method: RequestMethod,
        params: Map<String, String>,
        body: Map<String, Any>,
        enableCaching: Boolean
    ): ReactNativeFetchResponse {
        // Storing this in a variable to avoid a NPE that can occur if the site object is mutated
        // from another thread: https://github.com/wordpress-mobile/WordPress-FluxC-Android/issues/1579
        var wpApiRestUrl = site.wpApiRestUrl

        val usingSavedRestUrl = wpApiRestUrl != null
        if (!usingSavedRestUrl) {
            wpApiRestUrl = discoveryWPAPIRestClient.discoverWPAPIBaseURL(site.url) // discover rest api endpoint
                    ?: slashJoin(site.url, "wp-json/") // fallback to ".../wp-json/" default if discovery fails
            site.wpApiRestUrl = wpApiRestUrl
            persistSiteSafely(site)
        }
        val fullRestUrl = slashJoin(wpApiRestUrl, path)

        var nonce = nonceRestClient.getNonce(site)
        val usingSavedNonce = nonce is Available
        val failedRecently = true == (nonce as? FailedRequest)?.timeOfResponse?.let {
            it + FIVE_MIN_MILLIS > currentTimeMillis()
        }
        if (nonce is Unknown || !(usingSavedNonce || failedRecently)) {
            nonce = nonceRestClient.requestNonce(site)
        }

        val response = when (method) {
            RequestMethod.GET -> executeGet(fullRestUrl, params, nonce?.value, enableCaching)
            RequestMethod.POST -> executePost(fullRestUrl, body, nonce?.value)
        }
        return when (response) {
            is Success -> response

            is Error -> when (response.statusCode()) {
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    if (usingSavedNonce) {
                        // Call with saved nonce failed, so try getting a new one
                        val previousNonce = nonce?.value
                        val newNonce = nonceRestClient.requestNonce(site)?.value

                        // Try original call again if we have a new nonce
                        val nonceIsUpdated = newNonce != null && newNonce != previousNonce
                        if (nonceIsUpdated) {
                            return when (method) {
                                RequestMethod.GET -> executeGet(fullRestUrl, params, newNonce, enableCaching)
                                RequestMethod.POST -> executePost(fullRestUrl, body, newNonce)
                            }
                        }
                    }
                    response
                }

                HttpURLConnection.HTTP_NOT_FOUND -> {
                    // call failed with 'not found' so clear the (failing) rest url
                    site.wpApiRestUrl = null
                    persistSiteSafely(site)

                    if (usingSavedRestUrl) {
                        // If we did the previous call with a saved rest url, try again by making
                        // recursive call. This time there is no saved rest url to use
                        // so the rest url will be retrieved using discovery
                        executeWPAPIRequest(site, path, method, params, body, enableCaching)
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

    private suspend fun executeGet(
        fullRestApiUrl: String,
        params: Map<String, String>,
        nonce: String?,
        enableCaching: Boolean
    ): ReactNativeFetchResponse =
            wpAPIRestClient.getRequest(fullRestApiUrl, params, ::Success, ::Error, nonce, enableCaching)

    private suspend fun executePost(
        fullRestApiUrl: String,
        body: Map<String, Any>,
        nonce: String?
    ): ReactNativeFetchResponse =
        wpAPIRestClient.postRequest(fullRestApiUrl, body, ::Success, ::Error, nonce)

    private fun parseUrlAndParamsForWPCom(
        pathWithParams: String,
        wpComSiteId: Long
    ): Pair<String?, Map<String, String>> =
            parsePathAndParams(pathWithParams).let { (path, params) ->
                val url = path?.let {
                    val newPath = it
                            .replace("wp/v2".toRegex(), "wp/v2/sites/$wpComSiteId")
                            .replace("wpcom/v2".toRegex(), "wpcom/v2/sites/$wpComSiteId")
                            .replace("wp-block-editor/v1".toRegex(), "wp-block-editor/v1/sites/$wpComSiteId")
                            .replace("oembed/1.0".toRegex(), "oembed/1.0/sites/$wpComSiteId")
                    slashJoin(WPCOM_ENDPOINT, newPath)
                }
                Pair(url, params)
            }

    private fun parsePathAndParams(pathWithParams: String): Pair<String?, Map<String, String>> {
        val uri = uriParser(pathWithParams)
        val paramMap = uri.queryParameterNames.mapNotNull { key ->
            uri.getQueryParameter(key)?.let { value ->
                key to value
            }
        }.toMap()
        return Pair(uri.path, paramMap)
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

    private fun getNonce(site: SiteModel) = nonceRestClient.getNonce(site)

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
