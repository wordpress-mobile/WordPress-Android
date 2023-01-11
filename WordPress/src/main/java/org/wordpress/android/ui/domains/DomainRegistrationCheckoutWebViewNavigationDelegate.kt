package org.wordpress.android.ui.domains

import android.net.Uri

object DomainRegistrationCheckoutWebViewNavigationDelegate {
    private val optionalLanguagePath = "(?:/(?:\\w{2}-)?\\w{2})?".toRegex()
    private val allowedUrls = listOf(
        UrlMatcher(
            ".*wordpress.com".toRegex(),
            listOf(
                "/automattic-domain-name-registration-agreement.*".toRegex(),
                "/checkout.*".toRegex(),
                "$optionalLanguagePath/tos.*".toRegex(),
                "$optionalLanguagePath/support.*".toRegex()
            )
        )
    )

    fun canNavigateTo(url: Url) = allowedUrls.any { it.matches(url) }

    data class UrlMatcher(
        private val host: Regex,
        private val paths: List<Regex>
    ) {
        private fun matchesHost(url: Url) = url.host.matches(host)
        private fun matchesPath(url: Url) = paths.any { url.path.matches(it) }
        fun matches(url: Url) = matchesHost(url) && matchesPath(url)
    }

    data class Url(val host: String, val path: String)

    fun Uri.toUrl() = Url(host.orEmpty(), path.orEmpty())
}
