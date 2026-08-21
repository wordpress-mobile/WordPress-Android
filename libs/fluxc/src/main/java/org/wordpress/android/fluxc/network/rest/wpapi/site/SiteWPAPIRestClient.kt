package org.wordpress.android.fluxc.network.rest.wpapi.site

import com.android.volley.RequestQueue
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.discovery.DiscoveryUtils
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient
import org.wordpress.android.fluxc.network.discovery.RootWPAPIRestResponse
import org.wordpress.android.fluxc.network.rest.wpapi.BaseWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIDiscoveryUtils
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.store.SiteStore.FetchWPAPISitePayload
import org.wordpress.android.fluxc.utils.extensions.getPasswordProcessed
import org.wordpress.android.fluxc.utils.extensions.getUserNameProcessed
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.fluxc.module.OkHttpClientQualifiers
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SiteWPAPIRestClient @Inject constructor(
    private val wpapiGsonRequestBuilder: WPAPIGsonRequestBuilder,
    private val discoveryWPAPIRestClient: DiscoveryWPAPIRestClient,
    dispatcher: Dispatcher,
    @Named(OkHttpClientQualifiers.CUSTOM_SSL) requestQueue: RequestQueue,
    userAgent: UserAgent
) : BaseWPAPIRestClient(dispatcher, requestQueue, userAgent) {
    companion object {
        private const val WOO_API_NAMESPACE_PREFIX = "wc/"
        private const val JETPACK_API_NAMESPACE_PREFIX = "jetpack/"
        private const val FETCH_API_CALL_FIELDS =
            "name,description,gmt_offset,url,authentication,namespaces"
        private const val APPLICATION_PASSWORDS_URL_SUFFIX = "authorize-application.php"
    }

    /**
     * @param previousSite the site this fetch is refreshing, when there is one. The model returned here
     * replaces the stored row wholesale, so fields this fetch can't determine are carried forward from it
     * rather than reset.
     */
    suspend fun fetchWPAPISite(
        payload: FetchWPAPISitePayload,
        previousSite: SiteModel? = null
    ): SiteModel {
        val cleanedUrl = UrlUtils.addUrlSchemeIfNeeded(payload.url, false).let { urlWithScheme ->
            DiscoveryUtils.stripKnownPaths(urlWithScheme)
        }

        val discoveredWpApiUrl = discoverApiEndpoint(cleanedUrl)
        val urlScheme = discoveredWpApiUrl.toHttpUrl().scheme

        val result = wpapiGsonRequestBuilder.syncGetRequest(
            restClient = this,
            url = discoveredWpApiUrl,
            clazz = RootWPAPIRestResponse::class.java,
            params = mapOf("_fields" to FETCH_API_CALL_FIELDS)
        )

        return when (result) {
            is Success -> {
                val response = result.data
                SiteModel().apply {
                    name = response?.name
                    description = response?.description
                    timezone = response?.gmtOffset
                    origin = SiteModel.ORIGIN_WPAPI
                    hasWooCommerce = response?.namespaces?.any {
                        it.startsWith(WOO_API_NAMESPACE_PREFIX)
                    } ?: false
                    applyJetpackState(response?.namespaces, previousSite)

                    applicationPasswordsAuthorizeUrl = response?.authentication?.applicationPasswords
                        ?.endpoints?.authorization
                    if (!applicationPasswordsAuthorizeUrl.isNullOrEmpty() &&
                        applicationPasswordsAuthorizeUrl.contains(APPLICATION_PASSWORDS_URL_SUFFIX)) {
                        // Infer the admin URL from the application passwords authorization URL
                        adminUrl = applicationPasswordsAuthorizeUrl.substringBefore(APPLICATION_PASSWORDS_URL_SUFFIX)
                    }

                    wpApiRestUrl = discoveredWpApiUrl
                    this.url = cleanedUrl.replaceBefore("://", urlScheme)
                    if (payload.isApplicationPassword) {
                        this.apiRestUsernamePlain = payload.username
                        this.apiRestPasswordPlain = payload.password
                    } else {
                        this.username = payload.username
                        this.password = payload.password
                    }
                }
            }

            is Error -> {
                SiteModel().apply {
                    error = result.error
                }
            }
        }
    }

    suspend fun fetchWPAPISite(
        site: SiteModel
    ): SiteModel {
        val fetchedSite = fetchWPAPISite(
            payload = FetchWPAPISitePayload(
                url = site.url,
                username = site.getUserNameProcessed(),
                password = site.getPasswordProcessed(),
                isApplicationPassword =
                    site.hasApplicationPassword(),
            ),
            previousSite = site
        )

        if (!fetchedSite.isError) {
            // Carry the local id so SiteStore.updateSite finds the stored row and preserves the editor
            // preferences, and so SiteSqlUtils matches the row by local id rather than by SITE_ID + URL --
            // that match misses, and inserts a duplicate site, as soon as a real WP.com blog id is stored.
            fetchedSite.id = site.id
        }
        return fetchedSite
    }

    /**
     * Jetpack registers its REST namespace only while the plugin is active, which is exactly what
     * [SiteModel.isJetpackInstalled] documents. The connection to WordPress.com isn't visible in the REST
     * root at all -- it's established by the in-app connection flow, which persists it directly -- so it's
     * carried forward here rather than reset, and cleared only when Jetpack itself is gone.
     */
    private fun SiteModel.applyJetpackState(namespaces: List<String>?, previousSite: SiteModel?) {
        val hasJetpack = namespaces?.any { it.startsWith(JETPACK_API_NAMESPACE_PREFIX) } ?: false
        val carriedForward = previousSite?.takeIf { hasJetpack }
        setIsJetpackInstalled(hasJetpack)
        setIsJetpackConnected(carriedForward?.isJetpackConnected == true)
        siteId = carriedForward?.siteId ?: 0L
    }

    private fun discoverApiEndpoint(
        url: String
    ): String {
        return discoveryWPAPIRestClient.discoverWPAPIBaseURL(url) // discover rest api endpoint
            ?: WPAPIDiscoveryUtils.buildDefaultRESTBaseUrl(url)
    }
}
