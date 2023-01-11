package org.wordpress.android.ui.stats.refresh.lists.widget.alltime

import android.content.Context
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.stats.insights.AllTimeInsightsStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetBlockListProvider.BlockItemUiModel
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner::class)
class AllTimeWidgetBlockListViewModelTest {
    @Mock
    private lateinit var siteStore: SiteStore

    @Mock
    private lateinit var allTimeStore: AllTimeInsightsStore

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var statsUtils: StatsUtils

    @Mock
    private lateinit var site: SiteModel

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var allTimeWidgetUpdater: AllTimeWidgetUpdater

    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper
    private lateinit var viewModel: AllTimeWidgetBlockListViewModel
    private val siteId: Int = 15
    private val appWidgetId: Int = 1
    private val color = Color.LIGHT

    @Before
    fun setUp() {
        viewModel = AllTimeWidgetBlockListViewModel(
            siteStore,
            allTimeStore,
            resourceProvider,
            allTimeWidgetUpdater,
            appPrefsWrapper,
            statsUtils
        )
        viewModel.start(siteId, color, appWidgetId)
        whenever(statsUtils.toFormattedString(any<Int>(), any())).then { (it.arguments[0] as Int).toString() }
    }

    @Test
    fun `builds ui model`() {
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(site)
        val viewsKey = "Views"
        val visitorsKey = "Visitors"
        val postsKey = "Posts"
        val bestKey = "Best"
        val views = 500
        val visitors = 100
        val posts = 50
        val viewsBestDayTotal = 300
        whenever(resourceProvider.getString(string.stats_views)).thenReturn(viewsKey)
        whenever(resourceProvider.getString(string.stats_visitors)).thenReturn(visitorsKey)
        whenever(resourceProvider.getString(string.posts)).thenReturn(postsKey)
        whenever(resourceProvider.getString(string.stats_insights_best_ever)).thenReturn(bestKey)
        whenever(allTimeStore.getAllTimeInsights(site)).thenReturn(
            InsightsAllTimeModel(
                150L,
                null,
                visitors,
                views,
                posts,
                "Monday",
                viewsBestDayTotal
            )
        )
        viewModel.onDataSetChanged(context)

        Assertions.assertThat(viewModel.data).hasSize(2)
        assertListItem(viewModel.data[0], viewsKey, views, visitorsKey, visitors)
        assertListItem(viewModel.data[1], postsKey, posts, bestKey, viewsBestDayTotal)
        verify(appPrefsWrapper).setAppWidgetHasData(true, appWidgetId)
    }

    @Test
    fun `shows error when site is missing`() {
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(null)

        viewModel.onDataSetChanged(context)

        verify(allTimeWidgetUpdater).updateAppWidget(eq(context), any(), isNull())
    }

    private fun assertListItem(
        listItem: BlockItemUiModel,
        startKey: String,
        startValue: Int,
        endKey: String,
        endValue: Int
    ) {
        Assertions.assertThat(listItem.layout).isEqualTo(R.layout.stats_widget_block_item_light)
        Assertions.assertThat(listItem.localSiteId).isEqualTo(siteId)
        Assertions.assertThat(listItem.startKey).isEqualTo(startKey)
        Assertions.assertThat(listItem.startValue).isEqualTo(startValue.toString())
        Assertions.assertThat(listItem.endKey).isEqualTo(endKey)
        Assertions.assertThat(listItem.endValue).isEqualTo(endValue.toString())
    }
}
