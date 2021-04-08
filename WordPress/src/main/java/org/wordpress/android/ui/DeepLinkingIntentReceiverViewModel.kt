package org.wordpress.android.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction.OpenEditor
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction.OpenEditorForSite
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction.OpenInBrowser
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction.OpenInEditor
import org.wordpress.android.ui.DeeplinkNavigator.NavigateAction.StartCreateSiteFlow
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class DeepLinkingIntentReceiverViewModel
@Inject constructor(
    private val siteStore: SiteStore,
    private val postStore: PostStore,
    private val accountStore: AccountStore,
    private val deepLinkUriUtils: DeepLinkUriUtils,
    @Named(UI_THREAD) private val uiDispatcher: CoroutineDispatcher
) : ScopedViewModel(uiDispatcher) {
    private val _navigateAction = MutableLiveData<Event<NavigateAction>>()
    val navigateAction = _navigateAction as LiveData<Event<NavigateAction>>
    private val _toast = MutableLiveData<Event<Int>>()
    val toast = _toast as LiveData<Event<Int>>

    /**
     * URIs like `public-api.wordpress.com/mbar/...` come from emails and should be handled here
     */
    fun shouldHandleEmailUrl(uri: UriWrapper): Boolean {
        // https://public-api.wordpress.com/mbar/
        return uri.host == HOST_API_WORDPRESS_COM
                && uri.pathSegments.firstOrNull() == MOBILE_TRACKING_PATH
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
    fun handleEmailUrl(uri: UriWrapper) {
        _navigateAction.value = Event(buildNavigateAction(uri))
    }

    private fun buildNavigateAction(uri: UriWrapper, fallbackUri: UriWrapper = uri): NavigateAction {
        val redirectUri: UriWrapper? = getRedirectUri(uri)
        return if (redirectUri != null && redirectUri.host == HOST_WORDPRESS_COM) {
            when (redirectUri.pathSegments.firstOrNull()) {
                POST_PATH -> buildOpenEditorNavigateAction(redirectUri)
                START_PATH -> StartCreateSiteFlow(accountStore.hasAccessToken())
                WP_LOGIN -> {
                    buildNavigateAction(redirectUri, fallbackUri)
                }
                else -> redirectToBrowser(fallbackUri)
            }
        } else {
            // Replace host to redirect to the browser
            redirectToBrowser(fallbackUri)
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
        _navigateAction.value = Event(buildOpenEditorNavigateAction(uri))
    }

    private fun buildOpenEditorNavigateAction(uri: UriWrapper): NavigateAction {
        val pathSegments = uri.pathSegments
        // Match: https://wordpress.com/post/blogNameOrUrl/postId
        val targetSite = pathSegments.getOrNull(1)?.toSite()
        val targetPost = pathSegments.getOrNull(2)?.toPost(targetSite)
        return openEditorForSiteAndPost(targetSite, targetPost)
    }

    /**
     * Converts HOST name of a site to SiteModel. It finds the Site in the current local sites and matches the name
     * to the host.
     */
    private fun String.toSite(): SiteModel? {
        val site = extractSiteModelFromTargetHost(this)
        val host = deepLinkUriUtils.extractHostFromSite(site)
        // Check if a site is available with given targetHost
        return if (site != null && host != null && host == this) {
            site
        } else {
            null
        }
    }

    /**
     * Converts the post ID in String to the local PostModel (if available).
     */
    private fun String.toPost(site: SiteModel?): PostModel? {
        val remotePostId: Long? = toLongOrNull()
        return if (site != null && remotePostId != null) {
            val post = postStore.getPostByRemotePostId(remotePostId, site)
            if (post == null) {
                _toast.value = Event(R.string.post_not_found)
            }
            post
        } else {
            null
        }
    }

    private fun openEditorForSiteAndPost(site: SiteModel?, post: PostModel?): NavigateAction {
        return when {
            site == null -> {
                // Site not found, or host of site doesn't match the host in url
                _toast.value = Event(R.string.blog_not_found)
                // Open a new post editor with current selected site
                OpenEditor
            }
            post == null -> {
                // Open new post editor for given site
                OpenEditorForSite(site)
            }
            else -> OpenInEditor(site, post.id)
        }
    }

    private fun extractSiteModelFromTargetHost(host: String): SiteModel? {
        return siteStore.getSitesByNameOrUrlMatching(host).firstOrNull()
    }

    private fun getRedirectUri(uri: UriWrapper): UriWrapper? {
        return uri.getQueryParameter(REDIRECT_TO_PARAM)?.let { UriWrapper(it) }
    }

    private fun shouldShow(uri: UriWrapper, path: String): Boolean {
        return uri.host == HOST_WORDPRESS_COM && uri.pathSegments.firstOrNull() == path
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
