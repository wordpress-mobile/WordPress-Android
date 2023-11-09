package org.wordpress.android.ui.domains.management.details

import org.wordpress.android.ui.utils.AbstractAllowedUrlsWebViewNavigationDelegate

class DomainManagementDetailsWebViewNavigationDelegate(domain: String) :
    AbstractAllowedUrlsWebViewNavigationDelegate() {
    override val allowedUrls = listOf(
        UrlMatcher(
            "wordpress.com".toRegex(),
            listOf("/domains/manage/all/$domain/edit/.*".toRegex())
        )
    )
}
