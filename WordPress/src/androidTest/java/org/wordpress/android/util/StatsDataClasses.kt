package org.wordpress.android.util

val countriesMap = mapOf(
        "US" to "United States",
        "CA" to "Canada"
)

data class StatsKeyValueData(
    var key: String,
    val value: String
) {
}
