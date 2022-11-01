package org.wordpress.android.ui.qrcodeauth

import android.net.UrlQuerySanitizer
import dagger.Reusable
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

@Reusable
class QRCodeAuthValidator @Inject constructor() {
    @Suppress("ReturnCount")
    fun isValidUri(scannedValue: String?): Boolean {
        if (scannedValue == null) return false

        val uri = UriWrapper(scannedValue)
        if (uri.host != VALID_HOST) return false

        return true
    }

    @Suppress("ReturnCount")
    fun extractQueryParams(scannedValue: String?): Map<String, String> {
        if (scannedValue == null) return emptyMap()

        val uri = UriWrapper(scannedValue)
        if (uri.host != VALID_HOST) return emptyMap()

        val queryParams = mutableMapOf<String, String>()
        uri.fragment?.let {
            UrlQuerySanitizer(it).parameterList.forEach { pair ->
                queryParams[pair.mParameter] = pair.mValue
            }
        }
        return queryParams
    }

    companion object {
        const val VALID_HOST = "apps.wordpress.com"
    }
}
