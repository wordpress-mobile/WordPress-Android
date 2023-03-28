package org.wordpress.android.ui.jetpackoverlay.individualplugin

import org.wordpress.android.fluxc.persistence.JetpackCPConnectedSiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.config.WPIndividualPluginOverlayFeatureConfig
import org.wordpress.android.util.config.WPIndividualPluginOverlayMaxShownConfig
import org.wordpress.android.util.extensions.activeIndividualJetpackPluginNames
import org.wordpress.android.util.extensions.isJetpackIndividualPluginConnectedWithoutFullPlugin
import javax.inject.Inject

class WPJetpackIndividualPluginHelper @Inject constructor(
    private val siteStore: SiteStore,
    private val appPrefs: AppPrefsWrapper,
    private val wpIndividualPluginOverlayFeatureConfig: WPIndividualPluginOverlayFeatureConfig,
    private val wpIndividualPluginOverlayMaxShownConfig: WPIndividualPluginOverlayMaxShownConfig,
) {
    suspend fun shouldShowJetpackIndividualPluginOverlay(): Boolean {
        return wpIndividualPluginOverlayFeatureConfig.isEnabled() &&
                hasIndividualPluginJetpackConnectedSites() &&
                !wasOverlayShownOverMaxTimes()
    }

    suspend fun getJetpackConnectedSitesWithIndividualPlugins(): List<SiteWithIndividualJetpackPlugins> {
        val individualPluginConnectedSites = getIndividualPluginJetpackConnectedSites()
        return individualPluginConnectedSites.map { site ->
            SiteWithIndividualJetpackPlugins(
                name = site.name,
                url = site.url,
                individualPluginNames = site.activeIndividualJetpackPluginNames(),
            )
        }
    }

    fun incrementJetpackIndividualPluginOverlayShownCount() {
        appPrefs.incrementWPJetpackIndividualPluginOverlayShownCount()
    }

    private suspend fun hasIndividualPluginJetpackConnectedSites(): Boolean {
        val individualPluginConnectedSites = getIndividualPluginJetpackConnectedSites()
        return individualPluginConnectedSites.isNotEmpty()
    }

    private suspend fun getIndividualPluginJetpackConnectedSites(): List<JetpackCPConnectedSiteModel> {
        return siteStore.getJetpackCPConnectedSites()
            .filter { it.isJetpackIndividualPluginConnectedWithoutFullPlugin() }
    }

    private fun wasOverlayShownOverMaxTimes(): Boolean {
        val overlayMaxShownCount = wpIndividualPluginOverlayMaxShownConfig.getValue<Int>()
        return appPrefs.wpJetpackIndividualPluginOverlayShownCount >= overlayMaxShownCount
    }
}

data class SiteWithIndividualJetpackPlugins(
    val name: String,
    val url: String,
    val individualPluginNames: List<String>,
)
