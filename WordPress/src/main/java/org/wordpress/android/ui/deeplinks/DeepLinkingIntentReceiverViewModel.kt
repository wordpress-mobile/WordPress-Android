package org.wordpress.android.ui.deeplinks

import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DEEP_LINKED
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.deeplinks.DeepLinkEntryPoint.WEB_LINKS
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.LoginForResult
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenInBrowser
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenJetpackForDeepLink
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenLoginPrologue
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.ShowSignInFlow
import org.wordpress.android.ui.deeplinks.handlers.DeepLinkHandlers
import org.wordpress.android.ui.deeplinks.handlers.ServerTrackingHandler
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions")
class DeepLinkingIntentReceiverViewModel
@Inject constructor(
    @Named(UI_THREAD) private val uiDispatcher: CoroutineDispatcher,
    private val deepLinkHandlers: DeepLinkHandlers,
    private val deepLinkUriUtils: DeepLinkUriUtils,
    private val accountStore: AccountStore,
    private val serverTrackingHandler: ServerTrackingHandler,
    private val deepLinkTrackingUtils: DeepLinkTrackingUtils,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    private val openWebLinksWithJetpackHelper: DeepLinkOpenWebLinksWithJetpackHelper,
) : ScopedViewModel(uiDispatcher) {
    private val _navigateAction = MutableLiveData<Event<NavigateAction>>()
    val navigateAction = _navigateAction as LiveData<Event<NavigateAction>>
    private val _finish = MutableLiveData<Event<Unit>>()
    val finish = _finish as LiveData<Event<Unit>>
    private val _showOpenWebLinksWithJetpackOverlay = MutableLiveData<Event<Unit>>()
    val showOpenWebLinksWithJetpackOverlay = _showOpenWebLinksWithJetpackOverlay as LiveData<Event<Unit>>
    val toast = deepLinkHandlers.toast
    private var action: String? = null
    private var uriWrapper: UriWrapper? = null
    private var deepLinkEntryPoint = DeepLinkEntryPoint.DEFAULT

    fun start(
        action: String?,
        data: UriWrapper?,
        entryPoint: DeepLinkEntryPoint,
        savedInstanceState: Bundle?
    ) {
        extractSavedInstanceStateIfNeeded(savedInstanceState)
        applyFromArgsIfNeeded(action, data, entryPoint, savedInstanceState != null)

        val requestHandled = checkAndShowOpenWebLinksWithJetpackOverlayIfNeeded()
        if (!requestHandled)
            handleRequest()
    }

    fun handleRequest() {
        uriWrapper?.let { uri ->
            if (!handleUrl(uri, action)) {
                trackWithDeepLinkDataAndFinish()
            }
        } ?: trackWithDeepLinkDataAndFinish()
    }

    private fun trackWithDeepLinkDataAndFinish() {
        action?.let {
            analyticsUtilsWrapper.trackWithDeepLinkData(DEEP_LINKED, it, uriWrapper?.host ?: "", uriWrapper?.uri)
        }
        _finish.value = Event(Unit)
    }

    fun onSuccessfulLogin() {
        uriWrapper?.let {
            handleUrl(it)
        }
    }

    fun writeToBundle(outState: Bundle) {
        uriWrapper?.let {
            outState.putParcelable(URI_KEY, it.uri)
        }
        outState.putString(DEEP_LINK_ENTRY_POINT_KEY, deepLinkEntryPoint.name)
    }

    fun forwardDeepLinkToJetpack() {
        uriWrapper?.let {
            if (openWebLinksWithJetpackHelper.handleOpenLinksInJetpackIfPossible()) {
                _navigateAction.value = Event(OpenJetpackForDeepLink(action = action, uri = it))
            } else {
                handleRequest()
            }
        } ?: handleRequest()
    }

    /**
     * Handles the following URLs
     * `wordpress.com/post...`
     * `wordpress.com/stats...`
     * `public-api.wordpress.com/mbar`
     * and builds the navigation action based on them
     */
    private fun handleUrl(uriWrapper: UriWrapper, action: String? = null): Boolean {
        return buildNavigateAction(uriWrapper)?.also {
            if (action != null) {
                deepLinkTrackingUtils.track(action, it, uriWrapper)
            }
            if (loginIsUnnecessary(it)) {
                _navigateAction.value = Event(it)
            } else {
                _navigateAction.value = Event(LoginForResult)
            }
        } != null
    }

    private fun loginIsUnnecessary(action: NavigateAction): Boolean {
        return accountStore.hasAccessToken() ||
                action is OpenInBrowser ||
                action is ShowSignInFlow ||
                action is OpenLoginPrologue
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

    private fun extractSavedInstanceStateIfNeeded(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            val uri: Uri? = savedInstanceState.getParcelable(URI_KEY)
            uriWrapper = uri?.let { UriWrapper(it) }
            deepLinkEntryPoint =
                DeepLinkEntryPoint.valueOf(
                    savedInstanceState.getString(DEEP_LINK_ENTRY_POINT_KEY, DeepLinkEntryPoint.DEFAULT.name)
                )
        }
    }

    private fun applyFromArgsIfNeeded(
        actionValue: String?,
        uriValue: UriWrapper?,
        deepLinkEntryPointValue: DeepLinkEntryPoint,
        hasSavedInstanceState: Boolean
    ) {
        if (hasSavedInstanceState) return

        action = actionValue
        uriWrapper = uriValue
        deepLinkEntryPoint = deepLinkEntryPointValue
    }

    private fun checkAndShowOpenWebLinksWithJetpackOverlayIfNeeded(): Boolean {
        return if (deepLinkEntryPoint == WEB_LINKS &&
            accountStore.hasAccessToken() && // Already logged in
            openWebLinksWithJetpackHelper.shouldShowOpenLinksInJetpackOverlay()
        ) {
            openWebLinksWithJetpackHelper.onOverlayShown()
            _showOpenWebLinksWithJetpackOverlay.value = Event(Unit)
            true
        } else {
            false
        }
    }

    override fun onCleared() {
        serverTrackingHandler.clear()
        uriWrapper = null
        super.onCleared()
    }

    companion object {
        const val HOST_WORDPRESS_COM = "wordpress.com"
        const val APPLINK_SCHEME = "wordpress://"
        const val SITE_DOMAIN = "domain"
        private const val REGULAR_TRACKING_PATH = "bar"

        private const val URI_KEY = "uri_key"
        private const val DEEP_LINK_ENTRY_POINT_KEY = "deep_link_entry_point_key"
    }
}
