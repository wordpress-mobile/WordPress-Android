package org.wordpress.android.fluxc.network.utils

enum class StatsGranularity(private val value: String) {
    DAYS("day"),
    WEEKS("week"),
    MONTHS("month"),
    YEARS("year");

    override fun toString(): String {
        return value
    }
}
