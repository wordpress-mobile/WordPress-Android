package org.wordpress.android.ui.deeplinks

import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.util.UriWrapper

interface DeepLinkHandler {
    /**
     * Returns true when the url is handled by this handler
     */
    fun shouldHandleUrl(uri: UriWrapper): Boolean

    /**
     * Builds navigate action from the deep link
     */
    fun buildNavigateAction(uri: UriWrapper): NavigateAction

    /**
     * Strips all uri sensitive params for tracking purposes
     */
    fun stripUrl(uri: UriWrapper): String?
}
