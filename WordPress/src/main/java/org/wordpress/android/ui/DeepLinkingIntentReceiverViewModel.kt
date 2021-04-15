package org.wordpress.android.ui

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenInBrowser
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenInReader
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.ShowSignInFlow
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.StartCreateSiteFlow
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.utils.IntentUtils
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class DeepLinkingIntentReceiverViewModel
@Inject constructor(
    private val editorLinkHandler: EditorLinkHandler,
    private val accountStore: AccountStore,
    private val deepLinkUriUtils: DeepLinkUriUtils,
    private val serverTrackingHandler: ServerTrackingHandler,
    private val intentUtils: IntentUtils,
    @Named(UI_THREAD) private val uiDispatcher: CoroutineDispatcher
) : ScopedViewModel(uiDispatcher) {
    private val _navigateAction = MutableLiveData<Event<NavigateAction>>()
    val navigateAction = _navigateAction as LiveData<Event<NavigateAction>>
    val toast = editorLinkHandler.toast

    /**
     * Tracking URIs like `public-api.wordpress.com/mbar/...` come from emails and should be handled here
     */
    fun shouldHandleTrackingUrl(uri: UriWrapper): Boolean {
        // https://public-api.wordpress.com/mbar/
        return uri.host == HOST_API_WORDPRESS_COM &&
                uri.pathSegments.firstOrNull() == MOBILE_TRACKING_PATH
    }

    /**
     * URIs like `wordpress.com/post/...` with optional site name and post ID path parameters should be opened in editor
     */
    fun shouldOpenEditor(uri: UriWrapper) = shouldShow(uri, POST_PATH)

    /**
     * Recursively handles URIs that have a redirect_to query parameter set to:
     * `wordpress.com/post`
     * `wordpress.com/start`
     * `wordpress.com/wp-login.php`
     * The rest of URIs is redirected back to the browser
     */
    fun handleTrackingUrl(uri: UriWrapper) {
        val navigateAction = buildNavigateAction(uri)
        val event = if (navigateAction != null) {
            // Make sure we don't miss server tracking on `mbar` URIs
            if (shouldHandleTrackingUrl(uri)) {
                serverTrackingHandler.request(uri)
            }
            Event(navigateAction)
        } else {
            // No need to request the URI with ServerTrackingHandler because the browser will take care of it
            Event(redirectToBrowser(uri))
        }
        _navigateAction.value = event
    }

    private fun buildNavigateAction(uri: UriWrapper): NavigateAction? {
        val redirectUri: UriWrapper? = getRedirectUri(uri)
        return when {
            redirectUri == null -> null
            shouldOpenReader(redirectUri) -> OpenInReader(redirectUri)
            redirectUri.host == HOST_WORDPRESS_COM -> {
                when (redirectUri.pathSegments.firstOrNull()) {
                    POST_PATH -> editorLinkHandler.buildOpenEditorNavigateAction(redirectUri)
                    START_PATH -> if (accountStore.hasAccessToken()) StartCreateSiteFlow else ShowSignInFlow
                    WP_LOGIN -> buildNavigateAction(redirectUri)
                    else -> null
                }
            }
            else -> null
        }
    }

    private fun redirectToBrowser(uri: UriWrapper): NavigateAction {
        return OpenInBrowser(uri.copy(REGULAR_TRACKING_PATH))
    }

    /**
     * Opens post editor for provided uri. If uri contains a site and a postId
     * (e.g. https://wordpress.com/post/example.com/1231/), opens the post for editing, if available.
     * If the uri only contains a site (e.g. https://wordpress.com/post/example.com/ ), opens a new post
     * editor for that site, if available.
     * Else opens the new post editor for currently selected site.
     */
    fun handleOpenEditor(uri: UriWrapper) {
        _navigateAction.value = Event(editorLinkHandler.buildOpenEditorNavigateAction(uri))
    }

    private fun getRedirectUri(uri: UriWrapper): UriWrapper? {
        return deepLinkUriUtils.getUriFromQueryParameter(uri, REDIRECT_TO_PARAM)
    }

    private fun shouldShow(uri: UriWrapper, path: String): Boolean {
        return uri.host == HOST_WORDPRESS_COM && uri.pathSegments.firstOrNull() == path
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
        private const val HOST_WORDPRESS_COM = "wordpress.com"
        private const val HOST_API_WORDPRESS_COM = "public-api.wordpress.com"
        private const val MOBILE_TRACKING_PATH = "mbar"
        private const val REGULAR_TRACKING_PATH = "bar"
        private const val REDIRECT_TO_PARAM = "redirect_to"
        private const val POST_PATH = "post"
        private const val START_PATH = "start"
        private const val WP_LOGIN = "wp-login.php"
    }
}
