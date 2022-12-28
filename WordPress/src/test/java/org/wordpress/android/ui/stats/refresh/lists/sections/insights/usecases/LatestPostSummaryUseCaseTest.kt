package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.insights.LatestPostInsightsStore
import org.wordpress.android.ui.stats.refresh.NavigationTarget
import org.wordpress.android.ui.stats.refresh.NavigationTarget.SharePost
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPostDetailStats
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date

@ExperimentalCoroutinesApi
class LatestPostSummaryUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var insightsStore: LatestPostInsightsStore
    @Mock
    lateinit var latestPostSummaryMapper: LatestPostSummaryMapper
    @Mock
    lateinit var statsSiteProvider: StatsSiteProvider
    @Mock
    lateinit var site: SiteModel
    @Mock
    lateinit var tracker: AnalyticsTrackerWrapper
    @Mock
    lateinit var popupMenuHandler: ItemPopupMenuHandler
    @Mock
    lateinit var contentDescriptionHelper: ContentDescriptionHelper
    @Mock
    lateinit var statsUtils: StatsUtils
    private lateinit var useCase: LatestPostSummaryUseCase

    @Before
    fun setUp() = test {
        useCase = LatestPostSummaryUseCase(
            testDispatcher(),
            testDispatcher(),
            insightsStore,
            statsSiteProvider,
            latestPostSummaryMapper,
            tracker,
            popupMenuHandler,
            statsUtils,
            contentDescriptionHelper
        )
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        useCase.navigationTarget.observeForever {}
        whenever(
            contentDescriptionHelper.buildContentDescription(
                any(),
                any<Int>()
            )
        ).thenReturn("likes: 10")
        whenever(statsUtils.toFormattedString(any<Int>(), any())).then { (it.arguments[0] as Int).toString() }
    }

    @Test
    fun `returns Failed item on error`() = test {
        val forced = false
        val refresh = true
        val message = "message"
        whenever(insightsStore.fetchLatestPostInsights(site, forced)).thenReturn(
            OnStatsFetched(
                StatsError(
                    GENERIC_ERROR,
                    message
                )
            )
        )

        val result = loadLatestPostSummary(refresh, forced)

        assertThat(result!!.state).isEqualTo(UseCaseState.ERROR)
    }

    @Test
    fun `returns empty item when model is missing`() = test {
        val forced = false
        val refresh = true
        whenever(insightsStore.fetchLatestPostInsights(site, forced)).thenReturn(OnStatsFetched())

        val result = loadLatestPostSummary(refresh, forced)

        assertThat(result!!.state).isEqualTo(UseCaseState.EMPTY)
    }

    @Test
    fun `returns share empty item when views are empty`() = test {
        val forced = false
        val refresh = true
        val viewsCount = 0
        val postTitle = "title"
        val model = buildLatestPostModel(postTitle, viewsCount, listOf())
        whenever(insightsStore.getLatestPostInsights(site)).thenReturn(model)
        whenever(insightsStore.fetchLatestPostInsights(site, forced)).thenReturn(
            OnStatsFetched(
                model
            )
        )
        val textItem = mock<Text>()
        whenever(latestPostSummaryMapper.buildMessageItem(eq(model), any())).thenReturn(textItem)

        val result = loadLatestPostSummary(refresh, forced)

        assertThat(result!!.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            val title = this[0] as Title
            assertThat(title.textResource).isEqualTo(R.string.stats_insights_latest_post_summary)
            assertThat(this[1]).isEqualTo(textItem)
            val link = this[2] as Link
            assertThat(link.icon).isEqualTo(R.drawable.ic_share_white_24dp)
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
        whenever(insightsStore.getLatestPostInsights(site)).thenReturn(model)
        whenever(insightsStore.fetchLatestPostInsights(site, forced)).thenReturn(
            OnStatsFetched(
                model
            )
        )
        val textItem = mock<Text>()
        whenever(latestPostSummaryMapper.buildMessageItem(eq(model), any())).thenReturn(textItem)
        val chartItem = mock<BarChartItem>()
        whenever(latestPostSummaryMapper.buildBarChartItem(dayViews)).thenReturn(chartItem)

        val result = loadLatestPostSummary(refresh, forced)

        assertThat(result!!.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            val title = this[0] as Title
            assertThat(title.textResource).isEqualTo(R.string.stats_insights_latest_post_summary)
            assertThat(this[1]).isEqualTo(textItem)
            val valueItem = this[2] as ValueItem
            assertThat(valueItem.value).isEqualTo(viewsCount.toString())
            assertThat(valueItem.unit).isEqualTo(R.string.stats_views)
            assertThat(this[3]).isEqualTo(chartItem)
            val likesItem = this[4] as ListItemWithIcon
            assertThat(likesItem.textResource).isEqualTo(R.string.stats_likes)
            assertThat(likesItem.value).isEqualTo("0")
            val commentsItem = this[5] as ListItemWithIcon
            assertThat(commentsItem.textResource).isEqualTo(R.string.stats_comments)
            assertThat(commentsItem.value).isEqualTo("0")
            val link = this[6] as Link
            assertThat(link.icon).isNull()
            assertThat(link.text).isEqualTo(R.string.stats_insights_view_more)

            link.toNavigationTarget().apply {
                assertThat(this).isInstanceOf(ViewPostDetailStats::class.java)
                assertThat((this as ViewPostDetailStats).postUrl).isEqualTo(model.postURL)
                assertThat(this.postTitle).isEqualTo(model.postTitle)
                assertThat(this.postId).isEqualTo(model.postId)
            }
        }
    }

    private suspend fun loadLatestPostSummary(
        refresh: Boolean,
        forced: Boolean
    ): UseCaseModel? {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        advanceUntilIdle()
        return result
    }

    private fun Link.toNavigationTarget(): NavigationTarget? {
        var navigationTarget: NavigationTarget? = null
        useCase.navigationTarget.observeForever { navigationTarget = it?.getContentIfNotHandled() }
        this.navigateAction.click()
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
            dayViews,
            ""
        )
    }
}
