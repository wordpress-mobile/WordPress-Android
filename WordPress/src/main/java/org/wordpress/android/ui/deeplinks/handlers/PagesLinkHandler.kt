package org.wordpress.android.ui.deeplinks.handlers

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenPages
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenPagesForSite
import org.wordpress.android.ui.deeplinks.DeepLinkUriUtils
import org.wordpress.android.ui.deeplinks.DeepLinkingIntentReceiverViewModel.Companion.HOST_WORDPRESS_COM
import org.wordpress.android.ui.deeplinks.DeepLinkingIntentReceiverViewModel.Companion.SITE_DOMAIN
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class PagesLinkHandler
@Inject constructor(private val deepLinkUriUtils: DeepLinkUriUtils) : DeepLinkHandler {
    /**
     * Returns true if the URI looks like `wordpress.com/pages`
     */
    override fun shouldHandleUrl(uri: UriWrapper): Boolean {
        return uri.host == HOST_WORDPRESS_COM &&
                uri.pathSegments.firstOrNull() == PAGES_PATH
    }

    override fun buildNavigateAction(uri: UriWrapper): NavigateAction {
        val targetHost: String = uri.lastPathSegment ?: ""
        val site: SiteModel? = deepLinkUriUtils.hostToSite(targetHost)
        return if (site != null) {
            OpenPagesForSite(site)
        } else {
            // In other cases, launch pages with the current selected site.
            OpenPages
        }
    }

    override fun stripUrl(uri: UriWrapper): String {
        return buildString {
            append("$HOST_WORDPRESS_COM/$PAGES_PATH")
            if (uri.pathSegments.size > 1) {
                append("/$SITE_DOMAIN")
            }
        }
    }

    companion object {
        private const val PAGES_PATH = "pages"
    }
}
