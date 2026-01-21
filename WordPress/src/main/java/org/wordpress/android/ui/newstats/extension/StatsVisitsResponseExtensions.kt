package org.wordpress.android.ui.newstats.extension

import uniffi.wp_api.StatsCommentsDataPoint
import uniffi.wp_api.StatsLikesDataPoint
import uniffi.wp_api.StatsPostsDataPoint
import uniffi.wp_api.StatsVisitorsDataPoint
import uniffi.wp_api.StatsVisitsDataPoint
import uniffi.wp_api.StatsVisitsResponse
import uniffi.wp_api.getStatsCommentsData
import uniffi.wp_api.getStatsLikesData
import uniffi.wp_api.getStatsPostsData
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

fun StatsVisitsResponse.statsPostsData(): List<StatsPostsDataPoint> = getStatsPostsData(this)
