package org.wordpress.android.ui.stats.refresh.lists.widget

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
import org.wordpress.android.fluxc.model.stats.LimitMode.Top
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
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsViewsWidgetConfigureViewModel.Color
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner::class)
class ViewsListViewModelTest {
    @Mock private lateinit var siteStore: SiteStore
    @Mock private lateinit var visitsAndViewsStore: VisitsAndViewsStore
    @Mock private lateinit var overviewMapper: OverviewMapper
    @Mock private lateinit var resourceProvider: ResourceProvider
    @Mock private lateinit var statsDateFormatter: StatsDateFormatter
    @Mock private lateinit var site: SiteModel
    private lateinit var viewModel: ViewsListViewModel
    private val siteId: Long = 15
    @Before
    fun setUp() {
        viewModel = ViewsListViewModel(
                siteStore,
                visitsAndViewsStore,
                overviewMapper,
                resourceProvider,
                statsDateFormatter
        )
        whenever(siteStore.getSiteBySiteId(siteId)).thenReturn(site)
    }

    @Test
    fun `builds light ui model and shows change`() {
        val color = Color.LIGHT
        val showChangeColumn = true
        val firstViews: Long = 5
        val todayViews: Long = 20
        val dates = listOf(
                PeriodData("2019-01-06", firstViews, 0, 0, 0, 0, 0),
                PeriodData("2019-01-07", firstViews, 0, 0, 0, 0, 0),
                PeriodData("2019-01-08", todayViews, 0, 0, 0, 0, 0)
        )
        whenever(visitsAndViewsStore.getVisits(any(), eq(DAYS), eq(Top(LIST_ITEM_COUNT + 1)), any())).thenReturn(
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
                        any()
                )
        ).thenReturn(ValueItem(firstViews.toFormattedString(), 0, false, change, POSITIVE))
        whenever(
                overviewMapper.buildTitle(
                        eq(dates[1]),
                        eq(dates[0]),
                        any(),
                        any()
                )
        ).thenReturn(ValueItem(todayViews.toFormattedString(), 0, true, change, NEGATIVE))
        whenever(
                overviewMapper.buildTitle(
                        eq(dates[2]),
                        eq(dates[1]),
                        any(),
                        any()
                )
        ).thenReturn(ValueItem(todayViews.toFormattedString(), 0, true, change, NEUTRAL))

        viewModel.start(siteId, color.ordinal, showChangeColumn)

        viewModel.onDataSetChanged()

        viewModel.data.let { data ->
            assertThat(data).hasSize(dates.size)
            assertThat(data[0].layout).isEqualTo(R.layout.stats_views_widget_item_light)
            assertThat(data[0].key).isEqualTo(todayString)
            assertThat(data[0].isNeutralChangeVisible).isTrue()
            assertThat(data[0].isPositiveChangeVisible).isFalse()
            assertThat(data[0].isNegativeChangeVisible).isFalse()
            assertThat(data[0].change).isEqualTo(change)
            assertThat(data[0].showDivider).isFalse()
            assertThat(data[1].key).isEqualTo("Jan 7, 2019")
            assertThat(data[1].isNeutralChangeVisible).isFalse()
            assertThat(data[1].isPositiveChangeVisible).isFalse()
            assertThat(data[1].isNegativeChangeVisible).isTrue()
            assertThat(data[1].change).isEqualTo(change)
            assertThat(data[1].showDivider).isFalse()
            assertThat(data[2].key).isEqualTo("Jan 6, 2019")
            assertThat(data[2].isNeutralChangeVisible).isFalse()
            assertThat(data[2].isPositiveChangeVisible).isTrue()
            assertThat(data[2].isNegativeChangeVisible).isFalse()
            assertThat(data[2].change).isEqualTo(change)
            assertThat(data[2].showDivider).isFalse()
        }
    }
}
