package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.InsightsStore.OnInsightsFetched
import org.wordpress.android.fluxc.store.InsightsStore.StatsError
import org.wordpress.android.fluxc.store.InsightsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.Error
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.BlockList
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.SharePost
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewPostDetailStats
import java.util.Date

class LatestPostSummaryUseCaseTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: InsightsStore
    @Mock lateinit var latestPostSummaryMapper: LatestPostSummaryMapper
    @Mock lateinit var site: SiteModel
    private lateinit var useCase: LatestPostSummaryUseCase
    @Before
    fun setUp() = test {
        useCase = LatestPostSummaryUseCase(
                Dispatchers.Unconfined,
                insightsStore,
                latestPostSummaryMapper
        )
        useCase.navigationTarget.observeForever {}
        whenever(insightsStore.getLatestPostInsights(site)).thenReturn(null)
    }

    @Test
    fun `returns Failed item on error`() = test {
        val forced = false
        val refresh = true
        val message = "message"
        whenever(insightsStore.fetchLatestPostInsights(site, forced)).thenReturn(
                OnInsightsFetched(
                        StatsError(
                                GENERIC_ERROR,
                                message
                        )
                )
        )

        val result = loadLatestPostSummary(refresh, forced)

        assertThat(result).isInstanceOf(Error::class.java)
        val failed = result as Error
        assertThat(failed.errorType).isEqualTo(R.string.stats_insights_latest_post_summary)
        assertThat(failed.errorMessage).isEqualTo(message)
    }

    @Test
    fun `returns null item when model is missing`() = test {
        val forced = false
        val refresh = true
        whenever(insightsStore.fetchLatestPostInsights(site, forced)).thenReturn(OnInsightsFetched())

        val result = loadLatestPostSummary(refresh, forced)

        assertThat(result).isNull()
    }

    @Test
    fun `returns share empty item when views are empty`() = test {
        val forced = false
        val refresh = true
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

        val result = loadLatestPostSummary(refresh, forced)

        assertThat(result).isInstanceOf(BlockList::class.java)
        (result as BlockList).items.apply {
            val title = this[0] as Title
            assertThat(title.text).isEqualTo(R.string.stats_insights_latest_post_summary)
            assertThat(this[1]).isEqualTo(textItem)
            val link = this[2] as Link
            assertThat(link.icon).isEqualTo(R.drawable.ic_share_blue_medium_24dp)
            assertThat(link.text).isEqualTo(R.string.stats_insights_share_post)

            link.toNavigationTarget().apply {
                assertThat(this).isInstanceOf(SharePost::class.java)
                assertThat((this as SharePost).url).isEqualTo(model.postURL)
                assertThat(this.title).isEqualTo(model.postTitle)
            }
        }
    }

    @Test
    fun `returns populated item when views are not empty`() = test {
        val forced = false
        val refresh = true
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

        val result = loadLatestPostSummary(refresh, forced)

        assertThat(result).isInstanceOf(BlockList::class.java)
        (result as BlockList).items.apply {
            val title = this[0] as Title
            assertThat(title.text).isEqualTo(R.string.stats_insights_latest_post_summary)
            assertThat(this[1]).isEqualTo(textItem)
            assertThat(this[2]).isEqualTo(columnItem)
            assertThat(this[3]).isEqualTo(chartItem)
            val link = this[4] as Link
            assertThat(link.icon).isNull()
            assertThat(link.text).isEqualTo(R.string.stats_insights_view_more)

            link.toNavigationTarget().apply {
                assertThat(this).isInstanceOf(ViewPostDetailStats::class.java)
                assertThat((this as ViewPostDetailStats).postUrl).isEqualTo(model.postURL)
                assertThat(this.postTitle).isEqualTo(model.postTitle)
                assertThat(this.postID).isEqualTo(model.postId.toString())
                assertThat(this.siteID).isEqualTo(model.siteId)
            }
        }
    }

    private suspend fun loadLatestPostSummary(
        refresh: Boolean,
        forced: Boolean
    ): StatsBlock? {
        var result: StatsBlock? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(site, refresh, forced)
        return result
    }

    private fun Link.toNavigationTarget(): NavigationTarget? {
        var navigationTarget: NavigationTarget? = null
        useCase.navigationTarget.observeForever { navigationTarget = it }
        this.action()
        return navigationTarget
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
