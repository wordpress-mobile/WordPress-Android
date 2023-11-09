package org.wordpress.android.ui.domains

import org.wordpress.android.ui.utils.AbstractAllowedUrlsWebViewNavigationDelegate

object DomainRegistrationCheckoutWebViewNavigationDelegate : AbstractAllowedUrlsWebViewNavigationDelegate() {
    private val optionalLanguagePath = "(?:/(?:\\w{2}-)?\\w{2})?".toRegex()

    override val allowedUrls = listOf(
        UrlMatcher(
            ".*wordpress.com".toRegex(),
            listOf(
                "/jetpack-app".toRegex(),
                "/plans.*?.*".toRegex(),
                "/automattic-domain-name-registration-agreement.*".toRegex(),
                "/checkout.*".toRegex(),
                "$optionalLanguagePath/tos.*".toRegex(),
                "$optionalLanguagePath/support.*".toRegex()
            )
        )
    )
}
