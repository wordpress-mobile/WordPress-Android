package org.wordpress.android.ui.utils

import android.net.Uri

abstract class AbstractAllowedUrlsWebViewNavigationDelegate {
    abstract val allowedUrls: List<UrlMatcher>

    fun canNavigateTo(url: Url) = allowedUrls.any { it.matches(url) }

    fun canNavigateTo(uri: Uri) = canNavigateTo(uri.toUrl())

    fun canNavigateTo(urlString: String) = canNavigateTo(Uri.parse(urlString))

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
