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
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class LatestPostSummaryMapperTest {
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var statsUtilsWrapper: StatsUtilsWrapper
    private lateinit var mapper: LatestPostSummaryMapper
    @Before
    fun setUp() {
        mapper = LatestPostSummaryMapper(statsUtilsWrapper, resourceProvider)
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
        val date = Date(10)
        val viewCount = 0
        val postTitle = "post title"
        val siteId = 1L
        val postId = 10L
        val postURL = "url"
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
        val date = Date(10)
        val viewCount = 10
        val postTitle = "post title"
        val siteId = 1L
        val postId = 10L
        val postURL = "url"
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
}
