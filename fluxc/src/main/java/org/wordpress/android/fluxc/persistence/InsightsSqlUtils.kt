package org.wordpress.android.fluxc.persistence

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.AllTimeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.CommentsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowerType
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowerType.EMAIL
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowerType.WP_COM
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.MostPopularResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostStatsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse.PostResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.TagsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.VisitResponse
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.ALL_TIME_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.COMMENTS_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.EMAIL_FOLLOWERS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.LATEST_POST_DETAIL_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.LATEST_POST_STATS_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.MOST_POPULAR_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.TAGS_AND_CATEGORIES_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.TODAYS_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.WP_COM_FOLLOWERS
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightsSqlUtils
@Inject constructor(private val statsSqlUtils: StatsSqlUtils) {
    fun insert(site: SiteModel, data: AllTimeResponse) {
        statsSqlUtils.insert(site, ALL_TIME_INSIGHTS, data)
    }

    fun insert(site: SiteModel, data: MostPopularResponse) {
        statsSqlUtils.insert(site, MOST_POPULAR_INSIGHTS, data)
    }

    fun insert(site: SiteModel, data: PostResponse) {
        statsSqlUtils.insert(site, LATEST_POST_DETAIL_INSIGHTS, data)
    }

    fun insert(site: SiteModel, data: PostStatsResponse) {
        statsSqlUtils.insert(site, LATEST_POST_STATS_INSIGHTS, data)
    }

    fun insert(site: SiteModel, data: VisitResponse) {
        statsSqlUtils.insert(site, TODAYS_INSIGHTS, data)
    }

    fun insert(site: SiteModel, data: FollowersResponse, followerType: FollowerType) {
        statsSqlUtils.insert(site, followerType.toDbKey(), data)
    }

    fun insert(site: SiteModel, data: CommentsResponse) {
        statsSqlUtils.insert(site, COMMENTS_INSIGHTS, data)
    }

    fun insert(site: SiteModel, data: TagsResponse) {
        statsSqlUtils.insert(site, TAGS_AND_CATEGORIES_INSIGHTS, data)
    }

    fun selectAllTimeInsights(site: SiteModel): AllTimeResponse? {
        return statsSqlUtils.select(site, ALL_TIME_INSIGHTS, AllTimeResponse::class.java)
    }

    fun selectMostPopularInsights(site: SiteModel): MostPopularResponse? {
        return statsSqlUtils.select(site, MOST_POPULAR_INSIGHTS, MostPopularResponse::class.java)
    }

    fun selectLatestPostDetail(site: SiteModel): PostResponse? {
        return statsSqlUtils.select(site, LATEST_POST_DETAIL_INSIGHTS, PostResponse::class.java)
    }

    fun selectLatestPostStats(site: SiteModel): PostStatsResponse? {
        return statsSqlUtils.select(site, LATEST_POST_STATS_INSIGHTS, PostStatsResponse::class.java)
    }

    fun selectTodayInsights(site: SiteModel): VisitResponse? {
        return statsSqlUtils.select(site, TODAYS_INSIGHTS, VisitResponse::class.java)
    }

    fun selectFollowers(site: SiteModel, followerType: FollowerType): FollowersResponse? {
        return statsSqlUtils.select(site, followerType.toDbKey(), FollowersResponse::class.java)
    }

    private fun FollowerType.toDbKey(): StatsSqlUtils.Key {
        return when (this) {
            WP_COM -> WP_COM_FOLLOWERS
            EMAIL -> EMAIL_FOLLOWERS
        }
    }

    fun selectCommentInsights(site: SiteModel): CommentsResponse? {
        return statsSqlUtils.select(site, COMMENTS_INSIGHTS, CommentsResponse::class.java)
    }

    fun selectTags(site: SiteModel): TagsResponse? {
        return statsSqlUtils.select(site, TAGS_AND_CATEGORIES_INSIGHTS, TagsResponse::class.java)
    }
}
