package org.wordpress.android.ui.accounts.login

import org.wordpress.android.util.UriUtilsWrapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache to store and retrieve the ApiRootUrl between the login discovery process and the final store step
 * This cache is necessary because between those states we are interrupting the code flow by calling external intents
 * so we need a place to safely store the data
 *
 * The cache uses domain names (case insensitive) as keys for consistent retrieval
 */
@Singleton
class ApiRootUrlCache @Inject constructor(
    private val uriUtilsWrapper: UriUtilsWrapper
) {
    private val cache = mutableMapOf<String, String>()

    fun put(key: String, value: String) {
        if (key.isEmpty() || value.isEmpty()) {
            return
        }
        val domainKey = extractDomain(key)
        if (domainKey.isNotEmpty()) {
            cache[domainKey] = value
        }
    }

    fun get(key: String): String? {
        if (key.isEmpty()) {
            return null
        }

        val domainKey = extractDomain(key)
        val result = cache[domainKey]

        return result
    }

    /**
     * Extracts the domain from a URL or domain string
     * Examples:
     * - "http://www.mysite.wordpress.com/path" -> "mysite.wordpress.com"
     * - "https://example.com:8080" -> "example.com"
     * - "example.com" -> "example.com"
     */
    @Suppress("SwallowedException")
    private fun extractDomain(urlOrDomain: String): String {
        return try {
            // First try to parse as URI
            val uri = uriUtilsWrapper.parse(urlOrDomain)
            val host = uri.host

            if (!host.isNullOrEmpty()) {
                // Remove www. prefix and convert to lowercase
                host.removePrefix("www.").lowercase()
            } else {
                // If no host found, it might be just a domain without protocol
                // Clean it up and use as-is
                urlOrDomain.trim()
                    .removePrefix("http://")
                    .removePrefix("https://")
                    .removePrefix("www.")
                    .substringBefore("/")
                    .substringBefore(":")
                    .lowercase()
            }
        } catch (e: IllegalArgumentException) {
            // Fallback for malformed URLs
            urlOrDomain.trim()
        }
    }
}
