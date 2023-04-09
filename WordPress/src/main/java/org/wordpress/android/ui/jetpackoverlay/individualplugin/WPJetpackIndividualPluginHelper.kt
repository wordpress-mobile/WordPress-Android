package org.wordpress.android.ui.jetpackoverlay.individualplugin

import org.wordpress.android.fluxc.persistence.JetpackCPConnectedSiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.UrlUtils
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
                !wasOverlayShownOverMaxTimes() &&
                !wasOverlayShownRecently()
    }

    suspend fun getJetpackConnectedSitesWithIndividualPlugins(): List<SiteWithIndividualJetpackPlugins> {
        val individualPluginConnectedSites = getIndividualPluginJetpackConnectedSites()
        return individualPluginConnectedSites.map { site ->
            SiteWithIndividualJetpackPlugins(
                name = site.name,
                url = StringUtils.removeTrailingSlash(UrlUtils.removeScheme(site.url)),
                individualPluginNames = site.activeIndividualJetpackPluginNames(),
            )
        }
    }

    fun onJetpackIndividualPluginOverlayShown() {
        appPrefs.incrementWPJetpackIndividualPluginOverlayShownCount()
        appPrefs.wpJetpackIndividualPluginOverlayLastShownTimestamp = System.currentTimeMillis()
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

    private fun wasOverlayShownRecently(): Boolean {
        val lastShownTimestamp = appPrefs.wpJetpackIndividualPluginOverlayLastShownTimestamp
        val shownCount = appPrefs.wpJetpackIndividualPluginOverlayShownCount
        val delayBetweenOverlays = getDelayBetweenOverlays(shownCount)
        return System.currentTimeMillis() - lastShownTimestamp < delayBetweenOverlays
    }

    companion object {
        private const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
        private const val DELAY_BETWEEN_OVERLAYS_FIRST_TIME = 1 * DAY_IN_MILLIS
        private const val DELAY_BETWEEN_OVERLAYS_SECOND_TIME = 3 * DAY_IN_MILLIS
        private const val DELAY_BETWEEN_OVERLAYS_OTHER_TIMES = 7 * DAY_IN_MILLIS

        private fun getDelayBetweenOverlays(shownCount: Int): Long {
            if (shownCount < 1) return 0L
            return when (shownCount) {
                1 -> DELAY_BETWEEN_OVERLAYS_FIRST_TIME
                2 -> DELAY_BETWEEN_OVERLAYS_SECOND_TIME
                else -> DELAY_BETWEEN_OVERLAYS_OTHER_TIMES
            }
        }
    }
}

data class SiteWithIndividualJetpackPlugins(
    val name: String,
    val url: String,
    val individualPluginNames: List<String>,
)
