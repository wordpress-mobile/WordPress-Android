package org.wordpress.android.util

val countriesMap = mapOf(
    "US" to "United States",
    "CA" to "Canada"
)

data class StatsVisitsData(
    val views: String,
    val visitors: String,
    val likes: String,
    val comments: String
)

data class StatsKeyValueData(
    var key: String,
    val value: String
)
