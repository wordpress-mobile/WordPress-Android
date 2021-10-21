package org.wordpress.android.fluxc.network.rest.wpcom.plugin

import android.content.Context
import com.android.volley.RequestQueue
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.plugin.PluginResponseModel
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginError
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginErrorType
import org.wordpress.android.fluxc.store.PluginStore.InstalledSitePluginPayload
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PluginJetpackTunnelRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    companion object {
        private const val PLUGINS_API_PATH = "/wp/v2/plugins"
        private const val INACTIVE_STATUS = "inactive"
        private const val PLUGIN_ALREADY_EXISTS = "Destination folder already exists."
    }

    suspend fun fetchPlugin(site: SiteModel, pluginSlug: String): InstalledSitePluginPayload {
        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                restClient = this,
                site = site,
                url = "$PLUGINS_API_PATH/$pluginSlug",
                params = emptyMap(),
                PluginResponseModel::class.java
        )

        return when (response) {
            is JetpackSuccess -> InstalledSitePluginPayload(
                    site,
                    sitePluginModelFromResponse(site, response.data!!)
            )

            is JetpackError -> {
                InstalledSitePluginPayload(
                        site,
                        pluginSlug,
                        InstallSitePluginError(
                                InstallSitePluginErrorType.NOT_AVAILABLE,
                                response.error.message
                        )
                )
            }
        }
    }

    suspend fun installPlugin(site: SiteModel, pluginSlug: String): InstalledSitePluginPayload {
        val body = mapOf(
                "slug" to pluginSlug
        )

        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
                this,
                site,
                PLUGINS_API_PATH,
                body,
                PluginResponseModel::class.java
        )
        return when (response) {
            is JetpackSuccess -> InstalledSitePluginPayload(
                    site,
                    sitePluginModelFromResponse(site, response.data!!)
            )

            is JetpackError -> {
                val error = when (response.error.message) {
                    PLUGIN_ALREADY_EXISTS -> {
                        InstallSitePluginError(
                                InstallSitePluginErrorType.PLUGIN_ALREADY_INSTALLED,
                                response.error.message
                        )
                    }
                    else -> {
                        InstallSitePluginError(
                                InstallSitePluginErrorType.GENERIC_ERROR,
                                response.error.message
                        )
                    }
                }
                return InstalledSitePluginPayload(site, pluginSlug, error)
            }
        }
    }

    private fun sitePluginModelFromResponse(siteModel: SiteModel, response: PluginResponseModel): SitePluginModel {
        val sitePluginModel = SitePluginModel().apply {
            localSiteId = siteModel.id
            name = response.name
            displayName = response.name
            authorName = StringEscapeUtils.unescapeHtml4(response.author)
            authorUrl = response.authorUri
            description = response.description?.raw
            pluginUrl = response.pluginUri
            slug = response.plugin
            version = response.version
        }
        if (response.status == INACTIVE_STATUS) {
            sitePluginModel.setIsActive(false)
        } else {
            sitePluginModel.setIsActive(true)
        }
        return sitePluginModel
    }
}
