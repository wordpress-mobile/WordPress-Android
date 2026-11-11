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
 * `{"error":"unauthorized","message":"The plan for 12345 does not allow fetching Device stats"}`
 *
 * The API reuses this code for a user who cannot view the site's stats at all,
 * and only the message — which WordPress.com localizes, so it cannot be matched
 * on — tells the two apart. The caller resolves that ambiguity instead; see
 * [isStatsGatedByPlan].
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
 * Returns true when the stats API refused the stat because the site's plan does
 * not include it, so the user can be offered an upgrade.
 *
 * `unauthorized` is both how the API reports a plan gate and how it refuses a
 * user whose role may not read the site's stats at all. Only the message
 * distinguishes them and WordPress.com returns it in the caller's locale, so the
 * code is all that can be matched on and [userCanViewStats] — the capability the
 * app already stores for the site — settles the rest: a user who may view the
 * site's stats can only be refused by the plan. Anyone else gets the regular
 * permission error, since no purchase gives a user a role they do not have.
 *
 * The caller must also only ask this for a stat a plan can actually gate (region
 * and city views, devices, UTM): for every other endpoint an `unauthorized` is a
 * permission problem whatever the user's capability says.
 */
internal fun isStatsGatedByPlan(response: String?, userCanViewStats: Boolean): Boolean =
    userCanViewStats && parseStatsApiErrorCode(response) == ERROR_CODE_UNAUTHORIZED
