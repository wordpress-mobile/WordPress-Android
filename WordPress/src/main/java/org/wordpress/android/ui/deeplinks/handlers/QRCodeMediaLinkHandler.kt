package org.wordpress.android.ui.deeplinks.handlers

import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkUriUtils
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class QRCodeMediaLinkHandler @Inject constructor(
    private val deepLinkUriUtils: DeepLinkUriUtils
) : DeepLinkHandler {
    /**
     * Returns true if the URI looks like `apps.wordpress.com/get`
     */
    override fun shouldHandleUrl(uri: UriWrapper): Boolean {
        // https://apps.wordpress.com/get/?campaign=qr-code-media&data=post_id:6,site_id:227148183
        return uri.host == HOST_APPS_WORDPRESS_COM &&
                uri.pathSegments.firstOrNull() == GET_PATH &&
                uri.getQueryParameter("campaign") == CAMPAIGN_TYPE
    }

    override fun buildNavigateAction(uri: UriWrapper): NavigateAction {
        val extractedSiteId = extractSiteIdFromUrl(uri)
        return when (val siteModel = extractedSiteId?.let { siteId -> deepLinkUriUtils.blogIdToSite(siteId) }) {
            null -> {
                NavigateAction.OpenMySite
            }
            else -> {
                NavigateAction.OpenMediaForSite(siteModel)
            }
        }
    }

    override fun stripUrl(uri: UriWrapper): String {
        return buildString {
            append("$HOST_APPS_WORDPRESS_COM/$GET_PATH")
        }
    }

    private fun extractSiteIdFromUrl(uri: UriWrapper): String? {
        uri.getQueryParameter("data")?.let { data ->
            val siteIdPair = data.split(",").find { it.startsWith("site_id:") }
            return siteIdPair?.substringAfter(":")
        }
        return null
    }

    companion object {
        private const val GET_PATH = "get"
        private const val HOST_APPS_WORDPRESS_COM = "apps.wordpress.com"
        private const val CAMPAIGN_TYPE = "qr-code-media"
    }
}
