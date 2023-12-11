package org.wordpress.android.ui.deeplinks.handlers

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenMediaForSite
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenMedia
import org.wordpress.android.ui.deeplinks.DeepLinkUriUtils
import org.wordpress.android.ui.deeplinks.DeepLinkingIntentReceiverViewModel
import org.wordpress.android.ui.deeplinks.DeepLinkingIntentReceiverViewModel.Companion.HOST_WORDPRESS_COM
import org.wordpress.android.ui.deeplinks.DeepLinkingIntentReceiverViewModel.Companion.SITE_DOMAIN
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class MediaLinkHandler
@Inject constructor(private val deepLinkUriUtils: DeepLinkUriUtils) : DeepLinkHandler {
    /**
     * Returns true if the URI looks like `wordpress.com/media`
     */
    override fun shouldHandleUrl(uri: UriWrapper): Boolean {
        return  (uri.host == HOST_WORDPRESS_COM &&
                uri.pathSegments.firstOrNull() == NEDIA_PATH) || uri.host == NEDIA_PATH
    }

    override fun buildNavigateAction(uri: UriWrapper): NavigateAction {
        val targetHost: String = uri.lastPathSegment ?: ""
        val site: SiteModel? = deepLinkUriUtils.hostToSite(targetHost)
        return if (site != null) {
            OpenMediaForSite(site)
        } else {
            // In other cases, launch the home view for the currently selected site.
            OpenMedia
        }
    }

    override fun stripUrl(uri: UriWrapper): String {
        return buildString {
            val offset = if (uri.host == NEDIA_PATH) {
                append(DeepLinkingIntentReceiverViewModel.APPLINK_SCHEME)
                0
            } else {
                append("$HOST_WORDPRESS_COM/")
                1
            }
            append(NEDIA_PATH)
            val pathSegments = uri.pathSegments
            val size = pathSegments.size
            val hasSiteUrl = if (size > offset + 1) pathSegments.getOrNull(offset + 1) != null else false
            if (hasSiteUrl) {
                append("/$SITE_DOMAIN")
            }
        }
    }

    companion object {
        private const val NEDIA_PATH = "media"
    }
}
