package org.wordpress.android.ui.deeplinks.handlers

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkUriUtils
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class QRCodeMediaLinkHandler @Inject constructor(
    private val deepLinkUriUtils: DeepLinkUriUtils,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : DeepLinkHandler {
    /**
     * Returns true if the URI looks like `apps.wordpress.com/get`
     */
    override fun shouldHandleUrl(uri: UriWrapper): Boolean {
        // https://apps.wordpress.com/get/?campaign=qr-code-media#/media/{blog_id}
        return uri.host == HOST_APPS_WORDPRESS_COM &&
                uri.pathSegments.firstOrNull() == GET_PATH &&
                uri.getQueryParameter(CAMPAIGN) == CAMPAIGN_TYPE
    }

    override fun buildNavigateAction(uri: UriWrapper): NavigateAction {
        val extractedSiteId = extractSiteIdFromUrl(uri)
        return when (val siteModel = extractedSiteId?.let { siteId -> deepLinkUriUtils.blogIdToSite(siteId) }) {
            null -> {
                analyticsTrackerWrapper.track(AnalyticsTracker.Stat.DEEP_LINK_FAILED,
                    mapOf(ERROR to INVALID_SITE_ID,
                        CAMPAIGN to uri.getQueryParameter(CAMPAIGN)?.replace("-", "_")))
                NavigateAction.OpenMySiteWithMessage(org.wordpress.android.R.string.qrcode_media_deeplink_error)
            }
            else -> {
                NavigateAction.OpenMediaPickerForSite(siteModel)
            }
        }
    }

    override fun stripUrl(uri: UriWrapper): String {
        return buildString {
            append("$HOST_APPS_WORDPRESS_COM/$GET_PATH")
        }
    }

    private fun extractSiteIdFromUrl(uri: UriWrapper): String? {
        uri.fragment?.let { fragment ->
            val pathSegments = fragment.split("/")

            if (pathSegments.isNotEmpty()) {
                return pathSegments.last()
            }
        }
        return null
    }

    companion object {
        private const val GET_PATH = "get"
        private const val HOST_APPS_WORDPRESS_COM = "apps.wordpress.com"
        private const val CAMPAIGN = "campaign"
        private const val ERROR = "error"
        private const val INVALID_SITE_ID = "invalid_site_id"
        const val CAMPAIGN_TYPE = "qr-code-media"
    }
}
