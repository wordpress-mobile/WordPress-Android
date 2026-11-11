package org.wordpress.android.fluxc.network.rest.wpapi.site

import com.android.volley.RequestQueue
import com.google.gson.reflect.TypeToken
import okhttp3.Credentials
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
import org.wordpress.android.fluxc.network.rest.wpapi.jetpack.JetpackConnectionState
import org.wordpress.android.fluxc.network.rest.wpapi.jetpack.JetpackConnectionStatusFetcher
import org.wordpress.android.fluxc.network.rest.wpapi.plugin.PluginResponseModel
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
    private val jetpackConnectionStatusFetcher: JetpackConnectionStatusFetcher,
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
        private const val PLUGINS_PATH = "wp/v2/plugins"
        private const val PLUGINS_FIELDS = "plugin,status,version"
        private const val JETPACK_PLUGIN_SEARCH = "jetpack"
        private const val JETPACK_PLUGIN_ID = "jetpack/jetpack"
        private const val PLUGIN_STATUS_ACTIVE = "active"
        private const val PLUGIN_STATUS_NETWORK_ACTIVE = "network-active"
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
                val jetpackPlugin = fetchJetpackPluginState(response?.namespaces, discoveredWpApiUrl, payload)
                val jetpackConnection = fetchJetpackConnectionState(jetpackPlugin, discoveredWpApiUrl, payload)
                SiteModel().apply {
                    // Carry the local id so SiteStore.updateSite finds the stored row and preserves the
                    // editor preferences, and so SiteSqlUtils matches the row by local id rather than by
                    // SITE_ID + URL -- that match misses, and inserts a duplicate site, as soon as a real
                    // WP.com blog id is stored.
                    id = previousSite?.id ?: 0
                    name = response?.name
                    description = response?.description
                    timezone = response?.gmtOffset
                    origin = SiteModel.ORIGIN_WPAPI
                    hasWooCommerce = response?.namespaces?.any {
                        it.startsWith(WOO_API_NAMESPACE_PREFIX)
                    } ?: false
                    applyJetpackState(jetpackPlugin, jetpackConnection, previousSite)

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
        return fetchWPAPISite(
            payload = FetchWPAPISitePayload(
                url = site.url,
                username = site.getUserNameProcessed(),
                password = site.getPasswordProcessed(),
                isApplicationPassword =
                    site.hasApplicationPassword(),
            ),
            previousSite = site
        )
    }

    /**
     * The Jetpack plugin, as reported by the site itself. [isActive] is what
     * [SiteModel.isJetpackInstalled] documents -- installed *and* activated.
     */
    private data class JetpackPluginState(val isActive: Boolean, val version: String?)

    /**
     * Determines whether the site is running the Jetpack plugin, or null when it can't be determined.
     *
     * The `jetpack/` REST namespace is only a first filter: it's registered by the shared
     * `automattic/jetpack-connection` package, which also ships inside Jetpack Boost, Protect, Social and
     * VaultPress Backup, so its presence does *not* mean the Jetpack plugin is installed. What it does give
     * us for free is a reliable negative -- a namespace list without `jetpack/` means no active Jetpack --
     * which keeps the plugin lookup off the refresh path for the sites that have nothing to do with Jetpack.
     * That only holds when the list is present: an absent field is "couldn't read", not "no Jetpack".
     *
     * Reading the plugin list needs credentials and the `activate_plugins` capability, so it returns null for
     * sites without an application password and for users who aren't administrators. Callers must read null
     * as "unchanged", not as "not installed".
     */
    private suspend fun fetchJetpackPluginState(
        namespaces: List<String>?,
        apiRootUrl: String,
        payload: FetchWPAPISitePayload
    ): JetpackPluginState? = when {
        namespaces == null -> null
        namespaces.none { it.startsWith(JETPACK_API_NAMESPACE_PREFIX) } ->
            JetpackPluginState(isActive = false, version = null)
        payload.isApplicationPassword &&
            !payload.username.isNullOrEmpty() && !payload.password.isNullOrEmpty() ->
            requestJetpackPlugin(apiRootUrl, payload.username.orEmpty(), payload.password.orEmpty())
        else -> null
    }

    private suspend fun requestJetpackPlugin(
        apiRootUrl: String,
        username: String,
        password: String
    ): JetpackPluginState? {
        val result = wpapiGsonRequestBuilder.syncGetRequest<List<PluginResponseModel>>(
            restClient = this,
            url = apiRootUrl.trimEnd('/') + "/" + PLUGINS_PATH,
            params = mapOf("search" to JETPACK_PLUGIN_SEARCH, "_fields" to PLUGINS_FIELDS),
            type = object : TypeToken<List<PluginResponseModel>>() {}.type,
            headers = mapOf("Authorization" to Credentials.basic(username, password))
        )

        return when (result) {
            is Success -> {
                val jetpack = result.data?.firstOrNull { it.plugin == JETPACK_PLUGIN_ID }
                JetpackPluginState(
                    isActive = jetpack?.status == PLUGIN_STATUS_ACTIVE ||
                            jetpack?.status == PLUGIN_STATUS_NETWORK_ACTIVE,
                    version = jetpack?.version
                )
            }

            is Error -> null
        }
    }

    /**
     * Reads the site's Jetpack connection to WordPress.com, or null when it can't be determined. Only the
     * Jetpack plugin knows the blog ID WordPress.com assigned the site, and without it the row keeps
     * SITE_ID = 0 -- which is what makes a later /me/sites sync insert a duplicate site instead of matching
     * and upgrading this one. Skipped entirely when the plugin isn't there to ask.
     */
    private suspend fun fetchJetpackConnectionState(
        plugin: JetpackPluginState?,
        apiRootUrl: String,
        payload: FetchWPAPISitePayload
    ): JetpackConnectionState? {
        // isActive = true is only ever produced by requestJetpackPlugin, which already required the
        // application-password credentials, so no separate credential check is needed here.
        if (plugin?.isActive != true) return null
        val username = payload.username
        val password = payload.password
        return if (username != null && password != null) {
            jetpackConnectionStatusFetcher.fetch(apiRootUrl, username, password)
        } else {
            null
        }
    }

    /**
     * A null [plugin] or [connection] means that state couldn't be read on this run, in which case the
     * stored values are carried forward rather than reset. A non-null result is definitive, including
     * its null fields -- a disconnected site clears the blog id rather than keeping a stale one.
     *
     * Note that [SiteModel.isJetpackConnected] only says the site is connected to *some* WordPress.com
     * account, not necessarily the one signed in here -- callers that need the stronger claim have to check
     * account access separately. See CMM-2344.
     */
    private fun SiteModel.applyJetpackState(
        plugin: JetpackPluginState?,
        connection: JetpackConnectionState?,
        previousSite: SiteModel?
    ) {
        if (plugin != null) {
            setIsJetpackInstalled(plugin.isActive)
            jetpackVersion = plugin.version
        } else {
            setIsJetpackInstalled(previousSite?.isJetpackInstalled ?: false)
            jetpackVersion = previousSite?.jetpackVersion
        }
        when {
            // The plugin is definitively gone, so its WordPress.com connection is too.
            plugin != null && !plugin.isActive -> {
                setIsJetpackConnected(false)
                siteId = 0
            }
            connection != null -> {
                setIsJetpackConnected(connection.isConnected)
                siteId = connection.wpComSiteId ?: 0L
            }
            else -> {
                setIsJetpackConnected(previousSite?.isJetpackConnected ?: false)
                siteId = previousSite?.siteId ?: 0L
            }
        }
    }

    private fun discoverApiEndpoint(
        url: String
    ): String {
        return discoveryWPAPIRestClient.discoverWPAPIBaseURL(url) // discover rest api endpoint
            ?: WPAPIDiscoveryUtils.buildDefaultRESTBaseUrl(url)
    }
}
