package org.wordpress.android.ui.deeplinks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DEEP_LINKED
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.LoginForResult
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenHome
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenInBrowser
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.ShowSignInFlow
import org.wordpress.android.ui.deeplinks.handlers.DeepLinkHandlers
import org.wordpress.android.ui.deeplinks.handlers.ServerTrackingHandler
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class DeepLinkingIntentReceiverViewModel
@Inject constructor(
    @Named(UI_THREAD) private val uiDispatcher: CoroutineDispatcher,
    private val deepLinkHandlers: DeepLinkHandlers,
    private val deepLinkUriUtils: DeepLinkUriUtils,
    private val accountStore: AccountStore,
    private val serverTrackingHandler: ServerTrackingHandler,
    private val deepLinkTrackingUtils: DeepLinkTrackingUtils,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper
) : ScopedViewModel(uiDispatcher) {
    private val _navigateAction = MutableLiveData<Event<NavigateAction>>()
    val navigateAction = _navigateAction as LiveData<Event<NavigateAction>>
    private val _finish = MutableLiveData<Event<Unit>>()
    val finish = _finish as LiveData<Event<Unit>>
    val toast = deepLinkHandlers.toast
    var cachedUri: UriWrapper? = null

    fun start(action: String?, uri: UriWrapper?) {
        if (uri == null || !handleUrl(uri, action)) {
            if (action != null) {
                analyticsUtilsWrapper.trackWithDeepLinkData(
                        DEEP_LINKED,
                        action,
                        uri?.host ?: "",
                        uri?.uri
                )
            }
            _finish.value = Event(Unit)
        }
    }

    fun onSuccessfulLogin() {
        cachedUri?.let {
            handleUrl(it)
        }
    }

    /**
     * Handles the following URLs
     * `wordpress.com/post...`
     * `wordpress.com/stats...`
     * `public-api.wordpress.com/mbar`
     * and builds the navigation action based on them
     */
    private fun handleUrl(uriWrapper: UriWrapper, action: String? = null): Boolean {
        cachedUri = uriWrapper
        return buildNavigateAction(uriWrapper)?.also {
            if (action != null) {
                deepLinkTrackingUtils.track(action, it, uriWrapper)
            }
            if (accountStore.hasAccessToken() || it is OpenInBrowser || it is ShowSignInFlow || it is OpenHome) {
                _navigateAction.value = Event(it)
            } else {
                _navigateAction.value = Event(LoginForResult)
            }
        } != null
    }

    private fun buildNavigateAction(uri: UriWrapper, rootUri: UriWrapper = uri): NavigateAction? {
        return when {
            deepLinkUriUtils.isTrackingUrl(uri) -> getRedirectUriAndBuildNavigateAction(uri, rootUri)
                    ?.also {
                        // The new URL was build so we need to hit the original `mbar` tracking URL
                        serverTrackingHandler.request(uri)
                    }
                    ?: OpenInBrowser(rootUri.copy(REGULAR_TRACKING_PATH))
            deepLinkUriUtils.isWpLoginUrl(uri) -> getRedirectUriAndBuildNavigateAction(uri, rootUri)
            else -> deepLinkHandlers.buildNavigateAction(uri)
        }
    }

    private fun getRedirectUriAndBuildNavigateAction(uri: UriWrapper, rootUri: UriWrapper): NavigateAction? {
        return deepLinkUriUtils.getRedirectUri(uri)?.let { buildNavigateAction(it, rootUri) }
    }

    override fun onCleared() {
        serverTrackingHandler.clear()
        cachedUri = null
        super.onCleared()
    }

    companion object {
        const val HOST_WORDPRESS_COM = "wordpress.com"
        const val APPLINK_SCHEME = "wordpress://"
        const val SITE_DOMAIN = "domain"
        private const val REGULAR_TRACKING_PATH = "bar"
    }
}
