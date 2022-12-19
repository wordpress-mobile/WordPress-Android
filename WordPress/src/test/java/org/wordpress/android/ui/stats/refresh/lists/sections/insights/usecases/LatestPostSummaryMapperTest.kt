package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsSinceLabelFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class LatestPostSummaryMapperTest {
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var statsSinceLabelFormatter: StatsSinceLabelFormatter
    @Mock lateinit var statsUtils: StatsUtils
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    private lateinit var mapper: LatestPostSummaryMapper
    private val date = Date(10)
    private val postTitle = "post title"
    private val siteId = 1L
    private val postId = 10L
    private val postURL = "url"
    private val featuredImageURL = ""
    @Before
    fun setUp() {
        mapper = LatestPostSummaryMapper(
                statsSinceLabelFormatter,
                resourceProvider,
                statsDateFormatter,
                statsUtils
        )
    }

    @Test
    fun `builds empty message on null model`() {
        val emptyMessage = "empty message"
        whenever(resourceProvider.getString(R.string.stats_insights_latest_post_empty)).thenReturn(emptyMessage)

        val result = mapper.buildMessageItem(null) { }

        assertThat(result.text).isEqualTo(emptyMessage)
        assertThat(result.links).isNull()
    }

    @Test
    fun `builds message with no engagement and link`() {
        val viewCount = 0
        val model = InsightsLatestPostModel(
                siteId,
                postTitle,
                postURL,
                date,
                postId,
                viewCount,
                0,
                0,
                listOf(),
                featuredImageURL
        )

        val sinceTimeLabel = "10 mins"
        whenever(statsSinceLabelFormatter.getSinceLabelLowerCase(date)).thenReturn(sinceTimeLabel)
        val messageWithNoEngagement = "message with no engagement"
        whenever(
                resourceProvider.getString(
                        R.string.stats_insights_latest_post_with_no_engagement,
                        sinceTimeLabel,
                        postTitle
                )
        ).thenReturn(messageWithNoEngagement)

        var clickedPostId: Long? = null
        var clickedPostUrl: String? = null
        val result = mapper.buildMessageItem(model) { params ->
            clickedPostId = params.postId
            clickedPostUrl = params.postUrl
        }

        assertThat(result.text).isEqualTo(messageWithNoEngagement)
        assertThat(result.links).hasSize(1)

        result.links!![0].navigationAction.click()

        assertThat(clickedPostId).isEqualTo(model.postId)
        assertThat(clickedPostUrl).isEqualTo(model.postURL)
    }

    @Test
    fun `builds message with engagement`() {
        val viewCount = 10
        val model = InsightsLatestPostModel(
                siteId,
                postTitle,
                postURL,
                date,
                postId,
                viewCount,
                0,
                0,
                listOf(),
                featuredImageURL
        )

        val sinceTimeLabel = "10 mins"
        whenever(statsSinceLabelFormatter.getSinceLabelLowerCase(date)).thenReturn(sinceTimeLabel)
        val messageWithEngagement = "message with no engagement"
        whenever(
                resourceProvider.getString(
                        R.string.stats_insights_latest_post_message,
                        sinceTimeLabel,
                        postTitle
                )
        ).thenReturn(messageWithEngagement)

        val result = mapper.buildMessageItem(model) { }

        assertThat(result.text).isEqualTo(messageWithEngagement)
        assertThat(result.links).hasSize(1)
    }

    @Test
    fun `builds chart item with parsed date`() {
        val unparsedDate = "2018-01-01"
        val parsedDate = "Jan 1, 2018"
        val dayViews = listOf(unparsedDate to 50)
        whenever(statsDateFormatter.printDate(unparsedDate)).thenReturn(parsedDate)

        val barChartItem = mapper.buildBarChartItem(dayViews)

        barChartItem.entries.apply {
            assertThat(this).hasSize(1)
            assertThat(this[0].label).isEqualTo(parsedDate)
            assertThat(this[0].id).isEqualTo(unparsedDate)
            assertThat(this[0].value).isEqualTo(50)
        }
    }

    @Test
    fun `builds chart item with only last 30 dates`() {
        val dayViews = mutableListOf<Pair<String, Int>>()
        whenever(statsDateFormatter.printDate(any<String>())).thenReturn("mapped date")

        for (month in 10..12) {
            for (day in 10..30) {
                dayViews.add("2018-$month-$day" to month + day)
            }
        }

        val barChartItem = mapper.buildBarChartItem(dayViews)

        barChartItem.entries.apply {
            assertThat(this).hasSize(30)
        }
    }

    @Test
    fun `strips HTML from post title in latest post summary`() {
        val postTitleWithHtml = "<b>Title</b> with <font color=\"red\">HTML</color>"

        val viewCount = 0
        val model = InsightsLatestPostModel(
                siteId,
                postTitleWithHtml,
                postURL,
                date,
                postId,
                viewCount,
                0,
                0,
                listOf(),
                featuredImageURL
        )

        val sinceTimeLabel = "10 mins"
        whenever(statsSinceLabelFormatter.getSinceLabelLowerCase(date)).thenReturn(sinceTimeLabel)

        whenever(
                resourceProvider.getString(
                        eq(R.string.stats_insights_latest_post_with_no_engagement),
                        eq(sinceTimeLabel),
                        anyString()
                )
        ).thenAnswer { "message with no engagement for post ${it.getArgument<String>(2)}" }

        val result = mapper.buildMessageItem(model) {}

        assertThat(result.text).isEqualTo("message with no engagement for post Title with HTML")
    }
}
