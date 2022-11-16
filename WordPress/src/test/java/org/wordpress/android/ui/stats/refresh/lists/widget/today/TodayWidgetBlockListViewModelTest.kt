package org.wordpress.android.ui.stats.refresh.lists.widget.today

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
import org.wordpress.android.fluxc.model.stats.VisitsModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.stats.insights.TodayInsightsStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetBlockListProvider.BlockItemUiModel
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner::class)
class TodayWidgetBlockListViewModelTest {
    @Mock private lateinit var siteStore: SiteStore
    @Mock private lateinit var store: TodayInsightsStore
    @Mock private lateinit var resourceProvider: ResourceProvider
    @Mock private lateinit var statsUtils: StatsUtils
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var context: Context
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock private lateinit var todayWidgetUpdater: TodayWidgetUpdater
    private lateinit var viewModel: TodayWidgetBlockListViewModel
    private val siteId: Int = 15
    private val appWidgetId: Int = 1
    private val color = Color.LIGHT
    @Before
    fun setUp() {
        viewModel = TodayWidgetBlockListViewModel(
                siteStore,
                store,
                resourceProvider,
                todayWidgetUpdater,
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
        val likesKey = "Likes"
        val commentsKey = "Comments"
        val views = 500
        val visitors = 100
        val likes = 50
        val comments = 300
        whenever(resourceProvider.getString(string.stats_views)).thenReturn(viewsKey)
        whenever(resourceProvider.getString(string.stats_visitors)).thenReturn(visitorsKey)
        whenever(resourceProvider.getString(string.likes)).thenReturn(likesKey)
        whenever(resourceProvider.getString(string.stats_comments)).thenReturn(commentsKey)
        whenever(store.getTodayInsights(site)).thenReturn(
                VisitsModel("2019-10-10", views, visitors, likes, 0, comments, 0)
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
