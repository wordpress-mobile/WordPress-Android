package org.wordpress.android.fluxc.network.rest.wpcom.stats

import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

enum class StatsEndpointsEnum {
    VISITS,
    TOP_POSTS,
    REFERRERS,
    CLICKS,
    GEO_VIEWS,
    AUTHORS,
    VIDEO_PLAYS,
    COMMENTS,
    FOLLOWERS_WPCOM,
    FOLLOWERS_EMAIL,
    COMMENT_FOLLOWERS,
    TAGS_AND_CATEGORIES,
    PUBLICIZE,
    SEARCH_TERMS,
    INSIGHTS_POPULAR,
    INSIGHTS_ALL_TIME,
    INSIGHTS_TODAY,
    INSIGHTS_LATEST_POST_SUMMARY,
    INSIGHTS_LATEST_POST_VIEWS;

    val restEndpointPath: String
        get() {
            when (this) {
                VISITS -> return "visits"
                TOP_POSTS -> return "top-posts"
                REFERRERS -> return "referrers"
                CLICKS -> return "clicks"
                GEO_VIEWS -> return "country-views"
                AUTHORS -> return "top-authors"
                VIDEO_PLAYS -> return "video-plays"
                COMMENTS -> return "comments"
                FOLLOWERS_WPCOM -> return "followers?type=wpcom"
                FOLLOWERS_EMAIL -> return "followers?type=email"
                COMMENT_FOLLOWERS -> return "comment-followers"
                TAGS_AND_CATEGORIES -> return "tags"
                PUBLICIZE -> return "publicize"
                SEARCH_TERMS -> return "search-terms"
                INSIGHTS_POPULAR -> return "insights"
                INSIGHTS_ALL_TIME -> return ""
                INSIGHTS_TODAY -> return "summary"
                INSIGHTS_LATEST_POST_SUMMARY -> return "posts"
                INSIGHTS_LATEST_POST_VIEWS -> return "post"
                else -> {
                    AppLog.i(T.STATS, "Called an update of Stats of unknown section!?? " + this.name)
                    return ""
                }
            }
        }
}
