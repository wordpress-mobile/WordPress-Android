package org.wordpress.android.fluxc.persistence

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.AllTimeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.MostPopularResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostStatsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse.PostResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.VisitResponse
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.ALL_TIME_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.EMAIL_FOLLOWERS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.LATEST_POST_DETAIL_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.LATEST_POST_STATS_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.MOST_POPULAR_INSIGHTS
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

    fun insertWpComFollowers(site: SiteModel, data: FollowersResponse) {
        statsSqlUtils.insert(site, WP_COM_FOLLOWERS, data)
    }

    fun insertEmailFollowers(site: SiteModel, data: FollowersResponse) {
        statsSqlUtils.insert(site, EMAIL_FOLLOWERS, data)
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

    fun selectWpComFollowers(site: SiteModel): FollowersResponse? {
        return statsSqlUtils.select(site, WP_COM_FOLLOWERS, FollowersResponse::class.java)
    }

    fun selectEmailFollowers(site: SiteModel): FollowersResponse? {
        return statsSqlUtils.select(site, WP_COM_FOLLOWERS, FollowersResponse::class.java)
    }
}
