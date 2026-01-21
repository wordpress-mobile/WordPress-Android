package org.wordpress.android.ui.newstats.extension

import uniffi.wp_api.StatsCommentsDataPoint
import uniffi.wp_api.StatsLikesDataPoint
import uniffi.wp_api.StatsVisitorsDataPoint
import uniffi.wp_api.StatsVisitsDataPoint
import uniffi.wp_api.StatsVisitsDataValue
import uniffi.wp_api.StatsVisitsResponse
import uniffi.wp_api.getStatsCommentsData
import uniffi.wp_api.getStatsLikesData
import uniffi.wp_api.getStatsVisitorsData
import uniffi.wp_api.getStatsVisitsData

/**
 * Extension functions for [StatsVisitsResponse] to extract typed data points.
 * These wrap the top-level helper functions from the wordpress-rs API for a more idiomatic Kotlin API.
 */

fun StatsVisitsResponse.statsVisitsData(): List<StatsVisitsDataPoint> = getStatsVisitsData(this)

fun StatsVisitsResponse.statsVisitorsData(): List<StatsVisitorsDataPoint> = getStatsVisitorsData(this)

fun StatsVisitsResponse.statsLikesData(): List<StatsLikesDataPoint> = getStatsLikesData(this)

fun StatsVisitsResponse.statsCommentsData(): List<StatsCommentsDataPoint> = getStatsCommentsData(this)

/**
 * Data point containing posts count for a given period.
 */
data class StatsPostsDataPoint(
    val period: String,
    val posts: ULong
)

/**
 * Extracts posts data from the response.
 * Unlike other stats data, there's no built-in helper for posts,
 * so we manually extract it from the response fields and data.
 */
@Suppress("UNCHECKED_CAST")
fun StatsVisitsResponse.statsPostsData(): List<StatsPostsDataPoint> {
    val periodIndex = fields.indexOf("period")
    val postsIndex = fields.indexOf("posts")

    if (periodIndex == -1 || postsIndex == -1) {
        return emptyList()
    }

    return data.mapNotNull { row ->
        val periodValue = row.getOrNull(periodIndex)
        val postsValue = row.getOrNull(postsIndex)

        val period = when (val pv = periodValue) {
            is StatsVisitsDataValue.String -> {
                val (str) = pv
                str
            }
            else -> null
        }
        val posts = when (val ps = postsValue) {
            is StatsVisitsDataValue.Number -> {
                val (num) = ps
                num
            }
            else -> null
        }

        if (period != null && posts != null) {
            StatsPostsDataPoint(period = period, posts = posts)
        } else {
            null
        }
    }
}
