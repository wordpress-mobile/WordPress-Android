package org.wordpress.android.ui

import android.net.Uri
import android.net.Uri.Builder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction.OpenInActionView
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction.StartCreateSiteFlow
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class CreateSiteUriHandler
@Inject constructor(private val siteStore: SiteStore, private val accountStore: AccountStore) {
    private val _navigateAction = MutableLiveData<Event<NavigateAction>>()
    val navigateAction = _navigateAction as LiveData<Event<NavigateAction>>

    fun shouldHandleUri(uri: Uri): Boolean {
        // https://wordpress.com/start?immediate_login_attempt=1&login_reason=user_first_flow&login_email=vojtatestujici3%2Bnosite%40gmail.com&login_locale=en
        return uri.host == HOST_WORDPRESS_COM && uri.pathSegments.firstOrNull() == START_PATH
    }

    fun handleUri(uri: Uri) {
        if (shouldHandleUri(uri) && siteStore.sitesCount == 0) {
            _navigateAction.value = Event(StartCreateSiteFlow(accountStore.hasAccessToken()))
        } else {
            // Replace host to redirect to the browser
            val newUri = Builder()
                    .scheme(uri.scheme)
                    .path(REGULAR_TRACKING_PATH)
                    .query(uri.query)
                    .fragment(uri.fragment)
                    .authority(uri.authority)
                    .build()
            _navigateAction.value = Event(OpenInActionView(newUri))
        }
    }

    companion object {
        private const val HOST_WORDPRESS_COM = "wordpress.com"
        private const val REGULAR_TRACKING_PATH = "bar"
        private const val START_PATH = "start"
    }
}
