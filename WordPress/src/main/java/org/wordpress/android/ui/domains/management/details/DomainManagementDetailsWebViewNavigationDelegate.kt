package org.wordpress.android.ui.domains.management.details

import org.wordpress.android.ui.utils.AbstractAllowedUrlsWebViewNavigationDelegate

object DomainManagementDetailsWebViewNavigationDelegate: AbstractAllowedUrlsWebViewNavigationDelegate() {
    override val allowedUrls = listOf(
        UrlMatcher(
            "wordpress.com".toRegex(),
            listOf("/domains.*".toRegex())
        )
    )
}
