package org.wordpress.android.ui.deeplinks

import org.wordpress.android.analytics.AnalyticsTracker.Stat.DEEP_LINKED
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenInBrowser
import org.wordpress.android.ui.deeplinks.DeepLinkTrackingUtils.DeepLinkSource.BANNER
import org.wordpress.android.ui.deeplinks.DeepLinkTrackingUtils.DeepLinkSource.EMAIL
import org.wordpress.android.ui.deeplinks.DeepLinkTrackingUtils.DeepLinkSource.LINK
import org.wordpress.android.ui.deeplinks.handlers.DeepLinkHandlers
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import javax.inject.Inject

class DeepLinkTrackingUtils
@Inject constructor(
    private val deepLinkUriUtils: DeepLinkUriUtils,
    private val deepLinkHandlers: DeepLinkHandlers,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper
) {
    fun track(action: String, navigateAction: NavigateAction, uriWrapper: UriWrapper) {
        val trackingData = buildTrackingDataFromNavigateAction(navigateAction, uriWrapper)
        analyticsUtilsWrapper.trackWithDeepLinkData(
            DEEP_LINKED,
            action,
            uriWrapper.host ?: "",
            trackingData.source.value,
            trackingData.url,
            trackingData.sourceInfo
        )
    }

    private fun buildTrackingDataFromNavigateAction(navigateAction: NavigateAction, uri: UriWrapper): TrackingData {
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

    private fun buildTrackingData(
        uri: UriWrapper,
        source: DeepLinkSource? = null,
        sourceInfo: String? = null
    ): TrackingData {
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

    data class TrackingData(val source: DeepLinkSource, val url: String, val sourceInfo: String? = null)

    enum class DeepLinkSource(val value: String) {
        EMAIL("email"), BANNER("banner"), LINK("link")
    }
}
