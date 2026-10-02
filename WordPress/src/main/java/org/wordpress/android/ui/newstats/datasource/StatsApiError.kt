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
 * Note the site is named by its numeric blog id, not by a phrase like "this
 * site" — see [PLAN_GATE_MESSAGE_FRAGMENT].
 *
 * The API reuses this code for a user who cannot view the site's stats at all,
 * which is why the code alone does not identify a plan gate — see
 * [isStatsGatedByPlan].
 */
private const val ERROR_CODE_UNAUTHORIZED = "unauthorized"

/**
 * Fragment of the "message" field that separates a plan gate from the other
 * `unauthorized` errors. The gated endpoints (region/city views, devices, UTM)
 * all phrase it as "The plan for <blog id> does not allow fetching X stats",
 * while a permission failure reads "user cannot view stats".
 *
 * The fragment deliberately starts after the blog id, so it must stay shorter
 * than the sentence above: the id sits in the middle of that sentence, and
 * matching any more of it would never match a real response.
 */
private const val PLAN_GATE_MESSAGE_FRAGMENT = "does not allow fetching"

/**
 * Matches the value of the top-level "error" field in a WordPress.com API error
 * body. Kept as a lightweight regex so it works in plain JVM unit tests without
 * pulling in the Android [org.json] stubs.
 */
private val STATS_ERROR_CODE_REGEX = "\"error\"\\s*:\\s*\"([^\"]+)\"".toRegex()

/**
 * Matches the value of the top-level "message" field in a WordPress.com API
 * error body. Same lightweight-regex reasoning as [STATS_ERROR_CODE_REGEX].
 */
private val STATS_ERROR_MESSAGE_REGEX = "\"message\"\\s*:\\s*\"([^\"]+)\"".toRegex()

/**
 * Extracts the WordPress.com stats API error code from a raw response body, or
 * returns null when the body is missing or does not contain an error code.
 */
internal fun parseStatsApiErrorCode(response: String?): String? {
    if (response.isNullOrBlank()) return null
    return STATS_ERROR_CODE_REGEX.find(response)?.groupValues?.get(1)
}

/**
 * Extracts the WordPress.com stats API error message from a raw response body,
 * or returns null when the body is missing or carries no message.
 */
internal fun parseStatsApiErrorMessage(response: String?): String? {
    if (response.isNullOrBlank()) return null
    return STATS_ERROR_MESSAGE_REGEX.find(response)?.groupValues?.get(1)
}

/**
 * Returns true when the stats API reports that the requested stat is not
 * available for the site (WordPress.com `invalid_blog` error).
 */
internal fun isStatsUnavailableForSite(response: String?): Boolean =
    parseStatsApiErrorCode(response) == ERROR_CODE_INVALID_BLOG

/**
 * Returns true when the stats API reports that the site's plan does not include
 * the requested stat, meaning the user has to upgrade to see it.
 *
 * Both the plan gate and a plain "you may not view this site's stats" refusal
 * come back as `unauthorized`, so the message has to be checked too: every
 * stats endpoint runs its permission check before the plan check, and only the
 * plan check names the plan. Without that the upsell would also be shown to a
 * user whose role simply does not allow viewing stats — someone no purchase can
 * help. When the message does not name the plan this returns false and the
 * caller falls back to the regular permission error.
 */
internal fun isStatsGatedByPlan(response: String?): Boolean =
    parseStatsApiErrorCode(response) == ERROR_CODE_UNAUTHORIZED &&
        parseStatsApiErrorMessage(response)
            ?.contains(PLAN_GATE_MESSAGE_FRAGMENT, ignoreCase = true) == true
