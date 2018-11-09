package org.wordpress.android.ui.stats.refresh

import com.nhaarman.mockito_kotlin.isNull
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.InsightsStore.OnInsightsFetched
import org.wordpress.android.fluxc.store.InsightsStore.StatsError
import org.wordpress.android.fluxc.store.InsightsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class LatestPostSummaryUseCaseTest {
    @Mock lateinit var insightsStore: InsightsStore
    @Mock lateinit var latestPostSummaryMapper: LatestPostSummaryMapper
    @Mock lateinit var site: SiteModel
    private lateinit var useCase: LatestPostSummaryUseCase
    @Before
    fun setUp() {
        useCase = LatestPostSummaryUseCase(insightsStore, latestPostSummaryMapper)
    }

    @Test
    fun `returns Failed item on error`() = test {
        val forced = false
        val message = "message"
        whenever(insightsStore.fetchLatestPostInsights(site, forced)).thenReturn(
                OnInsightsFetched(
                        StatsError(
                                GENERIC_ERROR,
                                message
                        )
                )
        )

        val result = useCase.loadLatestPostSummary(site, forced)

        assertThat(result).isInstanceOf(Failed::class.java)
        (result as Failed).let {
            assertThat(result.failedType).isEqualTo(R.string.stats_insights_latest_post_summary)
            assertThat(result.errorMessage).isEqualTo(message)
        }
    }

    @Test
    fun `returns create empty item when model is missing`() = test {
        val forced = false
        whenever(insightsStore.fetchLatestPostInsights(site, forced)).thenReturn(OnInsightsFetched())
        val textItem = mock<Text>()
        whenever(latestPostSummaryMapper.buildMessageItem(isNull())).thenReturn(textItem)

        val result = useCase.loadLatestPostSummary(site, forced)

        assertThat(result).isInstanceOf(ListInsightItem::class.java)
        (result as ListInsightItem).items.apply {
            val title = this[0] as Title
            assertThat(title.text).isEqualTo(R.string.stats_insights_latest_post_summary)
            assertThat(this[1]).isEqualTo(textItem)
            val link = this[2] as Link
            assertThat(link.icon).isEqualTo(R.drawable.ic_create_blue_medium_24dp)
            assertThat(link.text).isEqualTo(R.string.stats_insights_create_post)
        }
    }

    @Test
    fun `returns share empty item when views are empty`() = test {
        val forced = false
        val viewsCount = 0
        val postTitle = "title"
        val model = buildLatestPostModel(postTitle, viewsCount, listOf())
        whenever(insightsStore.fetchLatestPostInsights(site, forced)).thenReturn(
                OnInsightsFetched(
                        model
                )
        )
        val textItem = mock<Text>()
        whenever(latestPostSummaryMapper.buildMessageItem(model)).thenReturn(textItem)

        val result = useCase.loadLatestPostSummary(site, forced)

        assertThat(result).isInstanceOf(ListInsightItem::class.java)
        (result as ListInsightItem).items.apply {
            val title = this[0] as Title
            assertThat(title.text).isEqualTo(R.string.stats_insights_latest_post_summary)
            assertThat(this[1]).isEqualTo(textItem)
            val link = this[2] as Link
            assertThat(link.icon).isEqualTo(R.drawable.ic_share_blue_medium_24dp)
            assertThat(link.text).isEqualTo(R.string.stats_insights_share_post)
        }
    }

    @Test
    fun `returns populated item when views are not empty`() = test {
        val forced = false
        val viewsCount = 10
        val postTitle = "title"
        val dayViews = listOf("2018-01-01" to 10)
        val model = buildLatestPostModel(postTitle, viewsCount, dayViews)
        whenever(insightsStore.fetchLatestPostInsights(site, forced)).thenReturn(
                OnInsightsFetched(
                        model
                )
        )
        val textItem = mock<Text>()
        whenever(latestPostSummaryMapper.buildMessageItem(model)).thenReturn(textItem)
        val columnItem = mock<Columns>()
        whenever(latestPostSummaryMapper.buildColumnItem(viewsCount, 0, 0)).thenReturn(columnItem)
        val chartItem = mock<BarChartItem>()
        whenever(latestPostSummaryMapper.buildBarChartItem(dayViews)).thenReturn(chartItem)

        val result = useCase.loadLatestPostSummary(site, forced)

        assertThat(result).isInstanceOf(ListInsightItem::class.java)
        (result as ListInsightItem).items.apply {
            val title = this[0] as Title
            assertThat(title.text).isEqualTo(R.string.stats_insights_latest_post_summary)
            assertThat(this[1]).isEqualTo(textItem)
            assertThat(this[2]).isEqualTo(columnItem)
            assertThat(this[3]).isEqualTo(chartItem)
            val link = this[4] as Link
            assertThat(link.icon).isNull()
            assertThat(link.text).isEqualTo(R.string.stats_insights_view_more)
        }
    }

    private fun buildLatestPostModel(
        postTitle: String,
        viewsCount: Int,
        dayViews: List<Pair<String, Int>>
    ): InsightsLatestPostModel {
        return InsightsLatestPostModel(
                1L,
                postTitle,
                "url",
                Date(),
                10L,
                viewsCount,
                0,
                0,
                dayViews
        )
    }
}
