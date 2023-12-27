package org.wordpress.android.ui.deeplinks.handlers

import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.DomainManagement
import org.wordpress.android.ui.deeplinks.DeepLinkingIntentReceiverViewModel.Companion.HOST_WORDPRESS_COM
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class DomainManagementLinkHandler
@Inject constructor() : DeepLinkHandler {
    /**
     * Returns true if the URI looks like `https://wordpress.com/me/domains` or `https://wordpress.com/domains/manage`
     */
    override fun shouldHandleUrl(uri: UriWrapper): Boolean {
        return (uri.host == HOST_WORDPRESS_COM &&
                (uri.pathSegments.isMeDomainsScheme() || uri.pathSegments.isDomainsManagementScheme()))
    }

    private fun List<String>.isMeDomainsScheme() =
        size == 2 && this[0] == ME_DOMAINS_SEGMENT1 && this[1] == ME_DOMAINS_SEGMENT2

    private fun List<String>.isDomainsManagementScheme() =
        size == 2 && this[0] == DOMAINS_MANAGE_SEGMENT1 && this[1] == DOMAINS_MANAGE_SEGMENT2

    override fun buildNavigateAction(uri: UriWrapper): NavigateAction = DomainManagement

    override fun stripUrl(uri: UriWrapper): String {
        return buildString {
            append("$HOST_WORDPRESS_COM/")
            if (uri.pathSegments.isMeDomainsScheme()) {
                append("$ME_DOMAINS_SEGMENT1/$ME_DOMAINS_SEGMENT2")
            } else {
                append("$DOMAINS_MANAGE_SEGMENT1/$DOMAINS_MANAGE_SEGMENT2")
            }
        }
    }

    companion object {
        private const val ME_DOMAINS_SEGMENT1 = "me"
        private const val ME_DOMAINS_SEGMENT2 = "domains"
        private const val DOMAINS_MANAGE_SEGMENT1 = "domains"
        private const val DOMAINS_MANAGE_SEGMENT2 = "manage"
    }
}
