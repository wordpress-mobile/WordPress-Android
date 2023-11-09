package org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail

import org.wordpress.android.ui.utils.AbstractAllowedUrlsWebViewNavigationDelegate

object CampaignDetailWebViewNavigationDelegate : AbstractAllowedUrlsWebViewNavigationDelegate() {
    override val allowedUrls = listOf(
        UrlMatcher(
            "wordpress.com".toRegex(),
            listOf(
                "/advertising/campaigns/\\d+/[a-zA-Z0-9.-]+\$".toRegex()
            )
        )
    )
}
