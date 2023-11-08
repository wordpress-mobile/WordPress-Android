package org.wordpress.android.ui.domains.management.details

import android.net.Uri

object DomainManagementDetailsWebViewNavigationDelegate {
    private val allowedUrls = listOf(
        UrlMatcher(
            "wordpress.com".toRegex(),
            listOf("/domains.*".toRegex())
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
