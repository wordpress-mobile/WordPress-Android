package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMostPopularModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.insights.MostPopularInsightsStore
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.QUICK_SCAN_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.utils.ActionCardHandler
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsDateUtils
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.text.PercentFormatter
import org.wordpress.android.viewmodel.ResourceProvider
import java.math.RoundingMode
import java.math.RoundingMode.HALF_UP
import kotlin.math.roundToInt

class MostPopularInsightsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: MostPopularInsightsStore
    @Mock lateinit var postStore: PostStore
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var site: SiteModel
    @Mock lateinit var mStatsDateUtils: StatsDateUtils
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var popupMenuHandler: ItemPopupMenuHandler
    @Mock lateinit var actionCardHandler: ActionCardHandler
    @Mock private lateinit var percentFormatter: PercentFormatter
    private lateinit var useCase: MostPopularInsightsUseCase
    private val day = 2
    private val highestDayPercent = 15.0
    private val dayString = "Tuesday"
    private val hour = 20
    private val highestHourPercent = 25.5
    private val hourString = "8:00 PM"
    @InternalCoroutinesApi
    @Before
    fun setUp() {
        useCase = MostPopularInsightsUseCase(
                Dispatchers.Unconfined,
                TEST_DISPATCHER,
                insightsStore,
                postStore,
                statsSiteProvider,
                mStatsDateUtils,
                resourceProvider,
                popupMenuHandler,
                actionCardHandler,
                percentFormatter
        )
        whenever(
                percentFormatter.format(
                        value = highestDayPercent.roundToInt(),
                        rounding = RoundingMode.HALF_UP
                )
        ).thenReturn("10%")
        whenever(
                percentFormatter.format(
                        value = highestHourPercent.roundToInt(),
                        rounding = RoundingMode.HALF_UP
                )
        ).thenReturn("20%")
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever(mStatsDateUtils.getWeekDay(day)).thenReturn(dayString)

        whenever(mStatsDateUtils.getHour(hour)).thenReturn(hourString)

        whenever(
                resourceProvider.getString(
                        R.string.stats_most_popular_percent_views, "10%"
                )
        ).thenReturn("${highestDayPercent.roundToInt()}% of views")

        whenever(
                resourceProvider.getString(
                        R.string.stats_most_popular_percent_views, "20%"
                )
        ).thenReturn("${highestHourPercent.roundToInt()}% of views")
    }

    @Test
    fun `maps full most popular insights to UI model`() = test {
        val forced = false
        val refresh = true
        val model = InsightsMostPopularModel(0, day, hour, highestDayPercent, highestHourPercent)
        whenever(insightsStore.getMostPopularInsights(site)).thenReturn(model)
        whenever(insightsStore.fetchMostPopularInsights(site, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadMostPopularInsights(refresh, forced)

        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(2)
            assertTitle(this[0])
            assertDayAndHour(this[1])
        }
    }

    @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val refresh = true
        val message = "Generic error"
        whenever(insightsStore.fetchMostPopularInsights(site, forced)).thenReturn(
                OnStatsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )

        val result = loadMostPopularInsights(refresh, forced)

        assertThat(result.state).isEqualTo(UseCaseState.ERROR)
    }

    @Test
    fun `when buildUiModel is called, should call PercentFormatter`() = test {
        useCase.buildUiModel(InsightsMostPopularModel(0, day, hour, highestDayPercent, highestHourPercent))
        verify(percentFormatter).format(
                value = highestDayPercent.roundToInt(),
                rounding = HALF_UP
        )
        verify(percentFormatter).format(
                value = highestHourPercent.roundToInt(),
                rounding = HALF_UP
        )
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE)
        assertThat((item as Title).textResource).isEqualTo(R.string.stats_insights_popular)
    }

    private fun assertDayAndHour(blockListItem: BlockListItem) {
        assertThat(blockListItem.type).isEqualTo(QUICK_SCAN_ITEM)
        val item = blockListItem as QuickScanItem
        assertThat(item.startColumn.label).isEqualTo(R.string.stats_insights_best_day)
        assertThat(item.startColumn.value).isEqualTo(dayString)
        assertThat(item.startColumn.tooltip).isEqualTo("${highestDayPercent.roundToInt()}% of views")
        assertThat(item.endColumn.label).isEqualTo(R.string.stats_insights_best_hour)
        assertThat(item.endColumn.value).isEqualTo(hourString)
        assertThat(item.endColumn.tooltip).isEqualTo("${highestHourPercent.roundToInt()}% of views")
    }

    private suspend fun loadMostPopularInsights(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        return checkNotNull(result)
    }
}
