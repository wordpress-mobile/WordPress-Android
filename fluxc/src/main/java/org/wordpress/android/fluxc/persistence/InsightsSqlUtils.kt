package org.wordpress.android.fluxc.persistence

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.AllTimeInsightsRestClient.AllTimeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.CommentsRestClient.CommentsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient.FollowerType
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient.FollowerType.EMAIL
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient.FollowerType.WP_COM
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient.FollowersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient.PostStatsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient.PostsResponse.PostResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.MostPopularRestClient.MostPopularResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.PostingActivityRestClient.PostingActivityResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.PublicizeRestClient.PublicizeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.TagsRestClient.TagsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.TodayInsightsRestClient.VisitResponse
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.ALL_TIME_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.COMMENTS_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.EMAIL_FOLLOWERS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.LATEST_POST_DETAIL_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.LATEST_POST_STATS_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.MOST_POPULAR_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.POSTING_ACTIVITY
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.PUBLICIZE_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.TAGS_AND_CATEGORIES_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.TODAYS_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.WP_COM_FOLLOWERS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.INSIGHTS
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightsSqlUtils
@Inject constructor(private val statsSqlUtils: StatsSqlUtils) {
    fun insert(site: SiteModel, data: AllTimeResponse) {
        insert(site, ALL_TIME_INSIGHTS, data)
    }

    fun insert(site: SiteModel, data: MostPopularResponse) {
        insert(site, MOST_POPULAR_INSIGHTS, data)
    }

    fun insert(site: SiteModel, data: PostResponse) {
        insert(site, LATEST_POST_DETAIL_INSIGHTS, data)
    }

    fun insert(site: SiteModel, postId: Long, data: PostStatsResponse) {
        insert(site, LATEST_POST_STATS_INSIGHTS, data, postId = postId)
    }

    fun insert(site: SiteModel, data: VisitResponse) {
        insert(site, TODAYS_INSIGHTS, data)
    }

    fun insert(site: SiteModel, data: FollowersResponse, followerType: FollowerType, replaceExistingData: Boolean) {
        insert(site, followerType.toDbKey(), data, replaceExistingData)
    }

    fun insert(site: SiteModel, data: CommentsResponse) {
        insert(site, COMMENTS_INSIGHTS, data)
    }

    fun insert(site: SiteModel, data: TagsResponse) {
        insert(site, TAGS_AND_CATEGORIES_INSIGHTS, data)
    }

    fun insert(site: SiteModel, data: PublicizeResponse) {
        insert(site, PUBLICIZE_INSIGHTS, data)
    }

    fun insert(site: SiteModel, data: PostingActivityResponse) {
        insert(site, POSTING_ACTIVITY, data)
    }

    fun selectAllTimeInsights(site: SiteModel): AllTimeResponse? {
        return select(site, ALL_TIME_INSIGHTS, AllTimeResponse::class.java)
    }

    fun selectMostPopularInsights(site: SiteModel): MostPopularResponse? {
        return select(site, MOST_POPULAR_INSIGHTS, MostPopularResponse::class.java)
    }

    fun selectLatestPostDetail(site: SiteModel): PostResponse? {
        return select(site, LATEST_POST_DETAIL_INSIGHTS, PostResponse::class.java)
    }

    fun selectLatestPostStats(site: SiteModel, postId: Long): PostStatsResponse? {
        return select(site, LATEST_POST_STATS_INSIGHTS, PostStatsResponse::class.java, postId)
    }

    fun selectTodayInsights(site: SiteModel): VisitResponse? {
        return select(site, TODAYS_INSIGHTS, VisitResponse::class.java)
    }

    fun selectFollowers(site: SiteModel, followerType: FollowerType): FollowersResponse? {
        return select(site, followerType.toDbKey(), FollowersResponse::class.java)
    }

    fun selectAllFollowers(site: SiteModel, followerType: FollowerType): List<FollowersResponse> {
        return selectAll(site, followerType.toDbKey(), FollowersResponse::class.java)
    }

    fun selectPublicizeInsights(site: SiteModel): PublicizeResponse? {
        return select(site, PUBLICIZE_INSIGHTS, PublicizeResponse::class.java)
    }

    fun selectPostingActivity(site: SiteModel): PostingActivityResponse? {
        return select(site, POSTING_ACTIVITY, PostingActivityResponse::class.java)
    }

    private fun FollowerType.toDbKey(): StatsSqlUtils.BlockType {
        return when (this) {
            WP_COM -> WP_COM_FOLLOWERS
            EMAIL -> EMAIL_FOLLOWERS
        }
    }

    fun selectCommentInsights(site: SiteModel): CommentsResponse? {
        return select(site, COMMENTS_INSIGHTS, CommentsResponse::class.java)
    }

    fun selectTags(site: SiteModel): TagsResponse? {
        return select(site, TAGS_AND_CATEGORIES_INSIGHTS, TagsResponse::class.java)
    }

    private fun <T> insert(
        site: SiteModel,
        blockType: BlockType,
        data: T,
        replaceExistingData: Boolean = true,
        postId: Long? = null
    ) {
        statsSqlUtils.insert(site, blockType, INSIGHTS, data, replaceExistingData, postId = postId)
    }

    private fun <T> select(site: SiteModel, blockType: BlockType, classOfT: Class<T>, postId: Long? = null): T? {
        return statsSqlUtils.select(site, blockType, INSIGHTS, classOfT, postId = postId)
    }

    private fun <T> selectAll(site: SiteModel, blockType: BlockType, classOfT: Class<T>): List<T> {
        return statsSqlUtils.selectAll(site, blockType, INSIGHTS, classOfT)
    }
}
