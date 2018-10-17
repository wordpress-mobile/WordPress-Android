package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.AllTimeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.AllTimeResponse.StatsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.MostPopularResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse.PostResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse.PostResponse.Discussion
import java.util.Date

val DATE = Date(10)
const val VISITORS = 10
const val VIEWS = 15
const val POSTS = 20
const val VIEWS_BEST_DAY = "Monday"
const val VIEWS_BEST_DAY_TOTAL = 25
val ALL_TIME_RESPONSE = AllTimeResponse(
        DATE, StatsResponse(VISITORS, VIEWS, POSTS, VIEWS_BEST_DAY, VIEWS_BEST_DAY_TOTAL)
)

const val HIGHEST_DAY_OF_WEEK = 10
const val HIGHEST_HOUR = 15
const val HIGHEST_DAY_PERCENT = 2.0
const val HIGHEST_HOUR_PERCENT = 5.0
val MOST_POPULAR_RESPONSE = MostPopularResponse(
        HIGHEST_DAY_OF_WEEK, HIGHEST_HOUR, HIGHEST_DAY_PERCENT, HIGHEST_HOUR_PERCENT
)

const val POSTS_FOUND = 15
const val ID: Long = 2
const val TITLE = "title"
const val URL = "URL"
const val LIKE_COUNT = 5
const val COMMENT_COUNT = 10
val LATEST_POST = PostResponse(ID, TITLE, DATE, URL, LIKE_COUNT, Discussion(COMMENT_COUNT))
val POST_VIEWS = PostViewsResponse(VIEWS)
