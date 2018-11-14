package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.AllTimeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.AllTimeResponse.StatsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowersResponse.FollowData
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowersResponse.FollowData.FollowParams
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowersResponse.FollowerResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.MostPopularResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostStatsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse.PostResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse.PostResponse.Discussion
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.VisitResponse
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

val FIELDS = listOf("period", "views")
const val FIRST_DAY = "2018-10-01"
const val FIRST_DAY_VIEWS = 10
const val SECOND_DAY = "2018-10-02"
const val SECOND_DAY_VIEWS = 11
val DATA = listOf(listOf(FIRST_DAY, FIRST_DAY_VIEWS.toString()), listOf(SECOND_DAY, SECOND_DAY_VIEWS.toString()))

val POST_STATS_RESPONSE = PostStatsResponse(0, 0, 0, VIEWS, null, DATA, FIELDS, listOf(), mapOf(), mapOf())

const val REBLOG_COUNT = 13
const val POST_COUNT = 17
val VISITS_FIELDS = listOf("period", "views", "visitors", "likes", "reblogs", "comments", "posts")
const val VISITS_DATE = "2018-11-02"
val VISITS_DATA = listOf(
        VISITS_DATE,
        "$VIEWS",
        "$VISITORS",
        "$LIKE_COUNT",
        "$REBLOG_COUNT",
        "$COMMENT_COUNT",
        "$POST_COUNT"
)
val VISITS_RESPONSE = VisitResponse(
        FIRST_DAY, "day", VISITS_FIELDS, listOf(VISITS_DATA)
)
const val USER_LABEL = "John Smith"
const val AVATAR = "avatar.jpg"
val PARAMS = FollowParams("follow", "following", "FollowingHover", true, "55", "75", "Source", "Blog.com")
val FOLLOWER_RESPONSE = FollowerResponse(
        USER_LABEL,
        AVATAR,
        URL,
        DATE,
        FollowData("type", PARAMS)
)
val FOLLOWERS_RESPONSE = FollowersResponse(0, 10, 100, 70, 30, listOf())
