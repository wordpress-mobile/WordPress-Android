package org.wordpress.android.ui.newstats.clicks

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.mostviewed.MostViewedCardUiState
import org.wordpress.android.ui.newstats.repository.ClickItemData
import org.wordpress.android.ui.newstats.repository.ClicksResult
import org.wordpress.android.ui.newstats.repository.StatsCacheBucket
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.viewmodel.ResourceProvider

/**
 * How a card behaves when the repository already holds the period (CMM-2473). Covers the four cards
 * built on `BaseStatsCardViewModel` (clicks, search terms, video plays, file downloads), which share
 * this load path. The cards that hand-roll it have their own coverage — see
 * `DevicesViewModelCacheTest` for the cancellation behaviour a hand-rolled copy has to get right.
 *
 * Runs on a [StandardTestDispatcher] rather than the unconfined default, so the state the card shows
 * *while* a load is in flight can be asserted — that is the whole point of the cached path.
 */
@ExperimentalCoroutinesApi
class ClicksViewModelCacheTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: ClicksViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(accountStore.accessToken).thenReturn(TEST_ACCESS_TOKEN)
    }

    private suspend fun initLoadedViewModel() {
        whenever(statsRepository.fetchClicks(any(), any(), any())).thenReturn(successResult())
        viewModel = ClicksViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider
        )
        viewModel.onPeriodChanged(FIRST_PERIOD)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(MostViewedCardUiState.Loaded::class.java)
    }

    private fun cachePeriod(period: StatsPeriod, needsRevalidation: Boolean = false) {
        whenever(statsRepository.isCached(StatsCacheBucket.CLICKS, TEST_SITE_ID, period))
            .thenReturn(true)
        whenever(statsRepository.needsRevalidation(StatsCacheBucket.CLICKS, TEST_SITE_ID, period))
            .thenReturn(needsRevalidation)
    }

    @Test
    fun `given the period is cached, when it is selected, then no loading state is shown`() = test {
        initLoadedViewModel()
        cachePeriod(SECOND_PERIOD)

        viewModel.onPeriodChanged(SECOND_PERIOD)

        // The fetch hasn't run yet: the previous numbers must still be on screen, because the cached
        // ones replace them in the same frame.
        assertThat(viewModel.uiState.value).isInstanceOf(MostViewedCardUiState.Loaded::class.java)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(MostViewedCardUiState.Loaded::class.java)
    }

    @Test
    fun `given the period is not cached, when it is selected, then the loading state is shown`() = test {
        initLoadedViewModel()

        viewModel.onPeriodChanged(SECOND_PERIOD)

        assertThat(viewModel.uiState.value).isEqualTo(MostViewedCardUiState.Loading)
    }

    @Test
    fun `given a cached period needs revalidation, when it is selected, then it is refetched once`() = test {
        initLoadedViewModel()
        cachePeriod(SECOND_PERIOD, needsRevalidation = true)

        viewModel.onPeriodChanged(SECOND_PERIOD)
        advanceUntilIdle()

        verify(statsRepository).fetchClicks(TEST_SITE_ID, SECOND_PERIOD, false)
        verify(statsRepository).fetchClicks(TEST_SITE_ID, SECOND_PERIOD, true)
    }

    @Test
    fun `given a cached period was already revalidated, when it is selected, then it is fetched once`() = test {
        initLoadedViewModel()
        cachePeriod(SECOND_PERIOD)

        viewModel.onPeriodChanged(SECOND_PERIOD)
        advanceUntilIdle()

        verify(statsRepository, times(1)).fetchClicks(eq(TEST_SITE_ID), eq(SECOND_PERIOD), any())
    }

    @Test
    fun `given revalidation fails, then the cached content stays on screen`() = test {
        initLoadedViewModel()
        cachePeriod(SECOND_PERIOD, needsRevalidation = true)
        whenever(statsRepository.fetchClicks(any(), any(), eq(true)))
            .thenReturn(ClicksResult.Error(R.string.stats_error_api))

        viewModel.onPeriodChanged(SECOND_PERIOD)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(MostViewedCardUiState.Loaded::class.java)
    }

    @Test
    fun `when refresh is called, then the cache is bypassed`() = test {
        initLoadedViewModel()

        viewModel.refresh()
        advanceUntilIdle()

        verify(statsRepository).fetchClicks(TEST_SITE_ID, FIRST_PERIOD, true)
    }

    private fun successResult() = ClicksResult.Success(
        items = listOf(ClickItemData(name = "example.com", clicks = 10, previousClicks = 5)),
        totalClicks = 10,
        totalClicksChange = 5,
        totalClicksChangePercent = 100.0
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_access_token"
        private val FIRST_PERIOD = StatsPeriod.Last7Days
        private val SECOND_PERIOD = StatsPeriod.Last30Days
    }
}
