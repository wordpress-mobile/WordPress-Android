package org.wordpress.android.ui.stats.refresh.lists.widget.views

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.NEGATIVE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.NEUTRAL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.POSITIVE
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.OverviewMapper
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner::class)
class ViewsWidgetListViewModelTest {
    @Mock private lateinit var siteStore: SiteStore
    @Mock private lateinit var visitsAndViewsStore: VisitsAndViewsStore
    @Mock private lateinit var overviewMapper: OverviewMapper
    @Mock private lateinit var resourceProvider: ResourceProvider
    @Mock private lateinit var statsDateFormatter: StatsDateFormatter
    @Mock private lateinit var site: SiteModel
    private lateinit var viewModel: ViewsWidgetListViewModel
    private val siteId: Int = 15
    private val appWidgetId: Int = 1
    private val color = Color.LIGHT
    private val showChangeColumn = true
    @Before
    fun setUp() {
        viewModel = ViewsWidgetListViewModel(
                siteStore,
                visitsAndViewsStore,
                overviewMapper,
                resourceProvider,
                statsDateFormatter
        )
    }

    @Test
    fun `builds light ui model and shows change`() {
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(site)
        val firstViews: Long = 5
        val todayViews: Long = 20
        val dates = listOf(
                PeriodData("2019-01-06", firstViews, 0, 0, 0, 0, 0),
                PeriodData("2019-01-07", firstViews, 0, 0, 0, 0, 0),
                PeriodData("2019-01-08", todayViews, 0, 0, 0, 0, 0)
        )
        whenever(visitsAndViewsStore.getVisits(any(), eq(DAYS), eq(LimitMode.All))).thenReturn(
                VisitsAndViewsModel("2019-01-08", dates)
        )
        val todayString = "Today"
        whenever(resourceProvider.getString(R.string.stats_insights_today_stats)).thenReturn(todayString)
        (5 until 8).forEach {
            whenever(statsDateFormatter.printDate("2019-01-0$it")).thenReturn("Jan $it, 2019")
        }
        val change = "+10%"
        whenever(
                overviewMapper.buildTitle(
                        eq(dates[0]),
                        isNull(),
                        any(),
                        any(),
                        any(),
                        any()
                )
        ).thenReturn(ValueItem(firstViews.toFormattedString(), 0, false, change, POSITIVE, change))
        whenever(
                overviewMapper.buildTitle(
                        eq(dates[1]),
                        eq(dates[0]),
                        any(),
                        any(),
                        any(),
                        any()
                )
        ).thenReturn(ValueItem(todayViews.toFormattedString(), 0, true, change, NEGATIVE, change))
        whenever(
                overviewMapper.buildTitle(
                        eq(dates[2]),
                        eq(dates[1]),
                        any(),
                        any(),
                        any(),
                        any()
                )
        ).thenReturn(ValueItem(todayViews.toFormattedString(), 0, true, change, NEUTRAL, change))

        viewModel.start(siteId, color, showChangeColumn, appWidgetId)

        viewModel.onDataSetChanged { }

        viewModel.data.let { data ->
            assertThat(data).hasSize(dates.size)
            assertThat(data[0].layout).isEqualTo(R.layout.stats_views_widget_item_light)
            assertThat(data[0].key).isEqualTo(todayString)
            assertThat(data[0].isNeutralChangeVisible).isTrue()
            assertThat(data[0].isPositiveChangeVisible).isFalse()
            assertThat(data[0].isNegativeChangeVisible).isFalse()
            assertThat(data[0].change).isEqualTo(change)
            assertThat(data[1].key).isEqualTo("Jan 7, 2019")
            assertThat(data[1].isNeutralChangeVisible).isFalse()
            assertThat(data[1].isPositiveChangeVisible).isFalse()
            assertThat(data[1].isNegativeChangeVisible).isTrue()
            assertThat(data[1].change).isEqualTo(change)
            assertThat(data[2].key).isEqualTo("Jan 6, 2019")
            assertThat(data[2].isNeutralChangeVisible).isFalse()
            assertThat(data[2].isPositiveChangeVisible).isTrue()
            assertThat(data[2].isNegativeChangeVisible).isFalse()
            assertThat(data[2].change).isEqualTo(change)
        }
    }

    @Test
    fun `on missing site triggers error callback`() {
        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(null)

        viewModel.start(siteId, color, showChangeColumn, appWidgetId)

        var errorCallbackTriggered = false

        viewModel.onDataSetChanged { appWidgetId ->
            assertThat(appWidgetId).isEqualTo(this.appWidgetId)
            errorCallbackTriggered = true
        }

        assertThat(errorCallbackTriggered).isTrue()
    }
}
