package org.wordpress.android.fluxc.model.stats

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowerType.WP_COM
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.VisitResponse
import org.wordpress.android.fluxc.store.stats.ALL_TIME_RESPONSE
import org.wordpress.android.fluxc.store.stats.COMMENT_COUNT
import org.wordpress.android.fluxc.store.stats.FIRST_DAY
import org.wordpress.android.fluxc.store.stats.FIRST_DAY_VIEWS
import org.wordpress.android.fluxc.store.stats.FOLLOWER_RESPONSE
import org.wordpress.android.fluxc.store.stats.LATEST_POST
import org.wordpress.android.fluxc.store.stats.LIKE_COUNT
import org.wordpress.android.fluxc.store.stats.MOST_POPULAR_RESPONSE
import org.wordpress.android.fluxc.store.stats.POST_COUNT
import org.wordpress.android.fluxc.store.stats.POST_STATS_RESPONSE
import org.wordpress.android.fluxc.store.stats.REBLOG_COUNT
import org.wordpress.android.fluxc.store.stats.SECOND_DAY
import org.wordpress.android.fluxc.store.stats.SECOND_DAY_VIEWS
import org.wordpress.android.fluxc.store.stats.VIEWS
import org.wordpress.android.fluxc.store.stats.VIEW_ALL_FOLLOWERS_RESPONSE
import org.wordpress.android.fluxc.store.stats.VISITS_DATE
import org.wordpress.android.fluxc.store.stats.VISITS_RESPONSE

@RunWith(MockitoJUnitRunner::class)
class InsightsMapperTest {
    @Mock lateinit var site: SiteModel
    private lateinit var mapper: InsightsMapper
    private val siteId = 3L
    @Before
    fun setUp() {
        mapper = InsightsMapper()
        whenever(site.siteId).thenReturn(siteId)
    }

    @Test
    fun `maps all time response`() {
        val model = mapper.map(ALL_TIME_RESPONSE, site)

        assertThat(model.siteId).isEqualTo(siteId)
        assertThat(model.date).isEqualTo(ALL_TIME_RESPONSE.date)
        assertThat(model.visitors).isEqualTo(ALL_TIME_RESPONSE.stats?.visitors)
        assertThat(model.views).isEqualTo(ALL_TIME_RESPONSE.stats?.views)
        assertThat(model.posts).isEqualTo(ALL_TIME_RESPONSE.stats?.posts)
        assertThat(model.viewsBestDay).isEqualTo(ALL_TIME_RESPONSE.stats?.viewsBestDay)
        assertThat(model.viewsBestDayTotal).isEqualTo(ALL_TIME_RESPONSE.stats?.viewsBestDayTotal)
    }

    @Test
    fun `maps most popular response`() {
        val model = mapper.map(MOST_POPULAR_RESPONSE, site)

        assertThat(model.siteId).isEqualTo(siteId)
        assertThat(model.highestDayOfWeek).isEqualTo(MOST_POPULAR_RESPONSE.highestDayOfWeek)
        assertThat(model.highestHour).isEqualTo(MOST_POPULAR_RESPONSE.highestHour)
        assertThat(model.highestDayPercent).isEqualTo(MOST_POPULAR_RESPONSE.highestDayPercent)
        assertThat(model.highestHourPercent).isEqualTo(MOST_POPULAR_RESPONSE.highestHourPercent)
    }

    @Test
    fun `maps latest posts response`() {
        val model = mapper.map(
                LATEST_POST,
                POST_STATS_RESPONSE, site)

        assertThat(model.siteId).isEqualTo(siteId)
        assertThat(model.postTitle).isEqualTo(LATEST_POST.title)
        assertThat(model.postURL).isEqualTo(LATEST_POST.url)
        assertThat(model.postDate).isEqualTo(LATEST_POST.date)
        assertThat(model.postId).isEqualTo(LATEST_POST.id)
        assertThat(model.postCommentCount).isEqualTo(COMMENT_COUNT)
        assertThat(model.postViewsCount).isEqualTo(VIEWS)
        assertThat(model.postLikeCount).isEqualTo(LIKE_COUNT)
        assertThat(model.dayViews).containsOnly(FIRST_DAY to FIRST_DAY_VIEWS, SECOND_DAY to SECOND_DAY_VIEWS)
    }

    @Test
    fun `maps visits response`() {
        val model = mapper.map(VISITS_RESPONSE)

        assertThat(model.period).isEqualTo(VISITS_DATE)
        assertThat(model.comments).isEqualTo(COMMENT_COUNT)
        assertThat(model.likes).isEqualTo(LIKE_COUNT)
        assertThat(model.posts).isEqualTo(POST_COUNT)
        assertThat(model.reblogs).isEqualTo(REBLOG_COUNT)
        assertThat(model.views).isEqualTo(VIEWS)
        assertThat(model.visitors).isEqualTo(org.wordpress.android.fluxc.store.stats.VISITORS)
    }

    @Test
    fun `maps visits response with empty data`() {
        val model = mapper.map(VisitResponse(null, null, listOf("views", "comments"), listOf(listOf("10"))))

        assertThat(model.views).isEqualTo(10)
        assertThat(model.comments).isEqualTo(0)
    }

    @Test
    fun `maps and merges followers responses`() {
        val model = mapper.mapAndMergeFollowersModels(VIEW_ALL_FOLLOWERS_RESPONSE, WP_COM, LoadMode.Paged(1, false))

        assertThat(model.followers.size).isEqualTo(1)
        assertThat(model.followers.first().label).isEqualTo(FOLLOWER_RESPONSE.label)
    }
}
