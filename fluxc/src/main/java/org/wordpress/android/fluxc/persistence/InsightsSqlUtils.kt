package org.wordpress.android.fluxc.persistence

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.AllTimeInsightsRestClient.AllTimeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.CommentsRestClient.CommentsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient.FollowersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient.PostStatsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient.PostsResponse.PostResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.MostPopularRestClient.MostPopularResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.PostingActivityRestClient.PostingActivityResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.PublicizeRestClient.PublicizeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.SummaryRestClient.SummaryResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.TagsRestClient.TagsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.TodayInsightsRestClient.VisitResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.subscribers.EmailsRestClient.EmailsSummaryResponse
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.ALL_TIME_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.COMMENTS_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.DETAILED_POST_STATS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.EMAILS_SUBSCRIBERS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.EMAIL_FOLLOWERS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.FOLLOWERS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.LATEST_POST_DETAIL_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.MOST_POPULAR_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.POSTING_ACTIVITY
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.PUBLICIZE_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.SUMMARY
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.TAGS_AND_CATEGORIES_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.TODAYS_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.WP_COM_FOLLOWERS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.INSIGHTS
import javax.inject.Inject

open class InsightsSqlUtils<RESPONSE_TYPE>
constructor(
    private val statsSqlUtils: StatsSqlUtils,
    private val statsRequestSqlUtils: StatsRequestSqlUtils,
    private val blockType: BlockType,
    private val classOfResponse: Class<RESPONSE_TYPE>
) {
    fun insert(
        site: SiteModel,
        data: RESPONSE_TYPE,
        requestedItems: Int? = null,
        replaceExistingData: Boolean = true,
        postId: Long? = null
    ) {
        statsSqlUtils.insert(site, blockType, INSIGHTS, data, replaceExistingData, postId = postId)
        if (replaceExistingData) {
            statsRequestSqlUtils.insert(
                    site,
                    blockType,
                    INSIGHTS,
                    requestedItems,
                    postId = postId
            )
        }
    }

    fun select(site: SiteModel, postId: Long? = null): RESPONSE_TYPE? {
        return statsSqlUtils.select(site, blockType, INSIGHTS, classOfResponse, postId = postId)
    }

    fun selectAll(site: SiteModel): List<RESPONSE_TYPE> {
        return statsSqlUtils.selectAll(site, blockType, INSIGHTS, classOfResponse)
    }

    fun hasFreshRequest(site: SiteModel, requestedItems: Int? = null, postId: Long? = null): Boolean {
        return statsRequestSqlUtils.hasFreshRequest(site, blockType, INSIGHTS, requestedItems, postId = postId)
    }

    class AllTimeSqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : InsightsSqlUtils<AllTimeResponse>(
            statsSqlUtils,
            statsRequestSqlUtils,
            ALL_TIME_INSIGHTS,
            AllTimeResponse::class.java
    )

    class MostPopularSqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : InsightsSqlUtils<MostPopularResponse>(
            statsSqlUtils,
            statsRequestSqlUtils,
            MOST_POPULAR_INSIGHTS,
            MostPopularResponse::class.java
    )

    class LatestPostDetailSqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : InsightsSqlUtils<PostResponse>(
            statsSqlUtils,
            statsRequestSqlUtils,
            LATEST_POST_DETAIL_INSIGHTS,
            PostResponse::class.java
    )

    class DetailedPostStatsSqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : InsightsSqlUtils<PostStatsResponse>(
            statsSqlUtils,
            statsRequestSqlUtils,
            DETAILED_POST_STATS,
            PostStatsResponse::class.java
    )

    class TodayInsightsSqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : InsightsSqlUtils<VisitResponse>(
            statsSqlUtils,
            statsRequestSqlUtils,
            TODAYS_INSIGHTS,
            VisitResponse::class.java
    )

    class CommentsInsightsSqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : InsightsSqlUtils<CommentsResponse>(
            statsSqlUtils,
            statsRequestSqlUtils,
            COMMENTS_INSIGHTS,
            CommentsResponse::class.java
    )

    class SummarySqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : InsightsSqlUtils<SummaryResponse>(
        statsSqlUtils,
        statsRequestSqlUtils,
        SUMMARY,
        SummaryResponse::class.java
    )

    class FollowersSqlUtils @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : InsightsSqlUtils<FollowersResponse>(
        statsSqlUtils,
        statsRequestSqlUtils,
        FOLLOWERS,
        FollowersResponse::class.java
    )

    class WpComFollowersSqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : InsightsSqlUtils<FollowersResponse>(
            statsSqlUtils,
            statsRequestSqlUtils,
            WP_COM_FOLLOWERS,
            FollowersResponse::class.java
    )

    class EmailFollowersSqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : InsightsSqlUtils<FollowersResponse>(
            statsSqlUtils,
            statsRequestSqlUtils,
            EMAIL_FOLLOWERS,
            FollowersResponse::class.java
    )

    class TagsSqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : InsightsSqlUtils<TagsResponse>(
            statsSqlUtils,
            statsRequestSqlUtils,
            TAGS_AND_CATEGORIES_INSIGHTS,
            TagsResponse::class.java
    )

    class PublicizeSqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : InsightsSqlUtils<PublicizeResponse>(
            statsSqlUtils,
            statsRequestSqlUtils,
            PUBLICIZE_INSIGHTS,
            PublicizeResponse::class.java
    )

    class PostingActivitySqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : InsightsSqlUtils<PostingActivityResponse>(
            statsSqlUtils,
            statsRequestSqlUtils,
            POSTING_ACTIVITY,
            PostingActivityResponse::class.java
    )

    class EmailsSqlUtils @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : InsightsSqlUtils<EmailsSummaryResponse>(
        statsSqlUtils,
        statsRequestSqlUtils,
        EMAILS_SUBSCRIBERS,
        EmailsSummaryResponse::class.java
    )
}
