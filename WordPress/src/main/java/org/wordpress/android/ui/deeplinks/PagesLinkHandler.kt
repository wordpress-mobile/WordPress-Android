package org.wordpress.android.ui.deeplinks

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenPages
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenPagesForSite
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class PagesLinkHandler
@Inject constructor(private val deepLinkUriUtils: DeepLinkUriUtils) {
    /**
     * Returns true if the URI looks like `wordpress.com/pages`
     */
    fun isPagesUrl(uri: UriWrapper): Boolean {
        return uri.host == DeepLinkingIntentReceiverViewModel.HOST_WORDPRESS_COM &&
                uri.pathSegments.firstOrNull() == PAGES_PATH
    }

    /**
     * Returns StartCreateSiteFlow is user logged in and ShowSignInFlow if user is logged out
     */
    fun buildNavigateAction(uri: UriWrapper): NavigateAction {
        val targetHost: String = deepLinkUriUtils.extractTargetHost(uri)
        val site: SiteModel? = deepLinkUriUtils.hostToSite(targetHost)
        return if (site != null) {
            OpenPagesForSite(site)
        } else {
            // In other cases, launch pages with the current selected site.
            OpenPages
        }
    }

    companion object {
        private const val PAGES_PATH = "pages"
    }
}
