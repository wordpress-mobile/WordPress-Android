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
    UNKNOWN(R.string.stats_error_unknown)
}
