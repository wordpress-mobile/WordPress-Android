package org.wordpress.android.fluxc.network.rest.wpapi

import org.wordpress.android.fluxc.utils.extensions.slashJoin
import org.wordpress.android.util.UrlUtils

internal object WPAPIDiscoveryUtils {
    fun buildDefaultRESTBaseUrl(
        url: String
    ): String {
        val urlWithoutScheme = UrlUtils.removeScheme(url)
        val httpsUrl = UrlUtils.addUrlSchemeIfNeeded(urlWithoutScheme, true)

        // TODO investigate the possibility to use `/?rest_route=` instead,
        //  `/wp-json` works only when permalinks are enabled, where `/?rest_route=` works without them
        // fallback to ".../wp-json/"
        return httpsUrl.slashJoin("wp-json")
    }
}
