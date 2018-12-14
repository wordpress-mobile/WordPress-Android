package org.wordpress.android.fluxc.store.stats.time

import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ClicksRestClient.ClicksResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ClicksRestClient.ClicksResponse.ClickGroup
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient.PostAndPageViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient.PostAndPageViewsResponse.ViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient.PostAndPageViewsResponse.ViewsResponse.PostViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse.Child
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse.Group
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse.Groups
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse.Referrer
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.SearchTermsRestClient.SearchTermsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.SearchTermsRestClient.SearchTermsResponse.SearchTerm
import org.wordpress.android.fluxc.store.stats.DATE
import org.wordpress.android.fluxc.store.stats.POST_COUNT

const val DAY_GRANULARITY = "day"
const val WEEK_GRANULARITY = "week"
const val MONTH_GRANULARITY = "month"
const val YEAR_GRANULARITY = "year"
const val TOTAL_VIEWS = 100
const val POST_ID = 1L
const val POST_TITLE = "ABCD"
const val POST_URL = "url.com"
const val POST_VIEWS = 10

val DAY_POST_VIEW_RESPONSE = PostViewsResponse(
        POST_ID,
        POST_TITLE,
        DAY_GRANULARITY,
        POST_URL,
        POST_VIEWS
)
val DAY_POST_VIEW_RESPONSE_LIST = List(POST_COUNT) { DAY_POST_VIEW_RESPONSE }
val DAY_VIEW_RESPONSE_MAP = mapOf(
        DATE.toString() to ViewsResponse(
                DAY_POST_VIEW_RESPONSE_LIST,
                TOTAL_VIEWS
        )
)
val DAY_POST_AND_PAGE_VIEWS_RESPONSE = PostAndPageViewsResponse(
        DATE,
        DAY_VIEW_RESPONSE_MAP,
        DAY_GRANULARITY
)

val WEEK_POST_VIEW_RESPONSE = PostViewsResponse(
        POST_ID,
        POST_TITLE,
        DAY_GRANULARITY,
        POST_URL,
        POST_VIEWS
)
val WEEK_POST_VIEW_RESPONSE_LIST = List(POST_COUNT) { WEEK_POST_VIEW_RESPONSE }
val WEEK_VIEW_RESPONSE_MAP = mapOf(
        DATE.toString() to ViewsResponse(
                WEEK_POST_VIEW_RESPONSE_LIST,
                TOTAL_VIEWS
        )
)
val WEEK_POST_AND_PAGE_VIEWS_RESPONSE = PostAndPageViewsResponse(
        DATE,
        WEEK_VIEW_RESPONSE_MAP,
        WEEK_GRANULARITY
)

val MONTH_POST_VIEW_RESPONSE = PostViewsResponse(
        POST_ID,
        POST_TITLE,
        DAY_GRANULARITY,
        POST_URL,
        POST_VIEWS
)
val MONTH_POST_VIEW_RESPONSE_LIST = List(POST_COUNT) { MONTH_POST_VIEW_RESPONSE }
val MONTH_VIEW_RESPONSE_MAP = mapOf(
        DATE.toString() to ViewsResponse(
                MONTH_POST_VIEW_RESPONSE_LIST,
                TOTAL_VIEWS
        )
)
val MONTH_POST_AND_PAGE_VIEWS_RESPONSE = PostAndPageViewsResponse(
        DATE,
        MONTH_VIEW_RESPONSE_MAP,
        MONTH_GRANULARITY
)

val YEAR_POST_VIEW_RESPONSE = PostViewsResponse(
        POST_ID,
        POST_TITLE,
        DAY_GRANULARITY,
        POST_URL,
        POST_VIEWS
)
val YEAR_POST_VIEW_RESPONSE_LIST = List(POST_COUNT) { YEAR_POST_VIEW_RESPONSE }
val YEAR_VIEW_RESPONSE_MAP = mapOf(
        DATE.toString() to ViewsResponse(
                YEAR_POST_VIEW_RESPONSE_LIST,
                TOTAL_VIEWS
        )
)
val YEAR_POST_AND_PAGE_VIEWS_RESPONSE = PostAndPageViewsResponse(
        DATE,
        YEAR_VIEW_RESPONSE_MAP,
        YEAR_GRANULARITY
)
const val GROUP_ID = "group ID"
val REFERRER = Referrer(
        GROUP_ID,
        "John Smith",
        "john.jpg",
        "john.com",
        30,
        listOf(Child("Child", 20, "child.jpg", "child.com"))
)
val GROUP = Group(GROUP_ID, "Group 1", "icon.jpg", "url.com", 50, null, referrers = listOf(REFERRER))
val REFERRERS_RESPONSE = ReferrersResponse(null, mapOf("2018-10-10" to Groups(10, 20, listOf(GROUP))))
val CLICK_GROUP = ClickGroup(GROUP_ID, "Click name", "click.jpg", "click.com", 20, null)
val CLICKS_RESPONSE = ClicksResponse(null, mapOf("2018-10-10" to ClicksResponse.Groups(10, 15, listOf(CLICK_GROUP))))
val SEARCH_TERMS_RESPONSE = SearchTermsResponse("day", mapOf("2018-10-10" to SearchTermsResponse.Day(10, 15, 20, listOf(
        SearchTerm("search term", 20)
))))
