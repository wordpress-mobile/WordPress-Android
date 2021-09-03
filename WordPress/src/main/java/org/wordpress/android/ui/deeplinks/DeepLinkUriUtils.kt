package org.wordpress.android.ui.deeplinks

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.deeplinks.DeepLinkingIntentReceiverViewModel.Companion.HOST_WORDPRESS_COM
import org.wordpress.android.util.UriUtilsWrapper
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class DeepLinkUriUtils
@Inject constructor(private val siteStore: SiteStore, private val uriUtilsWrapper: UriUtilsWrapper) {
    private fun extractHostFromSite(site: SiteModel?): String? {
        return site?.url?.let { uriUtilsWrapper.parse(it).host }
    }

    fun getRedirectUri(uri: UriWrapper): UriWrapper? {
        return uri.getQueryParameter(REDIRECT_TO_PARAM)?.let { uriUtilsWrapper.parse(it) }
    }

    private fun extractSiteModelFromTargetHost(host: String): List<SiteModel> {
        return siteStore.getSitesByNameOrUrlMatching(host)
    }

    fun hostToSite(siteUrl: String): SiteModel? {
        return extractSiteModelFromTargetHost(siteUrl).find { site ->
            // Check if a site is available with given targetHost
            val host = extractHostFromSite(site)
            host != null && host == siteUrl
        }
    }

    fun blogIdToSite(blogId: String): SiteModel? {
        return blogId.toLongOrNull()?.let { siteStore.getSiteBySiteId(it) }
    }

    /**
     * Tracking URIs like `public-api.wordpress.com/mbar/...` come from emails and should be handled here
     */
    fun isTrackingUrl(uri: UriWrapper): Boolean {
        // https://public-api.wordpress.com/mbar/
        return uri.host == HOST_API_WORDPRESS_COM &&
                uri.pathSegments.firstOrNull() == MOBILE_TRACKING_PATH
    }

    fun isWpLoginUrl(uri: UriWrapper): Boolean {
        // https://wordpress.com/wp-login.php/
        return uri.host == HOST_WORDPRESS_COM &&
                uri.pathSegments.firstOrNull() == WP_LOGIN
    }

    companion object {
        private const val HOST_API_WORDPRESS_COM = "public-api.wordpress.com"
        private const val MOBILE_TRACKING_PATH = "mbar"
        private const val WP_LOGIN = "wp-login.php"
        private const val REDIRECT_TO_PARAM = "redirect_to"
    }
}
