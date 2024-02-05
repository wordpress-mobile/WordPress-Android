package org.wordpress.android.ui.deeplinks.handlers

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator
import org.wordpress.android.ui.deeplinks.DeepLinkUriUtils
import org.wordpress.android.ui.deeplinks.DeepLinkingIntentReceiverViewModel
import org.wordpress.android.ui.sitemonitor.SiteMonitorType
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class SiteMonitorLinkHandler
@Inject constructor(private val deepLinkUriUtils: DeepLinkUriUtils) : DeepLinkHandler {
    /**
     * Returns true if the URI looks like `wordpress.com/SITE_MONITORING_PATH`
     */
    override fun shouldHandleUrl(uri: UriWrapper): Boolean {
        return (uri.host == DeepLinkingIntentReceiverViewModel.HOST_WORDPRESS_COM &&
                uri.pathSegments.firstOrNull() == SITE_MONITORING_PATH) || uri.host == SITE_MONITORING_PATH
    }

    override fun buildNavigateAction(uri: UriWrapper): DeepLinkNavigator.NavigateAction {
        val targetHost = extractHost(uri)
        val site: SiteModel? = deepLinkUriUtils.hostToSite(targetHost)
        val siteMonitorType = urlToType(uri.toString())
        return DeepLinkNavigator.NavigateAction.OpenSiteMonitoringForSite(site, siteMonitorType)
    }

    private fun extractHost(uri: UriWrapper): String {
        if (uri.pathSegments.size <= 1) return ""
        return uri.pathSegments[1]
    }

    override fun stripUrl(uri: UriWrapper): String {
        return buildString {
            val offset = if (uri.host == SITE_MONITORING_PATH) {
                append(DeepLinkingIntentReceiverViewModel.APPLINK_SCHEME)
                0
            } else {
                append("${DeepLinkingIntentReceiverViewModel.HOST_WORDPRESS_COM}/")
                1
            }
            append(SITE_MONITORING_PATH)
            val pathSegments = uri.pathSegments
            val size = pathSegments.size
            val hasSiteUrl = if (size > offset + 1) pathSegments.getOrNull(offset + 1) != null else false
            if (hasSiteUrl) {
                append("/${DeepLinkingIntentReceiverViewModel.SITE_DOMAIN}")
            }
        }
    }

    private fun urlToType(url: String): SiteMonitorType {
        return when {
            url.contains(PHP_LOGS_PATTERN) -> SiteMonitorType.PHP_LOGS
            url.contains(WEB_SERVER_LOGS_PATTERN) -> SiteMonitorType.WEB_SERVER_LOGS
            else -> SiteMonitorType.METRICS
        }
    }

    companion object {
        private const val SITE_MONITORING_PATH = "site-monitoring"
        private const val PHP_LOGS_PATTERN = "/php"
        private const val WEB_SERVER_LOGS_PATTERN = "/web"
    }
}
