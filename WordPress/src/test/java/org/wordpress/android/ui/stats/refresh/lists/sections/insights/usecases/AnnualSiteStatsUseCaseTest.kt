package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.YearsInsightsModel
import org.wordpress.android.fluxc.model.stats.YearsInsightsModel.YearInsights
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.insights.MostPopularInsightsStore
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.SUCCESS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.QUICK_SCAN_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider

class AnnualSiteStatsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: MostPopularInsightsStore
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var site: SiteModel
    private lateinit var useCase: AnnualSiteStatsUseCase
    @Before
    fun setUp() {
        useCase = AnnualSiteStatsUseCase(
                Dispatchers.Unconfined,
                insightsStore,
                statsSiteProvider
        )
        whenever(statsSiteProvider.siteModel).thenReturn(site)
    }

    @Test
    fun `maps full most popular insights to UI model`() = test {
        val forced = false
        val refresh = true
        val model = YearsInsightsModel(
                listOf(
                        YearInsights(
                                2.567,
                                1.5,
                                578.1,
                                53678.8,
                                155,
                                89,
                                746,
                                12,
                                237462847,
                                "2019"
                        )
                )
        )
        whenever(insightsStore.getYearsInsights(site)).thenReturn(model)
        whenever(insightsStore.fetchYearsInsights(site, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadMostPopularInsights(refresh, forced)

        Assertions.assertThat(result.state).isEqualTo(SUCCESS)
        result.data!!.apply {
            Assertions.assertThat(this).hasSize(5)
            assertTitle(this[0])
            assertQuickScanItem(this[1], R.string.stats_insights_year, "2019", R.string.stats_insights_posts, "12")
            assertQuickScanItem(
                    this[2],
                    R.string.stats_insights_total_comments,
                    "155",
                    R.string.stats_insights_average_comments,
                    "2.6"
            )
            assertQuickScanItem(
                    this[3],
                    R.string.stats_insights_total_likes,
                    "746",
                    R.string.stats_insights_average_likes,
                    "578.1"
            )
            assertQuickScanItem(
                    this[4],
                    R.string.stats_insights_total_words,
                    "237M",
                    R.string.stats_insights_average_words,
                    "53k"
            )
        }
    }

    @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val refresh = true
        val message = "Generic error"
        whenever(insightsStore.fetchYearsInsights(site, forced)).thenReturn(
                OnStatsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )

        val result = loadMostPopularInsights(refresh, forced)

        Assertions.assertThat(result.state).isEqualTo(ERROR)
    }

    private fun assertTitle(item: BlockListItem) {
        Assertions.assertThat(item.type).isEqualTo(TITLE)
        Assertions.assertThat((item as Title).textResource).isEqualTo(R.string.stats_insights_this_year_site_stats)
    }

    private fun assertQuickScanItem(
        blockListItem: BlockListItem,
        startLabel: Int,
        startValue: String,
        endLabel: Int,
        endValue: String
    ) {
        Assertions.assertThat(blockListItem.type).isEqualTo(QUICK_SCAN_ITEM)
        val item = blockListItem as QuickScanItem
        Assertions.assertThat(item.startColumn.label).isEqualTo(startLabel)
        Assertions.assertThat(item.startColumn.value).isEqualTo(startValue)
        Assertions.assertThat(item.endColumn.label).isEqualTo(endLabel)
        Assertions.assertThat(item.endColumn.value).isEqualTo(endValue)
    }

    private suspend fun loadMostPopularInsights(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        return checkNotNull(result)
    }
}
