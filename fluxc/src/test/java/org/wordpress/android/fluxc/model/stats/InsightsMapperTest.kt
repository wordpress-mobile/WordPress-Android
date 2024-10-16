package org.wordpress.android.fluxc.model.stats

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.insights.PostingActivityModel.Day
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient.FollowerType.WP_COM
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.PostingActivityRestClient.PostingActivityResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.PostingActivityRestClient.PostingActivityResponse.Streak
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.PostingActivityRestClient.PostingActivityResponse.Streaks
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.TodayInsightsRestClient.VisitResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.StatsUtils
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
import java.util.Calendar
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class InsightsMapperTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var statsUtils: StatsUtils
    private lateinit var mapper: InsightsMapper
    private val siteId = 3L
    @Before
    fun setUp() {
        mapper = InsightsMapper(statsUtils)
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
                POST_STATS_RESPONSE, site
        )

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
        val model = mapper.mapAndMergeFollowersModels(VIEW_ALL_FOLLOWERS_RESPONSE, WP_COM, LimitMode.Top(1))

        assertThat(model.followers.size).isEqualTo(1)
        assertThat(model.followers.first().label).isEqualTo(FOLLOWER_RESPONSE.label)
    }

    @Test
    fun `maps posting activity and crops by start and end date`() {
        val startDay = Day(2019, 1, 20)
        val endDay = Day(2019, 2, 5)
        val dayBeforeStart = Calendar.getInstance()
        dayBeforeStart.set(2019, 1, 19)
        val dateOnStart = Calendar.getInstance()
        dateOnStart.set(2019, 1, 20)
        val dateOnEnd = Calendar.getInstance()
        dateOnEnd.set(2019, 2, 5)
        val dayAfterEnd = Calendar.getInstance()
        dayAfterEnd.set(2019, 2, 6)
        val postCount = 2
        val date = "2010-10-11"
        val formattedDate = Date(123)
        whenever(statsUtils.fromFormattedDate(date)).thenReturn(formattedDate)
        val longStreak = Streak(date, date, 150)
        val currentStreak = Streak(date, date, 150)
        val response = PostingActivityResponse(
                Streaks(longStreak, currentStreak),
                mapOf(
                        dayBeforeStart.timeInMillis / 1000 to 1,
                        dateOnStart.timeInMillis / 1000 to postCount,
                        dateOnEnd.timeInMillis / 1000 to postCount,
                        dayAfterEnd.timeInMillis / 1000 to 3
                )
        )
        val model = mapper.map(response, startDay, endDay)

        assertThat(model.months).hasSize(2)
        assertThat(model.months[0].days[20]).isEqualTo(postCount)
        assertThat(model.months[1].days[5]).isEqualTo(postCount)
        assertThat(model.streak.longestStreakStart).isEqualTo(formattedDate)
        assertThat(model.streak.longestStreakEnd).isEqualTo(formattedDate)
        assertThat(model.streak.longestStreakLength).isEqualTo(longStreak.length)
        assertThat(model.streak.currentStreakStart).isEqualTo(formattedDate)
        assertThat(model.streak.currentStreakEnd).isEqualTo(formattedDate)
        assertThat(model.streak.currentStreakLength).isEqualTo(currentStreak.length)
    }
}
