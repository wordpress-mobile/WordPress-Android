package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMostPopularModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.BlockList
import org.wordpress.android.ui.stats.refresh.lists.Error
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.BLOCK_LIST
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.HEADER
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.utils.DateUtils
import org.wordpress.android.viewmodel.ResourceProvider
import kotlin.math.roundToInt

class MostPopularInsightsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: InsightsStore
    @Mock lateinit var site: SiteModel
    @Mock lateinit var dateUtils: DateUtils
    @Mock lateinit var resourceProvider: ResourceProvider
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
                dateUtils,
                resourceProvider
        )
        whenever(dateUtils.getWeekDay(day)).thenReturn(dayString)

        whenever(dateUtils.getHour(hour)).thenReturn(hourString)

        whenever(resourceProvider.getString(
                R.string.stats_most_popular_percent_views,
                highestDayPercent.roundToInt()
        )).thenReturn("${highestDayPercent.roundToInt()}%")

        whenever(resourceProvider.getString(
                R.string.stats_most_popular_percent_views,
                highestHourPercent.roundToInt()
        )).thenReturn("${highestHourPercent.roundToInt()}%")
    }

    @Test
    fun `maps full most popular insights to UI model`() = test {
        val forced = false
        val refresh = true
        whenever(insightsStore.fetchMostPopularInsights(site, forced)).thenReturn(
                OnStatsFetched(
                        InsightsMostPopularModel(0, day, hour, highestDayPercent, highestHourPercent)
                )
        )

        val result = loadMostPopularInsights(refresh, forced)

        assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).apply {
            assertThat(this.items).hasSize(4)
            assertTitle(this.items[0])
            assertLabel(this.items[1])
            assertDay(this.items[2])
            assertHour(this.items[3])
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

        assertThat(result.type).isEqualTo(ERROR)
        (result as Error).apply {
            assertThat(this.errorMessage).isEqualTo(message)
        }
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE)
        assertThat((item as Title).text).isEqualTo(R.string.stats_insights_popular)
    }

    private fun assertDay(blockListItem: BlockListItem) {
        assertThat(blockListItem.type).isEqualTo(LIST_ITEM)
        val item = blockListItem as ListItem
        assertThat(item.text).isEqualTo(dayString)
        assertThat(item.showDivider).isEqualTo(true)
        assertThat(item.value).isEqualTo("${highestDayPercent.roundToInt()}%")
    }

    private fun assertLabel(blockListItem: BlockListItem) {
        assertThat(blockListItem.type).isEqualTo(HEADER)
        val item = blockListItem as Header
        assertThat(item.leftLabel).isEqualTo(R.string.stats_insights_most_popular_day_and_hour_label)
        assertThat(item.rightLabel).isEqualTo(R.string.stats_insights_most_popular_views_label)
    }

    private fun assertHour(blockListItem: BlockListItem) {
        assertThat(blockListItem.type).isEqualTo(LIST_ITEM)
        val item = blockListItem as ListItem
        assertThat(item.text).isEqualTo(hourString)
        assertThat(item.showDivider).isEqualTo(false)
        assertThat(item.value).isEqualTo("${highestHourPercent.roundToInt()}%")
    }

    private suspend fun loadMostPopularInsights(refresh: Boolean, forced: Boolean): StatsBlock {
        var result: StatsBlock? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(site, refresh, forced)
        return checkNotNull(result)
    }
}
