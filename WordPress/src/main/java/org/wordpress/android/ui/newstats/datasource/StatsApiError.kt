package org.wordpress.android.ui.newstats.datasource

/**
 * Error code returned by the WordPress.com stats API when the requested stat is
 * not available for the site (e.g. file download stats on a Jetpack site). The
 * response body looks like:
 * `{"error":"invalid_blog","message":"File download stats are not available for Jetpack sites"}`
 */
private const val ERROR_CODE_INVALID_BLOG = "invalid_blog"

/**
 * Error code returned by the WordPress.com stats API when the requested stat is
 * gated behind a paid plan (region/city views, devices, UTM). The response body
 * looks like:
 * `{"error":"unauthorized","message":"The plan for this site does not allow fetching Device stats"}`
 */
private const val ERROR_CODE_UNAUTHORIZED = "unauthorized"

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

/**
 * Returns true when the stats API reports that the site's plan does not include
 * the requested stat (WordPress.com `unauthorized` error), meaning the user has
 * to upgrade to see it.
 *
 * The API reuses `unauthorized` for a user who simply cannot view the site's
 * stats, so this reads as "upgrade" in that case too. iOS makes the same
 * trade-off: the gate is by far the common cause, and the generic permission
 * error it replaces was no more accurate.
 */
internal fun isStatsGatedByPlan(response: String?): Boolean =
    parseStatsApiErrorCode(response) == ERROR_CODE_UNAUTHORIZED
