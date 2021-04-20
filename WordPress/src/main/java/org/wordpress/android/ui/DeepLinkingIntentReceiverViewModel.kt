package org.wordpress.android.ui

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenInBrowser
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenInReader
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.utils.IntentUtils
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class DeepLinkingIntentReceiverViewModel
@Inject constructor(
    @Named(UI_THREAD) private val uiDispatcher: CoroutineDispatcher,
    private val editorLinkHandler: EditorLinkHandler,
    private val statsLinkHandler: StatsLinkHandler,
    private val startLinkHandler: StartLinkHandler,
    private val deepLinkUriUtils: DeepLinkUriUtils,
    private val serverTrackingHandler: ServerTrackingHandler,
    private val intentUtils: IntentUtils
) : ScopedViewModel(uiDispatcher) {
    private val _navigateAction = MutableLiveData<Event<NavigateAction>>()
    val navigateAction = _navigateAction as LiveData<Event<NavigateAction>>
    val toast = editorLinkHandler.toast

    /**
     * Handles the following URLs
     * `wordpress.com/post...`
     * `wordpress.com/stats...`
     * `public-api.wordpress.com/mbar`
     * and builds the navigation action based on them
     */
    fun handleUrl(uriWrapper: UriWrapper): Boolean {
        return if (shouldHandleUrl(uriWrapper)) {
            _navigateAction.value = Event(buildNavigateAction(uriWrapper))
            true
        } else {
            false
        }
    }

    /**
     * This viewmodel handles the following URLs
     * `wordpress.com/post/...`
     * `wordpress.com/stats/...`
     * `public-api.wordpress.com/mbar/`
     * In these cases this function returns true
     */
    private fun shouldHandleUrl(uri: UriWrapper): Boolean {
        return isTrackingUrl(uri) ||
                editorLinkHandler.isEditorUrl(uri) ||
                statsLinkHandler.isStatsUrl(uri)
    }

    /**
     * Tracking URIs like `public-api.wordpress.com/mbar/...` come from emails and should be handled here
     */
    private fun isTrackingUrl(uri: UriWrapper): Boolean {
        // https://public-api.wordpress.com/mbar/
        return uri.host == HOST_API_WORDPRESS_COM &&
                uri.pathSegments.firstOrNull() == MOBILE_TRACKING_PATH
    }

    private fun isWpLoginUrl(uri: UriWrapper): Boolean {
        // https://wordpress.com/wp-login.php/
        return uri.host == HOST_WORDPRESS_COM &&
                uri.pathSegments.firstOrNull() == WP_LOGIN
    }

    private fun buildNavigateAction(uri: UriWrapper, rootUri: UriWrapper = uri): NavigateAction {
        val trackingUrl = isTrackingUrl(uri)
        val navigateAction = when {
            trackingUrl || isWpLoginUrl(uri) -> {
                val redirectUri = getRedirectUri(uri)
                if (redirectUri != null) {
                    buildNavigateAction(redirectUri, rootUri)
                } else {
                    null
                }
            }
            shouldOpenReader(uri) -> OpenInReader(uri)
            editorLinkHandler.isEditorUrl(uri) -> editorLinkHandler.buildOpenEditorNavigateAction(uri)
            statsLinkHandler.isStatsUrl(uri) -> statsLinkHandler.buildOpenStatsNavigateAction(uri)
            startLinkHandler.isStartUrl(uri) -> startLinkHandler.buildNavigateAction()
            else -> null
        }?.also {
            // The new URL was build so we need to hit the original `mbar` tracking URL
            if (trackingUrl) serverTrackingHandler.request(uri)
        }
        return navigateAction ?: OpenInBrowser(
                rootUri.copy(REGULAR_TRACKING_PATH)
        )
    }

    private fun getRedirectUri(uri: UriWrapper): UriWrapper? {
        return deepLinkUriUtils.getUriFromQueryParameter(uri, REDIRECT_TO_PARAM)
    }

    /**
     * URIs supported by the Reader are already defined as intent filters in the manifest. Instead of replicating
     * that logic here, we simply check if we can resolve an [Intent] that uses [ReaderConstants.ACTION_VIEW_POST].
     * Since that's a custom action that is only handled by the Reader, we can then assume it supports this URI.
     */
    private fun shouldOpenReader(uri: UriWrapper): Boolean {
        return intentUtils.canResolveWith(ReaderConstants.ACTION_VIEW_POST, uri)
    }

    override fun onCleared() {
        serverTrackingHandler.clear()
        super.onCleared()
    }

    companion object {
        const val HOST_WORDPRESS_COM = "wordpress.com"
        private const val HOST_API_WORDPRESS_COM = "public-api.wordpress.com"
        private const val MOBILE_TRACKING_PATH = "mbar"
        private const val REGULAR_TRACKING_PATH = "bar"
        private const val REDIRECT_TO_PARAM = "redirect_to"
        private const val WP_LOGIN = "wp-login.php"
    }
}
