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
 * Covers what happens to a load that is still in flight when the period changes again.
 *
 * Needs a [StandardTestDispatcher] rather than the unconfined default: cancelling a coroutine that
 * is suspended in a request only unwinds it when its dispatcher next runs, which is exactly the
 * ordering that lets a cancelled job's `finally` run after its replacement has been registered.
 */
@ExperimentalCoroutinesApi
class UtmViewModelPeriodChangeTest : BaseUnitTest(StandardTestDispatcher()) {
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
    fun `given a slow period is left twice, when it lands, then the period on screen is kept`() = test {
        // The first two periods are slow and the last one is already in memory, so without
        // cancellation a period the user left lands after the one they are looking at has rendered.
        whenever(
            statsRepository.fetchUtm(eq(TEST_SITE_ID), any(), any(), any())
        ).doSuspendableAnswer { invocation ->
            val period = invocation.arguments[2] as StatsPeriod
            if (period != StatsPeriod.Today) delay(REQUEST_MS)
            successFor(period)
        }

        viewModel = UtmViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            appPrefsWrapper,
            newStatsTracker
        )
        viewModel.onPeriodChanged(StatsPeriod.Last7Days)
        // Starts the first request and leaves it waiting on its response.
        runCurrent()

        viewModel.onPeriodChanged(StatsPeriod.Last30Days)
        // Unwinds the cancelled Last7Days load and starts the Last30Days one. The unwinding must not
        // deregister the job that has just replaced it, or nothing can cancel it from here on.
        runCurrent()
        viewModel.onPeriodChanged(StatsPeriod.Today)
        advanceUntilIdle()

        val state = viewModel.uiState.value as UtmCardUiState.Loaded
        assertThat(state.items.first().title).isEqualTo(StatsPeriod.Today.toString())
    }

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
