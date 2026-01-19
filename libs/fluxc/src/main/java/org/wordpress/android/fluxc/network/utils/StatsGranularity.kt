package org.wordpress.android.fluxc.network.utils

enum class StatsGranularity(private val value: String) {
    HOURS("hour"),
    DAYS("day"),
    WEEKS("week"),
    MONTHS("month"),
    YEARS("year");

    override fun toString(): String {
        return value
    }
}
