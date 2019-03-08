package org.wordpress.android.fluxc.persistance.stats

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
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
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.PublicizeRestClient.PublicizeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.TagsRestClient.TagsResponse
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils
import org.wordpress.android.fluxc.persistence.StatsRequestSqlUtils
import org.wordpress.android.fluxc.persistence.StatsSqlUtils
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.ALL_TIME_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.COMMENTS_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.EMAIL_FOLLOWERS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.LATEST_POST_DETAIL_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.LATEST_POST_STATS_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.MOST_POPULAR_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.PUBLICIZE_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.TAGS_AND_CATEGORIES_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.WP_COM_FOLLOWERS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.INSIGHTS
import org.wordpress.android.fluxc.store.stats.ALL_TIME_RESPONSE
import org.wordpress.android.fluxc.store.stats.FOLLOWERS_RESPONSE
import org.wordpress.android.fluxc.store.stats.LATEST_POST
import org.wordpress.android.fluxc.store.stats.MOST_POPULAR_RESPONSE
import org.wordpress.android.fluxc.store.stats.POST_STATS_RESPONSE
import org.wordpress.android.fluxc.store.stats.PUBLICIZE_RESPONSE
import org.wordpress.android.fluxc.store.stats.TAGS_RESPONSE
import org.wordpress.android.fluxc.store.stats.TOP_COMMENTS_RESPONSE
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class InsightsSqlUtilsTest {
    @Mock lateinit var statsSqlUtils: StatsSqlUtils
    @Mock lateinit var statsRequestSqlUtils: StatsRequestSqlUtils
    @Mock lateinit var site: SiteModel
    private lateinit var insightsSqlUtils: InsightsSqlUtils

    @Before
    fun setUp() {
        insightsSqlUtils = InsightsSqlUtils(statsSqlUtils, statsRequestSqlUtils)
    }

    @Test
    fun `returns all time response from stats utils`() {
        whenever(statsSqlUtils.select(site, ALL_TIME_INSIGHTS, INSIGHTS, AllTimeResponse::class.java)).thenReturn(
                ALL_TIME_RESPONSE
        )

        val result = insightsSqlUtils.selectAllTimeInsights(site)

        assertEquals(result, ALL_TIME_RESPONSE)
    }

    @Test
    fun `inserts all time response to stats utils`() {
        insightsSqlUtils.insert(site, ALL_TIME_RESPONSE)

        verify(statsSqlUtils).insert(site, ALL_TIME_INSIGHTS, INSIGHTS, ALL_TIME_RESPONSE, true)
    }

    @Test
    fun `returns most popular response from stats utils`() {
        whenever(statsSqlUtils.select(site, MOST_POPULAR_INSIGHTS, INSIGHTS, MostPopularResponse::class.java))
                .thenReturn(
                        MOST_POPULAR_RESPONSE
                )

        val result = insightsSqlUtils.selectMostPopularInsights(site)

        assertEquals(result, MOST_POPULAR_RESPONSE)
    }

    @Test
    fun `inserts most popular response to stats utils`() {
        insightsSqlUtils.insert(site, MOST_POPULAR_RESPONSE)

        verify(statsSqlUtils).insert(site, MOST_POPULAR_INSIGHTS, INSIGHTS, MOST_POPULAR_RESPONSE, true)
    }

    @Test
    fun `returns latest post detail response from stats utils`() {
        whenever(statsSqlUtils.select(site, LATEST_POST_DETAIL_INSIGHTS, INSIGHTS, PostResponse::class.java))
                .thenReturn(
                        LATEST_POST
                )

        val result = insightsSqlUtils.selectLatestPostDetail(site)

        assertEquals(result, LATEST_POST)
    }

    @Test
    fun `inserts latest post detail response to stats utils`() {
        insightsSqlUtils.insert(site, LATEST_POST)

        verify(statsSqlUtils).insert(site, LATEST_POST_DETAIL_INSIGHTS, INSIGHTS, LATEST_POST, true)
    }

    @Test
    fun `returns latest post views response from stats utils`() {
        whenever(
                statsSqlUtils.select(
                        site,
                        LATEST_POST_STATS_INSIGHTS,
                        INSIGHTS,
                        PostStatsResponse::class.java
                )
        ).thenReturn(
                POST_STATS_RESPONSE
        )

        val result = insightsSqlUtils.selectLatestPostStats(site)

        assertEquals(result, POST_STATS_RESPONSE)
    }

    @Test
    fun `inserts latest post views response to stats utils`() {
        insightsSqlUtils.insert(site, POST_STATS_RESPONSE)

        verify(statsSqlUtils).insert(site, LATEST_POST_STATS_INSIGHTS, INSIGHTS, POST_STATS_RESPONSE, true)
    }

    @Test
    fun `returns WPCOM followers response from stats utils`() {
        assertReturnsFollowers(WP_COM, WP_COM_FOLLOWERS)
    }

    @Test
    fun `returns email followers response from stats utils`() {
        assertReturnsFollowers(EMAIL, EMAIL_FOLLOWERS)
    }

    private fun assertReturnsFollowers(
        followerType: FollowerType,
        blockType: BlockType
    ) {
        whenever(
                statsSqlUtils.select(
                        site,
                        blockType,
                        INSIGHTS,
                        FollowersResponse::class.java
                )
        ).thenReturn(
                FOLLOWERS_RESPONSE
        )

        val result = insightsSqlUtils.selectFollowers(site, followerType)

        assertEquals(result, FOLLOWERS_RESPONSE)
    }

    @Test
    fun `inserts WPCOM followers response to stats utils`() {
        insightsSqlUtils.insert(site, FOLLOWERS_RESPONSE, WP_COM, true)

        verify(statsSqlUtils).insert(site, WP_COM_FOLLOWERS, INSIGHTS, FOLLOWERS_RESPONSE, true)
    }

    @Test
    fun `inserts email followers response to stats utils`() {
        insightsSqlUtils.insert(site, FOLLOWERS_RESPONSE, EMAIL, true)

        verify(statsSqlUtils).insert(site, EMAIL_FOLLOWERS, INSIGHTS, FOLLOWERS_RESPONSE, true)
    }

    @Test
    fun `returns comments response from stats utils`() {
        whenever(
                statsSqlUtils.select(
                        site,
                        COMMENTS_INSIGHTS,
                        INSIGHTS,
                        CommentsResponse::class.java
                )
        ).thenReturn(
                TOP_COMMENTS_RESPONSE
        )

        val result = insightsSqlUtils.selectCommentInsights(site)

        assertEquals(result, TOP_COMMENTS_RESPONSE)
    }

    @Test
    fun `inserts comments response to stats utils`() {
        insightsSqlUtils.insert(site, TOP_COMMENTS_RESPONSE)

        verify(statsSqlUtils).insert(site, COMMENTS_INSIGHTS, INSIGHTS, TOP_COMMENTS_RESPONSE, true)
    }

    @Test
    fun `returns tags response from stats utils`() {
        whenever(
                statsSqlUtils.select(
                        site,
                        TAGS_AND_CATEGORIES_INSIGHTS,
                        INSIGHTS,
                        TagsResponse::class.java
                )
        ).thenReturn(
                TAGS_RESPONSE
        )

        val result = insightsSqlUtils.selectTags(site)

        assertEquals(result, TAGS_RESPONSE)
    }

    @Test
    fun `inserts tags response to stats utils`() {
        insightsSqlUtils.insert(site, TAGS_RESPONSE)

        verify(statsSqlUtils).insert(site, TAGS_AND_CATEGORIES_INSIGHTS, INSIGHTS, TAGS_RESPONSE, true)
    }

    @Test
    fun `returns publicize response from stats utils`() {
        whenever(
                statsSqlUtils.select(
                        site,
                        PUBLICIZE_INSIGHTS,
                        INSIGHTS,
                        PublicizeResponse::class.java
                )
        ).thenReturn(
                PUBLICIZE_RESPONSE
        )

        val result = insightsSqlUtils.selectPublicizeInsights(site)

        assertEquals(result, PUBLICIZE_RESPONSE)
    }

    @Test
    fun `inserts publicize response to stats utils`() {
        insightsSqlUtils.insert(site, PUBLICIZE_RESPONSE)

        verify(statsSqlUtils).insert(site, PUBLICIZE_INSIGHTS, INSIGHTS, PUBLICIZE_RESPONSE, true)
    }
}
