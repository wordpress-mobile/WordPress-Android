package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_TOTAL_COMMENTS_ERROR
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.LimitMode.Top
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TitleWithMore
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE_WITH_MORE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueWithChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater.StatsWidgetUpdaters
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Calendar

@ExperimentalCoroutinesApi
class TotalCommentsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var store: VisitsAndViewsStore
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    @Mock lateinit var totalStatsMapper: TotalStatsMapper
    @Mock lateinit var site: SiteModel
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var statsWidgetUpdaters: StatsWidgetUpdaters
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock lateinit var valueWithChart: ValueWithChartItem
    @Mock lateinit var information: Text
    @Mock lateinit var useCaseMode: UseCaseMode
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    private lateinit var useCase: TotalCommentsUseCase
    private val periodData = PeriodData("2018-10-08", 10, 15, 20, 25, 30, 35)
    private val modelPeriod = "2018-10-10"
    private val limitMode = Top(15)
    private val model = VisitsAndViewsModel(modelPeriod, listOf(periodData))

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        useCase = TotalCommentsUseCase(
                Dispatchers.Unconfined,
                TEST_DISPATCHER,
                store,
                statsSiteProvider,
                resourceProvider,
                statsDateFormatter,
                totalStatsMapper,
                analyticsTrackerWrapper,
                statsWidgetUpdaters,
                localeManagerWrapper,
                useCaseMode
        )
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever(totalStatsMapper.buildTotalCommentsValue(any())).thenReturn(valueWithChart)
        whenever(totalStatsMapper.buildTotalCommentsInformation(any())).thenReturn(information)
    }

    @Test
    fun `maps domain model to UI model`() = test {
        val forced = false
        setupCalendar()
        whenever(store.getVisits(site, DAYS, LimitMode.All)).thenReturn(model)
        whenever(store.fetchVisits(site, DAYS, limitMode, forced)).thenReturn(OnStatsFetched(model))

        val result = loadData(true, forced)

        assertThat(result.type).isEqualTo(InsightType.TOTAL_COMMENTS)

        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(3)
            assertTitle(this[0])
            assertThat(this[1]).isEqualTo(valueWithChart)
            assertThat(this[2]).isEqualTo(information)
        }
    }

    @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val message = "Generic error"
        whenever(store.fetchVisits(site, DAYS, limitMode, forced)).thenReturn(
                OnStatsFetched(StatsError(GENERIC_ERROR, message))
        )

        val result = loadData(true, forced)

        assertThat(result.state).isEqualTo(UseCaseState.ERROR)
    }

    @Test
    fun `does not track incorrect data when the last stats item is less than 2 days old`() = test {
        val forced = false
        setupCalendar(1)
        whenever(store.fetchVisits(site, DAYS, limitMode, forced)).thenReturn(OnStatsFetched(model))

        loadData(true, forced)

        verify(analyticsTrackerWrapper, never()).track(eq(STATS_TOTAL_COMMENTS_ERROR), any<Map<String, *>>())
    }

    @Test
    fun `tracks incorrect data when the last stats item is at least 2 days old`() = test {
        val forced = false
        setupCalendar(2)
        whenever(store.fetchVisits(site, DAYS, limitMode, forced)).thenReturn(OnStatsFetched(model))

        loadData(true, forced)

        verify(analyticsTrackerWrapper).track(
                STATS_TOTAL_COMMENTS_ERROR,
                mapOf(
                        "stats_last_date" to "2020-12-13",
                        "stats_current_date" to "2020-12-15",
                        "stats_age_in_days" to 2,
                        "is_jetpack_connected" to false,
                        "is_atomic" to false,
                        "action_source" to "remote"
                )
        )
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE_WITH_MORE)
        assertThat((item as TitleWithMore).textResource).isEqualTo(R.string.stats_view_total_comments)
    }

    private suspend fun loadData(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        return checkNotNull(result)
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
            Calendar.getInstance().apply { this.time = today.time }
        }
        whenever(statsDateFormatter.parseStatsDate(any(), any())).thenReturn(lastItemAge.time)
        whenever(statsDateFormatter.printStatsDate(lastItemAge.time)).thenReturn("2020-12-$lastItemDay")
        whenever(statsDateFormatter.printStatsDate(today.time)).thenReturn("2020-12-$todayDay")
    }
}
