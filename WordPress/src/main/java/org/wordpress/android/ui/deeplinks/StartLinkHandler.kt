package org.wordpress.android.ui.deeplinks

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.ShowSignInFlow
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.StartCreateSiteFlow
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class StartLinkHandler
@Inject constructor(private val accountStore: AccountStore) {
    /**
     * Returns true if the URI looks like `wordpress.com/start`
     */
    fun isStartUrl(uri: UriWrapper): Boolean {
        return uri.host == DeepLinkingIntentReceiverViewModel.HOST_WORDPRESS_COM &&
                uri.pathSegments.firstOrNull() == START_PATH
    }

    /**
     * Returns StartCreateSiteFlow is user logged in and ShowSignInFlow if user is logged out
     */
    fun buildNavigateAction(): NavigateAction {
        return if (accountStore.hasAccessToken()) {
            StartCreateSiteFlow
        } else {
            ShowSignInFlow
        }
    }

    companion object {
        private const val START_PATH = "start"
    }
}
