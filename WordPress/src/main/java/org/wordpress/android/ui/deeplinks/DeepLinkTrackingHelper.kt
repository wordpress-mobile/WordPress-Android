package org.wordpress.android.ui.deeplinks

import org.wordpress.android.ui.deeplinks.DeepLinkModel.Source
import org.wordpress.android.ui.deeplinks.DeepLinkModel.Source.BANNER
import org.wordpress.android.ui.deeplinks.DeepLinkModel.Source.EMAIL
import org.wordpress.android.ui.deeplinks.DeepLinkModel.Source.LINK
import org.wordpress.android.ui.deeplinks.DeepLinkModel.TrackingData
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenInBrowser
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class DeepLinkTrackingHelper
@Inject constructor(
    private val deepLinkUriUtils: DeepLinkUriUtils,
    private val deepLinkHandlers: DeepLinkHandlers
) {
    fun buildTrackingDataFromNavigateAction(navigateAction: NavigateAction, uri: UriWrapper): TrackingData {
        return when {
            navigateAction is OpenInBrowser -> {
                TrackingData(EMAIL, navigateAction.uri.toString())
            }
            deepLinkUriUtils.isTrackingUrl(uri) -> {
                val targetUri = extractTargetUri(uri)
                val loginReason = uri.getQueryParameter("login_reason")
                buildTrackingData(targetUri, EMAIL, loginReason)
            }
            else -> {
                buildTrackingData(uri)
            }
        }
    }

    private fun buildTrackingData(uri: UriWrapper, source: Source? = null, sourceInfo: String? = null): TrackingData {
        val url = deepLinkHandlers.stripUrl(uri)
        val trackingSource = source ?: if (uri.host == DeepLinkingIntentReceiverViewModel.HOST_WORDPRESS_COM) {
            LINK
        } else {
            BANNER
        }
        return TrackingData(trackingSource, url ?: "", sourceInfo)
    }

    private fun extractTargetUri(uri: UriWrapper): UriWrapper {
        return deepLinkUriUtils.getRedirectUri(uri)?.let { firstRedirect ->
            if (deepLinkUriUtils.isWpLoginUrl(firstRedirect)) {
                deepLinkUriUtils.getRedirectUri(firstRedirect)
            } else {
                firstRedirect
            }
        } ?: uri
    }
}
