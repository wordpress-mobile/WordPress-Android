package org.wordpress.android.ui.stats.refresh

import android.content.Context
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
import org.wordpress.android.ui.stats.StatsUtilsWrapper
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Date
import java.util.Locale

@RunWith(MockitoJUnitRunner::class)
class LatestPostSummaryMapperTest {
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var statsUtilsWrapper: StatsUtilsWrapper
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    private lateinit var mapper: LatestPostSummaryMapper
    private val date = Date(10)
    private val postTitle = "post title"
    private val siteId = 1L
    private val postId = 10L
    private val postURL = "url"
    @Before
    fun setUp() {
        mapper = LatestPostSummaryMapper(statsUtilsWrapper, resourceProvider, localeManagerWrapper)
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.US)
    }

    @Test
    fun `builds empty message on null model`() {
        val emptyMessage = "empty message"
        whenever(resourceProvider.getString(R.string.stats_insights_latest_post_empty)).thenReturn(emptyMessage)

        val result = mapper.buildMessageItem(null)

        assertThat(result.text).isEqualTo(emptyMessage)
        assertThat(result.links).isNull()
    }

    @Test
    fun `builds message with no engagement and link`() {
        val viewCount = 0
        val model = InsightsLatestPostModel(siteId, postTitle, postURL, date, postId, viewCount, 0, 0, listOf())

        val sinceTimeLabel = "10 mins"
        whenever(statsUtilsWrapper.getSinceLabelLowerCase(date)).thenReturn(sinceTimeLabel)
        val messageWithNoEngagement = "message with no engagement"
        whenever(
                resourceProvider.getString(
                        string.stats_insights_latest_post_with_no_engagement,
                        sinceTimeLabel,
                        postTitle
                )
        ).thenReturn(messageWithNoEngagement)

        val result = mapper.buildMessageItem(model)

        assertThat(result.text).isEqualTo(messageWithNoEngagement)
        assertThat(result.links).hasSize(1)

        val context = mock<Context>()
        result.links!![0].action(context)

        verify(statsUtilsWrapper).openPostInReaderOrInAppWebview(
                context,
                siteId,
                postId.toString(),
                postURL
        )
    }

    @Test
    fun `builds message with engagement`() {
        val viewCount = 10
        val model = InsightsLatestPostModel(siteId, postTitle, postURL, date, postId, viewCount, 0, 0, listOf())

        val sinceTimeLabel = "10 mins"
        whenever(statsUtilsWrapper.getSinceLabelLowerCase(date)).thenReturn(sinceTimeLabel)
        val messageWithEngagement = "message with no engagement"
        whenever(
                resourceProvider.getString(
                        string.stats_insights_latest_post_message,
                        sinceTimeLabel,
                        postTitle
                )
        ).thenReturn(messageWithEngagement)

        val result = mapper.buildMessageItem(model)

        assertThat(result.text).isEqualTo(messageWithEngagement)
        assertThat(result.links).hasSize(1)

        val context = mock<Context>()
        result.links!![0].action(context)

        verify(statsUtilsWrapper).openPostInReaderOrInAppWebview(
                context,
                siteId,
                postId.toString(),
                postURL
        )
    }

    @Test
    fun `builds columns item with formatted items`() {
        val postLikeCount = 20000
        val postCommentCount = 15000000
        val postViewsCount = 10

        val columnItem = mapper.buildColumnItem(postViewsCount, postLikeCount, postCommentCount)

        columnItem.headers.apply {
            assertThat(this).hasSize(3)
            assertThat(this[0]).isEqualTo(R.string.stats_views)
            assertThat(this[1]).isEqualTo(R.string.stats_likes)
            assertThat(this[2]).isEqualTo(R.string.stats_comments)
        }

        columnItem.values.apply {
            assertThat(this).hasSize(3)
            assertThat(this[0]).isEqualTo("10")
            assertThat(this[1]).isEqualTo("20k")
            assertThat(this[2]).isEqualTo("15M")
        }
    }

    @Test
    fun `builds chart item with parsed date`() {
        val dayViews = listOf("2018-01-01" to 50)

        val barChartItem = mapper.buildBarChartItem(dayViews)

        barChartItem.entries.apply {
            assertThat(this).hasSize(1)
            assertThat(this[0].first).isEqualTo("Jan 1, 2018")
            assertThat(this[0].second).isEqualTo(50)
        }
    }

    @Test
    fun `builds chart item with only last 30 dates`() {
        val dayViews = mutableListOf<Pair<String, Int>>()

        for (month in 10..12) {
            for (day in 10..30) {
                dayViews.add("2018-$month-$day" to month + day)
            }
        }

        val barChartItem = mapper.buildBarChartItem(dayViews)

        barChartItem.entries.apply {
            assertThat(this).hasSize(30)
            assertThat(this.first().first).isEqualTo("Nov 22, 2018")
            assertThat(this.first().second).isEqualTo(33)
            assertThat(this.last().first).isEqualTo("Dec 30, 2018")
            assertThat(this.last().second).isEqualTo(42)
        }
    }
}
