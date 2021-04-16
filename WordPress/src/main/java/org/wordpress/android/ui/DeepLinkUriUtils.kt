package org.wordpress.android.ui

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.UriUtilsWrapper
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class DeepLinkUriUtils
@Inject constructor(private val siteStore: SiteStore, private val uriUtilsWrapper: UriUtilsWrapper) {
    private fun extractHostFromSite(site: SiteModel?): String? {
        return site?.url?.let { uriUtilsWrapper.parse(it).host }
    }

    fun getUriFromQueryParameter(uri: UriWrapper, key: String) =
            uri.getQueryParameter(key)?.let { uriUtilsWrapper.parse(it) }

    fun extractTargetHost(uri: UriWrapper): String {
        return uri.lastPathSegment ?: ""
    }

    private fun extractSiteModelFromTargetHost(host: String): SiteModel? {
        return siteStore.getSitesByNameOrUrlMatching(host).firstOrNull()
    }

    fun hostToSite(siteUrl: String): SiteModel? {
        val site = extractSiteModelFromTargetHost(siteUrl)
        val host = extractHostFromSite(site)
        // Check if a site is available with given targetHost
        return if (site != null && host != null && host == siteUrl) {
            site
        } else {
            null
        }
    }
}
