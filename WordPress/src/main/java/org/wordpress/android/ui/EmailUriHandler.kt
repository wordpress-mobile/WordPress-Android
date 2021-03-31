package org.wordpress.android.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction.OpenInWebView
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction.StartCreateSiteFlow
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class EmailUriHandler
@Inject constructor(private val siteStore: SiteStore, private val accountStore: AccountStore) {
    private val _navigateAction = MutableLiveData<Event<NavigateAction>>()
    val navigateAction = _navigateAction as LiveData<Event<NavigateAction>>

    fun shouldHandleUri(uri: UriWrapper): Boolean {
        // https://public-api.wordpress.com/bar/
        return uri.host == HOST_PUBLIC_API_WORDPRESS_COM && uri.pathSegments.firstOrNull() == BAR_PATH
    }

    private fun isCreateSiteUrl(uri: UriWrapper): Boolean {
        // https://wordpress.com/start?immediate_login_attempt=1&login_reason=user_first_flow&login_email=testing%40email.com&login_locale=en
        return uri.host == HOST_WORDPRESS_COM && uri.pathSegments.firstOrNull() == START_PATH
    }

    fun handleUri(uri: UriWrapper) {
        val redirectUri = getRedirectUri(uri)
        val nestedRedirectUri = redirectUri?.let { getRedirectUri(it) }
        if (nestedRedirectUri != null && isCreateSiteUrl(nestedRedirectUri) && siteStore.sitesCount == 0) {
            _navigateAction.value = Event(StartCreateSiteFlow(accountStore.hasAccessToken()))
        } else {
            _navigateAction.value = Event(OpenInWebView(uri))
        }
    }

    private fun getRedirectUri(uri: UriWrapper): UriWrapper? {
        return uri.getQueryParameter(REDIRECT_TO_PARAM)?.let { UriWrapper(it) }
    }

    companion object {
        private const val HOST_PUBLIC_API_WORDPRESS_COM = "public-api.wordpress.com"
        private const val HOST_WORDPRESS_COM = "wordpress.com"
        private const val BAR_PATH = "bar"
        private const val START_PATH = "start"
        private const val REDIRECT_TO_PARAM = "redirect_to"
    }
}
