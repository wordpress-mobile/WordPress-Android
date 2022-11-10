package org.wordpress.android.fluxc.network.rest.wpcom.plugin

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PluginActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WPAPI
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.plugin.PluginResponseModel
import org.wordpress.android.fluxc.network.rest.wpapi.plugin.toDomainModel
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequest
import org.wordpress.android.fluxc.store.PluginStore.ConfigureSitePluginError
import org.wordpress.android.fluxc.store.PluginStore.ConfiguredSitePluginPayload
import org.wordpress.android.fluxc.store.PluginStore.FetchSitePluginError
import org.wordpress.android.fluxc.store.PluginStore.FetchSitePluginErrorType.PLUGIN_DOES_NOT_EXIST
import org.wordpress.android.fluxc.store.PluginStore.FetchedSitePluginPayload
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginError
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginErrorType.PLUGIN_ALREADY_INSTALLED
import org.wordpress.android.fluxc.store.PluginStore.InstalledSitePluginPayload
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PluginJetpackTunnelRestClient @Inject constructor(
    private val dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    companion object {
        private const val PLUGIN_ALREADY_EXISTS = "Destination folder already exists."
    }

    /**
     * Fetch a plugin's information from a site.
     *
     * @param [pluginName] This should use the value of the `plugin` key inside a plugin object as returned
     * by the `GET wp/v2/plugins` endpoint. For example, for Jetpack, the correct value is `jetpack/jetpack`.
     */
    fun fetchPlugin(site: SiteModel, pluginName: String) {
        val url = WPAPI.plugins.name(pluginName).urlV2

        val request = JetpackTunnelGsonRequest.buildGetRequest(
                url,
                site.siteId,
                emptyMap(),
                PluginResponseModel::class.java,
                { response: PluginResponseModel? ->
                    response?.let {
                        val payload = FetchedSitePluginPayload(
                            it.toDomainModel(site.id)
                        )
                        dispatcher.dispatch(PluginActionBuilder.newFetchedSitePluginAction(payload))
                    }
                },
                {
                    val fetchError = FetchSitePluginError(
                        PLUGIN_DOES_NOT_EXIST
                    )
                    val payload = FetchedSitePluginPayload(
                        pluginName,
                        fetchError
                    )
                    dispatcher.dispatch(PluginActionBuilder.newFetchedSitePluginAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) }
        )
        add(request)
    }

    /**
     * Install a plugin to a site.
     *
     * @param [pluginSlug] This should use the value of the plugin's URL slug on the plugin directory, in the form of
     * https://wordpress.org/plugins/<slug>. For example, for Jetpack, the value is 'jetpack'.
     */
    fun installPlugin(site: SiteModel, pluginSlug: String) {
        val url = WPAPI.plugins.urlV2
        val body = mapOf(
                "slug" to pluginSlug
        )

        val request = JetpackTunnelGsonRequest.buildPostRequest(
                url,
                site.siteId,
                body,
                PluginResponseModel::class.java,
                { response: PluginResponseModel? ->
                    response?.let {
                        val payload = InstalledSitePluginPayload(
                                site,
                                it.toDomainModel(site.id)
                        )
                        dispatcher.dispatch(PluginActionBuilder.newInstalledSitePluginAction(payload))
                    }
                },
                { error ->
                    val installError = when (error.message) {
                        PLUGIN_ALREADY_EXISTS -> {
                            InstallSitePluginError(
                                    PLUGIN_ALREADY_INSTALLED,
                                    error.message
                            )
                        }
                        else -> {
                            InstallSitePluginError(
                                    GENERIC_ERROR,
                                    error.message
                            )
                        }
                    }
                    val payload = InstalledSitePluginPayload(site, pluginSlug, installError)
                    dispatcher.dispatch(PluginActionBuilder.newInstalledSitePluginAction(payload))
                }
        )
        add(request)
    }

    /**
     * Configure a plugin's status in a site. This supports making it 'active', or 'inactive'. The API also supports
     * the 'network-active' status, but it is not supported yet here.
     *
     * @param [pluginName] This should use the value of the `plugin` key inside a plugin object as returned
     * by the `GET wp/v2/plugins` endpoint. For example, for Jetpack, the correct value is `jetpack/jetpack`.
     */
    fun configurePlugin(site: SiteModel, pluginName: String, active: Boolean) {
        val url = WPAPI.plugins.name(pluginName).urlV2
        val body = mapOf(
                "status" to if (active) "active" else "inactive"
        )

        val request = JetpackTunnelGsonRequest.buildPostRequest(
                url,
                site.siteId,
                body,
                PluginResponseModel::class.java,
                { response: PluginResponseModel? ->
                    response?.let {
                        val payload = ConfiguredSitePluginPayload(
                                site,
                                it.toDomainModel(site.id)
                        )
                        dispatcher.dispatch(PluginActionBuilder.newConfiguredSitePluginAction(payload))
                    }
                },
                { error ->

                    val configurePluginError = ConfigureSitePluginError(
                            error.apiError, error.message
                    )

                    val payload = ConfiguredSitePluginPayload(site, pluginName, configurePluginError)
                    dispatcher.dispatch(PluginActionBuilder.newConfiguredSitePluginAction(payload))
                }
        )
        add(request)
    }
}
