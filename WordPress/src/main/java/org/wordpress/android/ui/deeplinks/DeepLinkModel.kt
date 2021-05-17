package org.wordpress.android.ui.deeplinks

import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction

data class DeepLinkModel(val navigateAction: NavigateAction, val trackingInfo: TrackingData) {
    data class TrackingData(val source: Source, val url: String, val sourceInfo: String? = null)

    enum class Source(val value: String) {
        EMAIL("email"), BANNER("banner"), LINK("link")
    }
}
