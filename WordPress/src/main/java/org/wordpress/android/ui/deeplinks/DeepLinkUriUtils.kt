package org.wordpress.android.ui.deeplinks

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
}
