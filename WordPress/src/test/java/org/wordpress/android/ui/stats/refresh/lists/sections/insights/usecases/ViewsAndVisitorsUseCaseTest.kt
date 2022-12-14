package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_VIEWS_AND_VISITORS_ERROR
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode.Top
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.VIEWS_AND_VISITORS
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.SUCCESS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Chips
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.LineChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValuesItem
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater.StatsWidgetUpdaters
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Calendar

@ExperimentalCoroutinesApi
class ViewsAndVisitorsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var store: VisitsAndViewsStore
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    @Mock lateinit var viewsAndVisitorsMapper: ViewsAndVisitorsMapper
    @Mock lateinit var popupMenuHandler: ItemPopupMenuHandler
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var title: ValuesItem
    @Mock lateinit var chips: Chips
    @Mock lateinit var lineChartItem: LineChartItem
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock lateinit var statsWidgetUpdaters: StatsWidgetUpdaters
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock lateinit var useCaseMode: UseCaseMode
    private lateinit var useCase: ViewsAndVisitorsUseCase
    private val site = SiteModel()
    private val siteId = 1L
    private val periodData = PeriodData("2018-10-08", 10, 15, 20, 25, 30, 35)
    private val modelPeriod = "2018-10-10"
    private val limitMode = Top(15)
    private val statsGranularity = DAYS
    private val model = VisitsAndViewsModel(modelPeriod, listOf(periodData))

    @Before
    fun setUp() {
        useCase = ViewsAndVisitorsUseCase(
                VIEWS_AND_VISITORS,
                statsGranularity,
                store,
                selectedDateProvider,
                statsSiteProvider,
                statsDateFormatter,
                viewsAndVisitorsMapper,
                Dispatchers.Unconfined,
                testDispatcher(),
                analyticsTrackerWrapper,
                statsWidgetUpdaters,
                localeManagerWrapper,
                resourceProvider,
                useCaseMode
        )
        site.siteId = siteId
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever(viewsAndVisitorsMapper.buildTitle(any(), any(), any(), any(), any())).thenReturn(title)
        whenever(viewsAndVisitorsMapper
                .buildChart(any(), any(), any(), any(), any(), any()))
                .thenReturn(listOf(lineChartItem))
        whenever(viewsAndVisitorsMapper.buildInformation(any(), any(), any())).thenReturn(Text(text = ""))
        whenever(viewsAndVisitorsMapper.buildChips(any(), any())).thenReturn(chips)
        whenever(resourceProvider.getString(string.stats_loading_card)).thenReturn("Loading")
    }

    @Test
    fun `maps domain model to UI model`() = test {
        val forced = false
        setupCalendar()
        whenever(store.getVisits(site, statsGranularity, limitMode)).thenReturn(model)
        whenever(store.fetchVisits(site, statsGranularity, limitMode, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadData(true, forced)

        Assertions.assertThat(result.type).isEqualTo(VIEWS_AND_VISITORS)
        Assertions.assertThat(result.state).isEqualTo(SUCCESS)
        result.data!!.apply {
            Assertions.assertThat(this[2]).isEqualTo(title)
            Assertions.assertThat(this[3]).isEqualTo(lineChartItem)
            Assertions.assertThat(this[5]).isEqualTo(chips)
        }
        verify(statsWidgetUpdaters, times(2)).updateViewsWidget(siteId)
    }

    @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val message = "Generic error"
        whenever(store.fetchVisits(site, statsGranularity, limitMode, forced)).thenReturn(
                OnStatsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )

        val result = loadData(true, forced)

        Assertions.assertThat(result.state).isEqualTo(ERROR)
    }

    @Test
    fun `does not track incorrect data when the last stats item is less than 2 days old`() = test {
        val forced = false
        setupCalendar(1)
        whenever(store.fetchVisits(site, statsGranularity, limitMode, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        loadData(true, forced)

        verify(analyticsTrackerWrapper, never()).track(eq(STATS_VIEWS_AND_VISITORS_ERROR), any<Map<String, *>>())
    }

    @Test
    fun `tracks incorrect data when the last stats item is at least 2 days old`() = test {
        val forced = false
        setupCalendar(2)
        whenever(store.fetchVisits(site, statsGranularity, limitMode, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        loadData(true, forced)

        verify(analyticsTrackerWrapper).track(
                STATS_VIEWS_AND_VISITORS_ERROR, mapOf(
                "stats_last_date" to "2020-12-13",
                "stats_current_date" to "2020-12-15",
                "stats_age_in_days" to 2,
                "is_jetpack_connected" to false,
                "is_atomic" to false,
                "action_source" to "remote"
        )
        )
    }

    private fun setupCalendar(ageOfLastStatsItemInDays: Int = 0) {
        val today = Calendar.getInstance()
        today.set(Calendar.YEAR, 2020)
        today.set(Calendar.MONTH, 11)
        val todayDay = 15
        today.set(Calendar.DAY_OF_MONTH, todayDay)
        today.set(Calendar.HOUR_OF_DAY, 20)
        val lastItemAge = Calendar.getInstance()
        lastItemAge.time = today.time
        val lastItemDay = todayDay - ageOfLastStatsItemInDays
        lastItemAge.set(Calendar.DAY_OF_MONTH, lastItemDay)
        lastItemAge.set(Calendar.HOUR_OF_DAY, 22)
        whenever(localeManagerWrapper.getCurrentCalendar()).then {
            Calendar.getInstance()
                    .apply { this.time = today.time }
        }
        whenever(statsDateFormatter.parseStatsDate(any(), any())).thenReturn(lastItemAge.time)
        whenever(statsDateFormatter.printStatsDate(lastItemAge.time)).thenReturn("2020-12-$lastItemDay")
        whenever(statsDateFormatter.printStatsDate(today.time)).thenReturn("2020-12-$todayDay")
    }

    private suspend fun loadData(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        return checkNotNull(result)
    }
}
