package org.wordpress.android.ui.stats.refresh.lists.widget.weeks

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.lists.widget.weeks.WeekViewsWidgetListViewModel.WeekItemUiModel
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner::class)
class WeekViewsWidgetListViewModelTest {
    @Mock
    private lateinit var siteStore: SiteStore

    @Mock
    private lateinit var store: VisitsAndViewsStore

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    private lateinit var statsUtils: StatsUtils

    @Mock
    private lateinit var site: SiteModel
    private lateinit var viewModel: WeekViewsWidgetListViewModel
    private val siteId: Int = 15
    private val appWidgetId: Int = 1
    private val color = Color.LIGHT

    @Before
    fun setUp() {
        viewModel = WeekViewsWidgetListViewModel(
            siteStore,
            store,
            resourceProvider,
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
        whenever(resourceProvider.getString(string.stats_views)).thenReturn(viewsKey)
        whenever(resourceProvider.getString(string.stats_visitors)).thenReturn(visitorsKey)
        whenever(resourceProvider.getString(string.likes)).thenReturn(likesKey)
        whenever(resourceProvider.getString(string.stats_comments)).thenReturn(commentsKey)
        whenever(store.getVisits(site, WEEKS, LimitMode.All)).thenReturn(
            VisitsAndViewsModel("2019-10-10", listOf(periodData))
        )

        viewModel.onDataSetChanged { }

        Assertions.assertThat(viewModel.data).hasSize(4)
        assertListItem(viewModel.data[0], viewsKey, views)
        assertListItem(viewModel.data[1], visitorsKey, visitors)
        assertListItem(viewModel.data[2], likesKey, likes)
        assertListItem(viewModel.data[3], commentsKey, comments)
        verify(appPrefsWrapper).setAppWidgetHasData(true, appWidgetId)
    }

    @Test
    fun `shows error when site is missing`() {
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(null)

        var onError: Int? = null
        viewModel.onDataSetChanged { onError = it }

        Assertions.assertThat(onError).isEqualTo(appWidgetId)
    }

    private fun assertListItem(listItem: WeekItemUiModel, key: String, value: Long) {
        Assertions.assertThat(listItem.layout).isEqualTo(R.layout.stats_views_widget_item_light)
        Assertions.assertThat(listItem.localSiteId).isEqualTo(siteId)
        Assertions.assertThat(listItem.key).isEqualTo(key)
        Assertions.assertThat(listItem.value).isEqualTo(value.toString())
    }
}
