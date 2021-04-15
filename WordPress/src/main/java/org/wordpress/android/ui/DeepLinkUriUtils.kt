package org.wordpress.android.ui

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class DeepLinkUriUtils
@Inject constructor() {
    fun extractHostFromSite(site: SiteModel?): String? {
        return site?.url?.let { UriWrapper(it).host }
    }
    fun getUriFromQueryParameter(uri: UriWrapper, key: String) = uri.getQueryParameter(key)?.let { UriWrapper(it) }
}
