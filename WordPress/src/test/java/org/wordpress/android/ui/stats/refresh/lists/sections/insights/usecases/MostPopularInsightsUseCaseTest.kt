package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMostPopularModel
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
import org.wordpress.android.ui.stats.refresh.utils.DateUtils
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.viewmodel.ResourceProvider
import kotlin.math.roundToInt

class MostPopularInsightsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: MostPopularInsightsStore
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var site: SiteModel
    @Mock lateinit var dateUtils: DateUtils
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var popupMenuHandler: ItemPopupMenuHandler
    private lateinit var useCase: MostPopularInsightsUseCase
    private val day = 2
    private val highestDayPercent = 15.0
    private val dayString = "Tuesday"
    private val hour = 20
    private val highestHourPercent = 25.5
    private val hourString = "8:00 PM"
    @Before
    fun setUp() {
        useCase = MostPopularInsightsUseCase(
                Dispatchers.Unconfined,
                insightsStore,
                statsSiteProvider,
                dateUtils,
                resourceProvider,
                popupMenuHandler
        )
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever(dateUtils.getWeekDay(day)).thenReturn(dayString)

        whenever(dateUtils.getHour(hour)).thenReturn(hourString)

        whenever(
                resourceProvider.getString(
                        R.string.stats_most_popular_percent_views,
                        highestDayPercent.roundToInt()
                )
        ).thenReturn("${highestDayPercent.roundToInt()}% of views")

        whenever(
                resourceProvider.getString(
                        R.string.stats_most_popular_percent_views,
                        highestHourPercent.roundToInt()
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
