package org.wordpress.android.fluxc.persistence

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.AllTimeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.MostPopularResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse.PostResponse
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.ALL_TIME_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.LATEST_POST_DETAIL_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.LATEST_POST_DETAIL_VIEWS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.Key.MOST_POPULAR_INSIGHTS
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

    fun insert(site: SiteModel, data: PostViewsResponse) {
        statsSqlUtils.insert(site, LATEST_POST_DETAIL_VIEWS, data)
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

    fun selectLatestPostViews(site: SiteModel): PostViewsResponse? {
        return statsSqlUtils.select(site, LATEST_POST_DETAIL_VIEWS, PostViewsResponse::class.java)
    }
}
