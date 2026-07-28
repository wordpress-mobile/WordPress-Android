package org.wordpress.android.ui.newstats.datasource

/**
 * Error code returned by the WordPress.com stats API when the requested stat is
 * not available for the site (e.g. file download stats on a Jetpack site). The
 * response body looks like:
 * `{"error":"invalid_blog","message":"File download stats are not available for Jetpack sites"}`
 */
private const val ERROR_CODE_INVALID_BLOG = "invalid_blog"

/**
 * Matches the value of the top-level "error" field in a WordPress.com API error
 * body. Kept as a lightweight regex so it works in plain JVM unit tests without
 * pulling in the Android [org.json] stubs.
 */
private val STATS_ERROR_CODE_REGEX = "\"error\"\\s*:\\s*\"([^\"]+)\"".toRegex()

/**
 * Extracts the WordPress.com stats API error code from a raw response body, or
 * returns null when the body is missing or does not contain an error code.
 */
internal fun parseStatsApiErrorCode(response: String?): String? {
    if (response.isNullOrBlank()) return null
    return STATS_ERROR_CODE_REGEX.find(response)?.groupValues?.get(1)
}

/**
 * Returns true when the stats API reports that the requested stat is not
 * available for the site (WordPress.com `invalid_blog` error).
 */
internal fun isStatsUnavailableForSite(response: String?): Boolean =
    parseStatsApiErrorCode(response) == ERROR_CODE_INVALID_BLOG
