package org.wordpress.android.ui.deeplinks.handlers

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenMySite
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenLoginPrologue
import org.wordpress.android.ui.deeplinks.DeepLinkingIntentReceiverViewModel
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class HomeLinkHandler
@Inject constructor(private val accountStore: AccountStore) : DeepLinkHandler {
    /**
     * Returns true if the URI looks like `wordpress://home`
     */
    override fun shouldHandleUrl(uri: UriWrapper): Boolean {
        return uri.host == HOME_PATH
    }

    override fun buildNavigateAction(uri: UriWrapper): NavigateAction {
        return if (accountStore.hasAccessToken()) {
            OpenMySite
        } else {
            OpenLoginPrologue
        }
    }

    override fun stripUrl(uri: UriWrapper): String {
        return buildString {
            if (uri.host == HOME_PATH) {
                append(DeepLinkingIntentReceiverViewModel.APPLINK_SCHEME)
            }
            append(HOME_PATH)
        }
    }

    companion object {
        private const val HOME_PATH = "home"
    }
}
