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
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
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
    private val siteOptionsRsFetcher: SiteOptionsRsFetcher,
    dispatcher: Dispatcher,
    @Named(OkHttpClientQualifiers.CUSTOM_SSL) requestQueue: RequestQueue,
    userAgent: UserAgent
) : BaseWPAPIRestClient(dispatcher, requestQueue, userAgent) {
    companion object {
        private const val WOO_API_NAMESPACE_PREFIX = "wc/"
        private const val JETPACK_API_NAMESPACE_PREFIX = "jetpack/"
        private const val FETCH_API_CALL_FIELDS =
            "name,description,gmt_offset,timezone_string,url,authentication,namespaces"
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
     * replaces the stored row wholesale -- the write is a full-row update that excludes only the
     * application-password credential columns -- so this refreshes [previousSite] in place rather than
     * returning a fresh model. Anything left unset would otherwise be written back as its default,
     * including [SiteModel.getSelfHostedSiteId] (the blog id every XML-RPC call is addressed with) and
     * [SiteModel.isSelfHostedAdmin] (which gates Pages and Plugins) -- both set only by
     * `wp.getUsersBlogs` at sign-in, so neither could be recovered.
     *
     * [SiteModel.getOrigin] is likewise left as the stored row records it. An application-password site
     * whose sign-in went through `wp.getUsersBlogs` is stored as `ORIGIN_XMLRPC`, and origin still routes
     * `CommentsStore` and `SiteStore.fetchPostFormats`, where `ORIGIN_WPAPI` means "no XML-RPC available"
     * rather than "use REST". Which transport refreshes the row is decided by its credentials
     * (`SiteStore.fetchSite`); what the row *records* is deliberately not changed here. See CMM-2935.
     */
    suspend fun fetchWPAPISite(
        payload: FetchWPAPISitePayload,
        previousSite: SiteModel? = null
    ): SiteModel {
        val cleanedUrl = UrlUtils.addUrlSchemeIfNeeded(payload.url, false).let { urlWithScheme ->
            DiscoveryUtils.stripKnownPaths(urlWithScheme)
        }

        val (discoveredWpApiUrl, result) = requestRoot(cleanedUrl, previousSite)
        val urlScheme = discoveredWpApiUrl.toHttpUrl().scheme

        return when (result) {
            is Success -> {
                val response = result.data
                val jetpackPlugin = fetchJetpackPluginState(response?.namespaces, discoveredWpApiUrl, payload)
                val jetpackConnection = fetchJetpackConnectionState(jetpackPlugin, discoveredWpApiUrl, payload)
                // Only a stored row can be asked for its own options, because the request goes out with
                // the application password held on it. At sign-in there's no row yet; the refresh that
                // follows moments later, when the app opens, fills these in.
                val siteOptions = previousSite
                    ?.takeIf { it.isUsingSelfHostedRestApi }
                    ?.let { siteOptionsRsFetcher.fetch(it) }
                // The local id matters even on the fresh-model path: SiteStore.updateSite needs it to find
                // the stored row and preserve the editor preferences, and SiteSqlUtils needs it to match
                // the row by local id rather than by SITE_ID + URL -- that match misses, and inserts a
                // duplicate site, as soon as a real WP.com blog id is stored.
                (previousSite ?: SiteModel()).apply {
                    applyRootResponse(response, siteOptions, previousSite)
                    applyJetpackState(jetpackPlugin, jetpackConnection, previousSite)
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
     * Reads the REST root, preferring the endpoint the row already has over re-deriving one from the
     * site URL: that endpoint was verified when the site was added, and rediscovery costs a request and
     * resolves to nothing useful on a host whose home URL doesn't serve WordPress, or that strips the
     * `api.w.org` link header. Discovery still runs for a row that has no stored endpoint, and as a
     * retry when the stored one no longer answers -- otherwise a site that moved its REST root would be
     * pinned to the stale one, since a failed fetch writes nothing back. That retry can re-request the
     * same endpoint when discovery lands back on it, which only costs a request on a site that is
     * already failing.
     *
     * @return the endpoint the returned response came from, which is the one to persist.
     */
    private suspend fun requestRoot(
        cleanedUrl: String,
        previousSite: SiteModel?
    ): Pair<String, WPAPIResponse<RootWPAPIRestResponse>> {
        val storedUrl = previousSite?.wpApiRestUrl?.takeIf { it.isNotEmpty() }
        val storedResult = storedUrl?.let { requestRootFrom(it) }
        if (storedUrl != null && storedResult is Success) {
            return storedUrl to storedResult
        }
        val discoveredUrl = discoverApiEndpoint(cleanedUrl)
        return discoveredUrl to requestRootFrom(discoveredUrl)
    }

    private suspend fun requestRootFrom(url: String) = wpapiGsonRequestBuilder.syncGetRequest(
        restClient = this,
        url = url,
        clazz = RootWPAPIRestResponse::class.java,
        params = mapOf("_fields" to FETCH_API_CALL_FIELDS)
    )

    /**
     * Applies the REST root's fields and the separately-read [siteOptions] to the stored row. Every
     * assignment is guarded: a thin `_fields` response, or an option read the site refused, must leave
     * the stored value alone rather than blank it.
     */
    private fun SiteModel.applyRootResponse(
        response: RootWPAPIRestResponse?,
        siteOptions: SiteOptions?,
        previousSite: SiteModel?
    ) {
        response?.name?.takeIf { it.isNotEmpty() }?.let { name = it }
        response?.description?.let { description = it }
        // The index reports the Olson name unauthenticated, so prefer it over the numeric gmt_offset --
        // which can't express DST, and is what caused CMM-1095 -- and don't depend on the settings
        // endpoint, which only an administrator can read.
        (
            siteOptions?.timezone
                ?: response?.timezoneString?.takeIf { it.isNotEmpty() }
                ?: response?.gmtOffset
        )?.let { timezone = it }
        siteOptions?.defaultCommentStatus?.let { defaultCommentStatus = it }
        siteOptions?.isFeaturedImageSupported?.let { setIsFeaturedImageSupported(it) }
        // ORIGIN_WPAPI tells CommentsStore and SiteStore.fetchPostFormats that no XML-RPC is available,
        // so only heal a legacy ORIGIN_UNKNOWN row that has no verified XML-RPC endpoint to lose.
        if (previousSite == null ||
            (previousSite.origin == SiteModel.ORIGIN_UNKNOWN && previousSite.xmlRpcUrl.isNullOrEmpty())) {
            origin = SiteModel.ORIGIN_WPAPI
        }
        response?.namespaces?.let { namespaces ->
            hasWooCommerce = namespaces.any { it.startsWith(WOO_API_NAMESPACE_PREFIX) }
        }
        response?.authentication?.applicationPasswords?.endpoints?.authorization
            ?.takeIf { it.isNotEmpty() }
            ?.let { authorizeUrl ->
                applicationPasswordsAuthorizeUrl = authorizeUrl
                if (authorizeUrl.contains(APPLICATION_PASSWORDS_URL_SUFFIX)) {
                    // Infer the admin URL from the application passwords authorization URL
                    adminUrl = authorizeUrl.substringBefore(APPLICATION_PASSWORDS_URL_SUFFIX)
                }
            }
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
