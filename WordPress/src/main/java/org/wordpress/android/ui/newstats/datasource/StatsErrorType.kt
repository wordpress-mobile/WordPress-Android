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
     * The site's plan does not include this stat. Cards that show the upsell
     * supply its copy themselves, so this message is only the fallback for a
     * card without one: it has to stand on its own as an error, which the
     * upsell copy ("Upgrade your plan…") would not.
     */
    PLAN_GATED(R.string.stats_error_not_available),
    UNKNOWN(R.string.stats_error_unknown)
}
