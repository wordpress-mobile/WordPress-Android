package org.wordpress.android.fluxc.store

import kotlinx.coroutines.delay
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType.SITE
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Available
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.FailedRequest
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Unknown
import org.wordpress.android.fluxc.network.rest.wpapi.NonceRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.plugin.PluginWPAPIRestClient
import org.wordpress.android.fluxc.persistence.PluginSqlUtils
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.store.PluginStore.ConfigureSitePluginError
import org.wordpress.android.fluxc.store.PluginStore.DeleteSitePluginError
import org.wordpress.android.fluxc.store.PluginStore.DeleteSitePluginErrorType.UNKNOWN_PLUGIN
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginError
import org.wordpress.android.fluxc.store.PluginStore.OnPluginDirectoryFetched
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginConfigured
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginDeleted
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginInstalled
import org.wordpress.android.fluxc.store.PluginStore.PluginDirectoryError
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject

class PluginCoroutineStore
@Inject constructor(
    private val coroutineEngine: CoroutineEngine,
    private val dispatcher: Dispatcher,
    private val pluginWPAPIRestClient: PluginWPAPIRestClient,
    private val discoveryWPAPIRestClient: DiscoveryWPAPIRestClient,
    private val siteSqlUtils: SiteSqlUtils,
    private val nonceRestClient: NonceRestClient,
    private val currentTimeProvider: CurrentTimeProvider
) {
    fun fetchWPApiPlugins(siteModel: SiteModel) = coroutineEngine.launch(T.PLUGINS, this, "Fetching WPAPI plugins") {
        val event = syncFetchWPApiPlugins(siteModel)
        dispatcher.emitChange(event)
    }

    suspend fun syncFetchWPApiPlugins(
        siteModel: SiteModel
    ): OnPluginDirectoryFetched {
        val payload = executeWPAPIRequest(siteModel, true, pluginWPAPIRestClient::fetchPlugins)
        val event = OnPluginDirectoryFetched(SITE, false)
        if (payload.isError) {
            event.error = PluginDirectoryError(payload.error?.volleyError?.message, payload.error?.message)
        } else if (!payload.data.isNullOrEmpty()) {
            event.canLoadMore = false
            PluginSqlUtils.insertOrReplaceSitePlugins(siteModel, payload.data)
        }
        return event
    }

    fun deleteSitePlugin(site: SiteModel, pluginName: String, slug: String) =
            coroutineEngine.launch(T.PLUGINS, this, "Deleting WPAPI plugin") {
                val event = syncDeleteSitePlugin(site, slug, pluginName)
                dispatcher.emitChange(event)
            }

    suspend fun syncDeleteSitePlugin(
        site: SiteModel,
        pluginName: String,
        slug: String
    ): OnSitePluginDeleted {
        val plugin = PluginSqlUtils.getSitePluginBySlug(site, slug)
        val payload = executeWPAPIRequest(site, false) { siteModel, nonce, _ ->
            pluginWPAPIRestClient.deletePlugin(siteModel, nonce, plugin.name)
        }
        val event = OnSitePluginDeleted(payload.site, pluginName, slug)
        val error = payload.error?.let {
            DeleteSitePluginError(
                    payload.error?.volleyError?.message,
                    payload.error?.message
            )
        }
        if (error != null && error.type != UNKNOWN_PLUGIN) {
            event.error = error
        } else {
            PluginSqlUtils.deleteSitePlugin(payload.site, slug)
        }
        return event
    }

    fun configureSitePlugin(site: SiteModel, pluginName: String, slug: String, isActive: Boolean) =
            coroutineEngine.launch(T.PLUGINS, this, "Configuring WPAPI plugin") {
                val event = syncConfigureSitePlugin(site, pluginName, slug, isActive)
                dispatcher.emitChange(event)
            }

    suspend fun syncConfigureSitePlugin(
        site: SiteModel,
        pluginName: String,
        slug: String,
        isActive: Boolean
    ): OnSitePluginConfigured {
        val plugin = PluginSqlUtils.getSitePluginBySlug(site, slug)
        val payload = executeWPAPIRequest(site, false) { siteModel, nonce, _ ->
            pluginWPAPIRestClient.updatePlugin(siteModel, nonce, plugin.name, isActive)
        }
        val event = OnSitePluginConfigured(payload.site, pluginName, slug)
        if (payload.isError()) {
            event.error = ConfigureSitePluginError(payload.error?.volleyError?.message, payload.error?.message)
        } else {
            PluginSqlUtils.insertOrUpdateSitePlugin(payload.site, payload.data)
        }
        return event
    }

    fun installSitePlugin(site: SiteModel, slug: String) =
            coroutineEngine.launch(T.PLUGINS, this, "Installing WPAPI plugin") {
                syncInstallSitePlugin(site, slug)
            }

    suspend fun syncInstallSitePlugin(
        site: SiteModel,
        slug: String
    ): OnSitePluginInstalled {
        val payload = executeWPAPIRequest(site, false) { siteModel, nonce, _ ->
            pluginWPAPIRestClient.installPlugin(siteModel, nonce, slug)
        }
        val slug = payload.data?.slug
        val event = OnSitePluginInstalled(payload.site, payload.data?.slug ?: slug)
        if (payload.isError) {
            event.error = InstallSitePluginError(payload.error?.volleyError?.message, payload.error?.message)
        } else {
            PluginSqlUtils.insertOrUpdateSitePlugin(payload.site, payload.data)
        }
        dispatcher.emitChange(event)

        // Once the plugin is installed activate it and enable auto-updates
        if (!payload.isError && payload.data != null) {
            // Give a second to the server as otherwise the following configure call may fail
            delay(1000)
            syncConfigureSitePlugin(site, payload.data.name, payload.data.slug, true)
        }
        return event
    }

    private suspend fun <T : Payload<BaseNetworkError?>> executeWPAPIRequest(
        site: SiteModel,
        enableCaching: Boolean,
        fetchMethod: suspend (SiteModel, Nonce?, enableCaching: Boolean) -> T
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
        val usingSavedNonce = getNonce(site) is Available
        val failedRecently = true == (getNonce(site) as? FailedRequest)?.timeOfResponse?.let {
            it + FIVE_MIN_MILLIS > currentTimeProvider.currentDate().time
        }
        if (getNonce(site) is Unknown || !(usingSavedNonce || failedRecently)) {
            nonceRestClient.requestNonce(site)
        }

        val response = fetchMethod(site, getNonce(site), enableCaching)
        return when (response.isError) {
            false -> response
            else -> when (response.error?.volleyError?.networkResponse?.statusCode) {
                401 -> {
                    if (usingSavedNonce) {
                        // Call with saved nonce failed, so try getting a new one
                        val previousNonce = getNonce(site)?.value
                        nonceRestClient.requestNonce(site)
                        val newNonce = getNonce(site)?.value

                        // Try original call again if we have a new nonce
                        val nonceIsUpdated = newNonce != null && newNonce != previousNonce
                        if (nonceIsUpdated) {
                            return fetchMethod(site, getNonce(site), enableCaching)
                        }
                    }
                    response
                }

                404 -> {
                    // call failed with 'not found' so clear the (failing) rest url
                    site.wpApiRestUrl = null
                    (siteSqlUtils::insertOrUpdateSite)(site)

                    if (usingSavedRestUrl) {
                        // If we did the previous call with a saved rest url, try again by making
                        // recursive call. This time there is no saved rest url to use
                        // so the rest url will be retrieved using discovery
                        executeWPAPIRequest(site, enableCaching, fetchMethod)
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

    private fun getNonce(site: SiteModel) = nonceRestClient.getNonce(site)

    class WPApiPluginsPayload<T>(
        val site: SiteModel?,
        val data: T?
    ) : Payload<BaseNetworkError?>() {
        constructor(error: BaseNetworkError) : this(null, null) {
            this.error = error
        }
    }

    companion object {
        private const val FIVE_MIN_MILLIS: Long = 5 * 60 * 1000
    }
}
