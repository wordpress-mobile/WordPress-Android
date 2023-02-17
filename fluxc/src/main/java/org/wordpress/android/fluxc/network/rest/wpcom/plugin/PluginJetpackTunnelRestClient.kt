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
import org.wordpress.android.fluxc.store.PluginStore.FetchedSitePluginPayload
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginError
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
                        it.type,
                        it.message
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
        runInstallPlugin(
            site,
            pluginSlug,
            mapOf("slug" to pluginSlug),
            false
        )
    }

    fun installJetpackOnIndividualPluginSite(site: SiteModel) {
        runInstallPlugin(
            site,
            "jetpack",
            mapOf("slug" to "jetpack", "status" to "active"),
            true
        )
    }

    private fun runInstallPlugin(
        site: SiteModel,
        pluginSlug: String,
        body: Map<String, String>,
        isJetpackIndividualPluginScenario: Boolean
    ) {
        val url = WPAPI.plugins.urlV2

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
                        dispatcher.dispatch(if (isJetpackIndividualPluginScenario) {
                            PluginActionBuilder.newInstalledJpForIndividualPluginSiteAction(payload)
                        } else {
                            PluginActionBuilder.newInstalledSitePluginAction(payload)
                        })
                    }
                },
                { error ->
                    val installError = InstallSitePluginError(error)
                    val payload = InstalledSitePluginPayload(site, pluginSlug, installError)
                    dispatcher.dispatch(if (isJetpackIndividualPluginScenario) {
                        PluginActionBuilder.newInstalledJpForIndividualPluginSiteAction(payload)
                    } else {
                        PluginActionBuilder.newInstalledSitePluginAction(payload)
                    })
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
                    val configurePluginError = ConfigureSitePluginError(error, active)

                    val payload = ConfiguredSitePluginPayload(site, pluginName, configurePluginError)
                    dispatcher.dispatch(PluginActionBuilder.newConfiguredSitePluginAction(payload))
                }
        )
        add(request)
    }
}
