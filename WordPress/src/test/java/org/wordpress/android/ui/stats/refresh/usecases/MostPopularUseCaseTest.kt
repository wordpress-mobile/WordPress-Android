package org.wordpress.android.ui.stats.refresh.usecases

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
import org.wordpress.android.fluxc.store.InsightsStore.OnInsightsFetched
import org.wordpress.android.fluxc.store.InsightsStore.StatsError
import org.wordpress.android.fluxc.store.InsightsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.BlockListItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.BlockListItem.Item
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.LIST_ITEM
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.DateUtils
import org.wordpress.android.ui.stats.refresh.Failed
import org.wordpress.android.ui.stats.refresh.InsightsItem
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.FAILED
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.LIST_INSIGHTS
import org.wordpress.android.ui.stats.refresh.ListInsightItem
import kotlin.math.roundToInt

class MostPopularUseCaseTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: InsightsStore
    @Mock lateinit var site: SiteModel
    @Mock lateinit var dateUtils: DateUtils
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
                dateUtils
        )
        whenever(dateUtils.getWeekDay(day)).thenReturn(dayString)
        whenever(dateUtils.getWeekDay(hour)).thenReturn(hourString)
    }

    @Test
    fun `maps full most popular insights to UI model`() = test {
        val forced = false
        val refresh = true
        whenever(insightsStore.fetchMostPopularInsights(site, forced)).thenReturn(
                OnInsightsFetched(
                        InsightsMostPopularModel(0, day, hour, highestDayPercent, highestHourPercent)
                )
        )

        val result = loadMostPopularInsights(refresh, forced)

        assertThat(result.type).isEqualTo(LIST_INSIGHTS)
        (result as ListInsightItem).apply {
            assertThat(this.items).hasSize(3)
            assertTitle(this.items[0])
            assertDay(this.items[1])
            assertHour(this.items[2])
        }
    }

    @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val refresh = true
        val message = "Generic error"
        whenever(insightsStore.fetchMostPopularInsights(site, forced)).thenReturn(
                OnInsightsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )

        val result = loadMostPopularInsights(refresh, forced)

        assertThat(result.type).isEqualTo(FAILED)
        (result as Failed).apply {
            assertThat(this.failedType).isEqualTo(R.string.stats_insights_popular)
            assertThat(this.errorMessage).isEqualTo(message)
        }
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE)
        assertThat((item as Title).text).isEqualTo(R.string.stats_insights_popular)
    }

    private fun assertDay(blockListItem: BlockListItem) {
        assertThat(blockListItem.type).isEqualTo(LIST_ITEM)
        val item = blockListItem as Item
        assertThat(item.text).isEqualTo(dayString)
        assertThat(item.showDivider).isEqualTo(true)
        assertThat(item.value).isEqualTo("${highestDayPercent.roundToInt()}%")
    }

    private fun assertHour(blockListItem: BlockListItem) {
        assertThat(blockListItem.type).isEqualTo(LIST_ITEM)
        val item = blockListItem as Item
        assertThat(item.text).isEqualTo(hourString)
        assertThat(item.showDivider).isEqualTo(false)
        assertThat(item.value).isEqualTo("${highestHourPercent.roundToInt()}%")
    }

    private suspend fun loadMostPopularInsights(refresh: Boolean, forced: Boolean): InsightsItem {
        var result: InsightsItem? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(site, refresh, forced)
        return checkNotNull(result)
    }
}
