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
@Inject constructor(private val deepLinkUriUtils: DeepLinkUriUtils){
    fun buildTrackingDataFromNavigateAction(navigateAction: NavigateAction, uri: UriWrapper): TrackingData {
        return when {
            navigateAction is OpenInBrowser -> {
                buildTrackingData(navigateAction.uri, EMAIL)
            }
            deepLinkUriUtils.isTrackingUrl(uri) -> {
                val redirectUri = deepLinkUriUtils.getRedirectUri(uri)
                val loginReason = uri.getQueryParameter("login_reason")
                buildTrackingData(redirectUri ?: uri, EMAIL, loginReason)
            }
            else -> {
                buildTrackingData(uri)
            }
        }
    }

    private fun buildTrackingData(uri: UriWrapper, source: Source? = null, sourceInfo: String? = null): TrackingData {
        val url = StringBuilder()
        if (uri.host != DeepLinkingIntentReceiverViewModel.HOST_WORDPRESS_COM) {
            url.append("wordpress://")
        }
        url.append(uri.host ?: DeepLinkingIntentReceiverViewModel.HOST_WORDPRESS_COM)
        val path = uri.pathSegments.firstOrNull()
        if (uri.host == DeepLinkingIntentReceiverViewModel.HOST_WORDPRESS_COM && path != null) {
            url.append("/")
            url.append(path)
        }
        val trackingSource = source ?: if (uri.host == DeepLinkingIntentReceiverViewModel.HOST_WORDPRESS_COM) {
            LINK
        } else {
            BANNER
        }
        return TrackingData(trackingSource, url.toString(), sourceInfo)
    }
}

