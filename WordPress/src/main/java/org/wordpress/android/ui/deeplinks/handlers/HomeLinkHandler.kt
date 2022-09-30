package org.wordpress.android.ui.deeplinks.handlers

import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenHome
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class HomeLinkHandler
@Inject constructor() : DeepLinkHandler {
    /**
     * Returns true if the URI looks like `wordpress://home`
     */
    override fun shouldHandleUrl(uri: UriWrapper): Boolean {
        return uri.host == HOME_PATH
    }

    override fun buildNavigateAction(uri: UriWrapper): NavigateAction {
        return OpenHome
    }

    override fun stripUrl(uri: UriWrapper): String {
        return buildString {
            append(HOME_PATH)
        }
    }

    companion object {
        private const val HOME_PATH = "home"
    }
}
