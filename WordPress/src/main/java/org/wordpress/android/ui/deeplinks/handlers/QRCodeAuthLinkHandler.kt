package org.wordpress.android.ui.deeplinks.handlers

import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenQRCodeAuthFlow
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class QRCodeAuthLinkHandler @Inject constructor() : DeepLinkHandler {
    /**
     * Returns true if the URI looks like `apps.wordpress.com/get`
     */
    override fun shouldHandleUrl(uri: UriWrapper): Boolean {
        // https://apps.wordpress.com/get/?campaign=login-qr-code#qr-code-login?token=XXXX&data=XXXXX
        return uri.host == HOST_APPS_WORDPRESS_COM &&
                uri.pathSegments.firstOrNull() == GET_PATH &&
                uri.getQueryParameter(CAMPAIGN) == CAMPAIGN_TYPE
    }

    override fun buildNavigateAction(uri: UriWrapper): NavigateAction {
        return OpenQRCodeAuthFlow(uri.toString())
    }

    override fun stripUrl(uri: UriWrapper): String {
        return buildString {
            append("$HOST_APPS_WORDPRESS_COM/$GET_PATH")
        }
    }

    companion object {
        private const val GET_PATH = "get"
        private const val HOST_APPS_WORDPRESS_COM = "apps.wordpress.com"
        private const val CAMPAIGN = "campaign"
        const val CAMPAIGN_TYPE = "login-qr-code"
    }
}
