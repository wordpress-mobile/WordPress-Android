package org.wordpress.android.ui.newstats.datasource

import androidx.annotation.StringRes
import org.wordpress.android.R

/**
 * Classifies stats API errors into user-friendly categories.
 */
enum class StatsErrorType(@StringRes val messageResId: Int) {
    AUTH_ERROR(R.string.stats_error_auth),
    NETWORK_ERROR(R.string.stats_error_network),
    PARSING_ERROR(R.string.stats_error_parsing),
    API_ERROR(R.string.stats_error_api),
    NOT_AVAILABLE(R.string.stats_error_not_available),

    /**
     * The site's plan does not include this stat, and the user may buy one that
     * does. Only the stats a plan can gate report this, so the cards that render
     * the upsell are the only place it reaches; the upsell supplies its own copy,
     * leaving this message as the fallback for a surface without one — the plain
     * permission wording those surfaces showed before the upsell existed.
     */
    PLAN_GATED(R.string.stats_error_auth),
    UNKNOWN(R.string.stats_error_unknown)
}
