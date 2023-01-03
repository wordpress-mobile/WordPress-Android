package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.YearsInsightsModel
import org.wordpress.android.fluxc.model.stats.YearsInsightsModel.YearInsights
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.insights.MostPopularInsightsStore
import org.wordpress.android.ui.stats.refresh.NavigationTarget
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.ANNUAL_STATS
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.SUCCESS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.Event
import java.util.Calendar
import java.util.Locale

@ExperimentalCoroutinesApi
class AnnualSiteStatsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: MostPopularInsightsStore
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock lateinit var annualStatsMapper: AnnualStatsMapper
    @Mock lateinit var site: SiteModel
    @Mock lateinit var popupMenuHandler: ItemPopupMenuHandler
    private val year2019 = YearInsights(null, null, null, null, 0, 0, 0, 0, 0, "2019")
    private val year2018 = year2019.copy(year = "2018")
    private lateinit var useCase: AnnualSiteStatsUseCase
    @Before
    fun setUp() {
        useCase = AnnualSiteStatsUseCase(
                testDispatcher(),
                testDispatcher(),
                insightsStore,
                statsSiteProvider,
                selectedDateProvider,
                annualStatsMapper,
                localeManagerWrapper,
                popupMenuHandler,
                BLOCK
        )
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.US)
    }

    @Test
    fun `maps full most popular insights to UI model`() = test {
        val forced = false
        val refresh = true
        val model = YearsInsightsModel(listOf(year2019))
        whenever(insightsStore.getYearsInsights(site)).thenReturn(model)
        whenever(insightsStore.fetchYearsInsights(site, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )
        whenever(annualStatsMapper.mapYearInBlock(year2019)).thenReturn(listOf(Empty()))

        val result = loadMostPopularInsights(refresh, forced)

        assertThat(result.state).isEqualTo(SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(2)
            assertTitle(this[0])
            assertThat(this[1] is Empty).isTrue()
        }
        val selectedDate = Calendar.getInstance()
        selectedDate.timeInMillis = 0
        selectedDate.set(Calendar.YEAR, 2019)
        selectedDate.set(Calendar.MONTH, Calendar.DECEMBER)
        selectedDate.set(Calendar.DAY_OF_MONTH, 31)
        verify(selectedDateProvider, times(1)).selectDate(selectedDate.time, listOf(selectedDate.time), ANNUAL_STATS)
    }

    @Test
    fun `show the view more button when more years are available`() = test {
        val forced = false
        val refresh = true
        val model = YearsInsightsModel(listOf(year2018, year2019))
        whenever(insightsStore.getYearsInsights(site)).thenReturn(model)
        whenever(insightsStore.fetchYearsInsights(site, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )
        whenever(annualStatsMapper.mapYearInBlock(year2019)).thenReturn(listOf(Empty()))

        val result = loadMostPopularInsights(refresh, forced)

        assertThat(result.state).isEqualTo(SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(3)
            assertTitle(this[0])
            assertThat(this[1] is Empty).isTrue()
            assertThat(this[2] is Link).isTrue()
            var navigationEvent: Event<NavigationTarget>? = null
            useCase.navigationTarget.observeForever { navigationEvent = it }
            (this[2] as Link).navigateAction.click()
            assertThat(navigationEvent).isNotNull
            val navigationTarget = navigationEvent?.getContentIfNotHandled()
            assertThat(navigationTarget is NavigationTarget.ViewAnnualStats).isTrue()
        }
    }

    @Test
    fun `hide title and view more block in view all mode`() = test {
        useCase = AnnualSiteStatsUseCase(
                testDispatcher(),
                testDispatcher(),
                insightsStore,
                statsSiteProvider,
                selectedDateProvider,
                annualStatsMapper,
                localeManagerWrapper,
                popupMenuHandler,
                VIEW_ALL
        )
        val forced = false
        val refresh = true
        val model = YearsInsightsModel(listOf(year2018, year2019))
        whenever(insightsStore.getYearsInsights(site)).thenReturn(model)
        whenever(insightsStore.fetchYearsInsights(site, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )
        whenever(annualStatsMapper.mapYearInViewAll(year2019)).thenReturn(listOf(Empty()))

        val result = loadMostPopularInsights(refresh, forced)

        assertThat(result.state).isEqualTo(SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(1)
            assertThat(this[0] is Empty).isTrue()
        }
    }

    @Test
    fun `maps empty result to UI model`() = test {
        val forced = false
        val refresh = true
        val model = YearsInsightsModel(
                listOf()
        )
        whenever(insightsStore.getYearsInsights(site)).thenReturn(model)
        whenever(insightsStore.fetchYearsInsights(site, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadMostPopularInsights(refresh, forced)

        assertThat(result.state).isEqualTo(UseCaseState.EMPTY)
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

        assertThat(result.state).isEqualTo(ERROR)
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE)
        assertThat((item as Title).textResource).isEqualTo(R.string.stats_insights_this_year_site_stats)
    }

    private suspend fun loadMostPopularInsights(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        advanceUntilIdle()
        return checkNotNull(result)
    }
}
