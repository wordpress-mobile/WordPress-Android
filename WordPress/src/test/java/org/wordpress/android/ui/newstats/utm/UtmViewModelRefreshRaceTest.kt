package org.wordpress.android.ui.newstats.utm

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.analytics.NewStatsTracker
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.UtmItemData
import org.wordpress.android.ui.newstats.repository.UtmResult
import org.wordpress.android.ui.prefs.AppPrefsWrapper

/**
 * Covers a pull-to-refresh that is still in flight when the user switches period.
 *
 * Needs a [StandardTestDispatcher] rather than the unconfined default: the refresh has to be left
 * waiting on its response while the next period renders, which is the ordering that used to let it
 * land its rows on top of the period the user had moved to.
 */
@ExperimentalCoroutinesApi
class UtmViewModelRefreshRaceTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    private lateinit var newStatsTracker: NewStatsTracker

    private lateinit var viewModel: UtmViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(accountStore.accessToken).thenReturn(TEST_ACCESS_TOKEN)
        whenever(appPrefsWrapper.getStatsUtmCategory(TEST_SITE_ID)).thenReturn(null)
    }

    @Test
    fun `given a refresh is in flight, when the period changes, then the new period stays on screen`() = test {
        givenSlowSevenDays()
        viewModel = UtmViewModel(selectedSiteRepository, accountStore, statsRepository, appPrefsWrapper, newStatsTracker)

        // Pull-to-refresh on Last7Days, still waiting on its response...
        viewModel.refresh()
        runCurrent()
        // ...while the user switches to a Last30Days that is already in memory.
        viewModel.onPeriodChanged(StatsPeriod.Last30Days)
        advanceUntilIdle()

        assertThat(loadedItemTitle()).isEqualTo(StatsPeriod.Last30Days.toString())
    }

    @Test
    fun `given a refresh was left behind, when the period is re-dispatched, then the card is not stuck`() = test {
        givenSlowSevenDays()
        viewModel = UtmViewModel(selectedSiteRepository, accountStore, statsRepository, appPrefsWrapper, newStatsTracker)

        viewModel.refresh()
        runCurrent()
        viewModel.onPeriodChanged(StatsPeriod.Last30Days)
        advanceUntilIdle()

        // Adding or reordering a card re-dispatches the selected period to every visible card. A
        // refresh that recorded its own rows under Last30Days would short-circuit this for good.
        viewModel.onPeriodChanged(StatsPeriod.Last30Days)
        advanceUntilIdle()

        assertThat(loadedItemTitle()).isEqualTo(StatsPeriod.Last30Days.toString())
    }

    @Test
    fun `given a refresh is in flight, when the period changes, then the spinner stops`() = test {
        givenSlowSevenDays()
        viewModel = UtmViewModel(selectedSiteRepository, accountStore, statsRepository, appPrefsWrapper, newStatsTracker)

        viewModel.refresh()
        runCurrent()
        viewModel.onPeriodChanged(StatsPeriod.Last30Days)
        advanceUntilIdle()

        // The period change cancels the refresh, so nothing else is left to clear its spinner.
        assertThat(viewModel.isRefreshing.value).isFalse()
    }

    /** Every period answers from memory except the one the refresh is sent for. */
    private suspend fun givenSlowSevenDays() {
        whenever(
            statsRepository.fetchUtm(eq(TEST_SITE_ID), any(), any(), any())
        ).doSuspendableAnswer { invocation ->
            val period = invocation.arguments[2] as StatsPeriod
            if (period == StatsPeriod.Last7Days) delay(REQUEST_MS)
            successFor(period)
        }
    }

    private fun loadedItemTitle() =
        (viewModel.uiState.value as UtmCardUiState.Loaded).items.first().title

    private fun successFor(period: StatsPeriod) = UtmResult.Success(
        items = listOf(UtmItemData(name = period.toString(), views = 5L, topPosts = emptyList())),
        totalViews = 5L
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_token"
        private const val REQUEST_MS = 1_000L
    }
}
