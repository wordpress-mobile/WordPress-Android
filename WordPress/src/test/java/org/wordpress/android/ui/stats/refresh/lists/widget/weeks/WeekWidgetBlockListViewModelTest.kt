package org.wordpress.android.ui.stats.refresh.lists.widget.weeks

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
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetBlockListProvider.BlockItemUiModel
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner::class)
class WeekWidgetBlockListViewModelTest {
    @Mock
    private lateinit var siteStore: SiteStore

    @Mock
    private lateinit var store: VisitsAndViewsStore

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var statsUtils: StatsUtils

    @Mock
    private lateinit var site: SiteModel

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    private lateinit var todayWidgetUpdater: WeekViewsWidgetUpdater
    private lateinit var viewModel: WeekWidgetBlockListViewModel
    private val siteId: Int = 15
    private val appWidgetId: Int = 1
    private val color = Color.LIGHT

    @Before
    fun setUp() {
        viewModel = WeekWidgetBlockListViewModel(
            siteStore,
            store,
            resourceProvider,
            todayWidgetUpdater,
            appPrefsWrapper,
            statsUtils
        )
        viewModel.start(siteId, color, appWidgetId)
        whenever(statsUtils.toFormattedString(any<Long>(), any())).then { (it.arguments[0] as Long).toString() }
    }

    @Test
    fun `builds ui model`() {
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(site)
        val viewsKey = "Views"
        val visitorsKey = "Visitors"
        val likesKey = "Likes"
        val commentsKey = "Comments"
        val views = 500L
        val visitors = 100L
        val likes = 50L
        val comments = 300L
        val periodData = PeriodData("2019-10-10", views, visitors, likes, 0, comments, 0)
        whenever(resourceProvider.getString(R.string.stats_views)).thenReturn(viewsKey)
        whenever(resourceProvider.getString(R.string.stats_visitors)).thenReturn(visitorsKey)
        whenever(resourceProvider.getString(R.string.likes)).thenReturn(likesKey)
        whenever(resourceProvider.getString(R.string.stats_comments)).thenReturn(commentsKey)
        whenever(store.getVisits(site, WEEKS, LimitMode.All)).thenReturn(
            VisitsAndViewsModel("2019-10-10", listOf(periodData))
        )

        viewModel.onDataSetChanged(context)

        Assertions.assertThat(viewModel.data).hasSize(2)
        assertListItem(viewModel.data[0], viewsKey, views, visitorsKey, visitors)
        assertListItem(viewModel.data[1], likesKey, likes, commentsKey, comments)
        verify(appPrefsWrapper).setAppWidgetHasData(true, appWidgetId)
    }

    @Test
    fun `shows error when site is missing`() {
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(null)

        viewModel.onDataSetChanged(context)

        verify(todayWidgetUpdater).updateAppWidget(eq(context), any(), isNull())
    }

    private fun assertListItem(
        listItem: BlockItemUiModel,
        startKey: String,
        startValue: Long,
        endKey: String,
        endValue: Long
    ) {
        Assertions.assertThat(listItem.layout).isEqualTo(R.layout.stats_widget_block_item_light)
        Assertions.assertThat(listItem.localSiteId).isEqualTo(siteId)
        Assertions.assertThat(listItem.startKey).isEqualTo(startKey)
        Assertions.assertThat(listItem.startValue).isEqualTo(startValue.toString())
        Assertions.assertThat(listItem.endKey).isEqualTo(endKey)
        Assertions.assertThat(listItem.endValue).isEqualTo(endValue.toString())
    }
}
