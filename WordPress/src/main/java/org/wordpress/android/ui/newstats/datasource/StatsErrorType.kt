package org.wordpress.android.ui.newstats.datasource

/**
 * Classifies stats API errors into user-friendly categories.
 */
enum class StatsErrorType {
    AUTH_ERROR,
    NETWORK_ERROR,
    PARSING_ERROR,
    API_ERROR,
    UNKNOWN
}
