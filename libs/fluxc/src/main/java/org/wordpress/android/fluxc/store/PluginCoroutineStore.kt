package org.wordpress.android.fluxc.store

import kotlinx.coroutines.delay
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType.SITE
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIAuthenticator
import org.wordpress.android.fluxc.network.rest.wpapi.plugin.PluginWPAPIRestClient
import org.wordpress.android.fluxc.persistence.PluginSqlUtilsWrapper
import org.wordpress.android.fluxc.store.PluginStore.ConfigureSitePluginError
import org.wordpress.android.fluxc.store.PluginStore.DeleteSitePluginError
import org.wordpress.android.fluxc.store.PluginStore.DeleteSitePluginErrorType.UNKNOWN_PLUGIN
import org.wordpress.android.fluxc.store.PluginStore.FetchSitePluginError
import org.wordpress.android.fluxc.store.PluginStore.FetchedSitePluginPayload
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginError
import org.wordpress.android.fluxc.store.PluginStore.OnPluginDirectoryFetched
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginConfigured
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginDeleted
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginFetched
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginInstalled
import org.wordpress.android.fluxc.store.PluginStore.PluginDirectoryError
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject

private const val PLUGIN_CONFIGURATION_DELAY = 1000L

class PluginCoroutineStore
@Inject constructor(
    private val coroutineEngine: CoroutineEngine,
    private val dispatcher: Dispatcher,
    private val pluginWPAPIRestClient: PluginWPAPIRestClient,
    private val wpapiAuthenticator: WPAPIAuthenticator,
    private val pluginSqlUtils: PluginSqlUtilsWrapper
) {
    fun fetchWPApiPlugins(siteModel: SiteModel) =
        coroutineEngine.launch(T.PLUGINS, this, "Fetching WPAPI plugins") {
            val event = syncFetchWPApiPlugins(siteModel)
            dispatcher.emitChange(event)
        }

    suspend fun syncFetchWPApiPlugins(
        siteModel: SiteModel
    ): OnPluginDirectoryFetched {
        val payload = wpapiAuthenticator.makeAuthenticatedWPAPIRequest(siteModel) { nonce ->
            pluginWPAPIRestClient.fetchPlugins(siteModel, nonce)
        }
        val event = OnPluginDirectoryFetched(SITE, false)
        val error = payload.error
        if (error != null) {
            event.error = PluginDirectoryError(error.type, error.message)
        } else if (!payload.data.isNullOrEmpty()) {
            event.canLoadMore = false
            pluginSqlUtils.insertOrReplaceSitePlugins(siteModel, payload.data)
        }
        return event
    }

    fun fetchWPApiPlugin(site: SiteModel, pluginName: String) =
        coroutineEngine.launch(T.PLUGINS, this, "Fetching WPAPI plugin") {
            val event = syncFetchWPApiPlugin(site, pluginName)
            dispatcher.emitChange(event)
        }

    suspend fun syncFetchWPApiPlugin(site: SiteModel, pluginName: String): OnSitePluginFetched {
        val payload = wpapiAuthenticator.makeAuthenticatedWPAPIRequest(site) { nonce ->
            pluginWPAPIRestClient.fetchPlugin(site, nonce, pluginName)
        }
        val error = payload.error
        return if (error != null) {
            val fetchError = FetchSitePluginError(error.type, error.message)
            OnSitePluginFetched(FetchedSitePluginPayload(pluginName, fetchError))
                .apply {
                    this.error = fetchError
                }
        } else {
            pluginSqlUtils.insertOrUpdateSitePlugin(site, payload.data)
            OnSitePluginFetched(
                FetchedSitePluginPayload(payload.data)
            )
        }
    }

    fun deleteSitePlugin(site: SiteModel, pluginName: String, slug: String) =
            coroutineEngine.launch(T.PLUGINS, this, "Deleting WPAPI plugin") {
                val event = syncDeleteSitePlugin(site, pluginName, slug)
                dispatcher.emitChange(event)
            }

    suspend fun syncDeleteSitePlugin(
        site: SiteModel,
        pluginName: String,
        slug: String
    ): OnSitePluginDeleted {
        val plugin = pluginSqlUtils.getSitePluginBySlug(site, slug)
        val payload = wpapiAuthenticator.makeAuthenticatedWPAPIRequest(site) { nonce ->
            pluginWPAPIRestClient.deletePlugin(site, nonce, plugin?.name ?: pluginName)
        }
        val event = OnSitePluginDeleted(payload.site, pluginName, slug)
        val error = payload.error?.let {
            DeleteSitePluginError(it.type, it.message)
        }
        if (error != null && error.type != UNKNOWN_PLUGIN) {
            event.error = error
        } else {
            pluginSqlUtils.deleteSitePlugin(site, slug)
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
        val plugin = pluginSqlUtils.getSitePluginBySlug(site, slug)
        val payload = wpapiAuthenticator.makeAuthenticatedWPAPIRequest(site) { nonce, ->
            pluginWPAPIRestClient.updatePlugin(site, nonce, plugin?.name ?: pluginName, isActive)
        }
        val event = OnSitePluginConfigured(payload.site, pluginName, slug)
        val error = payload.error
        if (error != null) {
            event.error = ConfigureSitePluginError(error, isActive)
        } else {
            pluginSqlUtils.insertOrUpdateSitePlugin(site, payload.data)
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
        val payload = wpapiAuthenticator.makeAuthenticatedWPAPIRequest(site) { nonce ->
            pluginWPAPIRestClient.installPlugin(site, nonce, slug)
        }
        val event = OnSitePluginInstalled(payload.site, payload.data?.slug ?: slug)
        val error = payload.error
        if (error != null) {
            event.error = InstallSitePluginError(error)
        } else {
            pluginSqlUtils.insertOrUpdateSitePlugin(site, payload.data)
        }
        dispatcher.emitChange(event)

        // Once the plugin is installed activate it and enable auto-updates
        if (!payload.isError && payload.data != null) {
            // Give a second to the server as otherwise the following configure call may fail
            delay(PLUGIN_CONFIGURATION_DELAY)
            val configureEvent = syncConfigureSitePlugin(site, payload.data.name, payload.data.slug, true)
            dispatcher.emitChange(configureEvent)
        }
        return event
    }

    class WPApiPluginsPayload<T>(
        val site: SiteModel?,
        val data: T?
    ) : Payload<BaseNetworkError?>() {
        constructor(error: BaseNetworkError) : this(null, null) {
            this.error = error
        }
    }
}
