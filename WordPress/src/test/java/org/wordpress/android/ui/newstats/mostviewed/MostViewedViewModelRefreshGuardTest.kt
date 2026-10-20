package org.wordpress.android.ui.newstats.mostviewed

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.MostViewedResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.viewmodel.ResourceProvider

/**
 * Covers the guard that keeps a period from being requested twice when a pull-to-refresh replaces
 * the load that was holding it.
 *
 * Needs a [StandardTestDispatcher] rather than the unconfined default: cancelling a coroutine that
 * is suspended in a request only unwinds it when its dispatcher next runs, which is what makes the
 * cancelled load skip the clear and leaves the refresh owning the guard.
 */
@ExperimentalCoroutinesApi
class MostViewedViewModelRefreshGuardTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: MostViewedViewModel

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

    @Test
    fun `given a refresh replaced a failing load, when the period is re-dispatched, then it is requested`() = test {
        var calls = 0
        whenever(
            statsRepository.fetchMostViewed(eq(TEST_SITE_ID), any(), eq(POSTS), any())
        ).doSuspendableAnswer {
            calls++
            when (calls) {
                // The load the pull-to-refresh below cancels.
                1 -> {
                    delay(REQUEST_MS)
                    success()
                }
                // The refresh itself, which fails.
                2 -> MostViewedResult.Error("Network error")
                else -> success()
            }
        }

        viewModel = MostViewedViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider
        )
        viewModel.onPeriodChangedPosts(StatsPeriod.Last7Days)
        // Starts the first request and leaves it waiting on its response.
        runCurrent()
        viewModel.refreshPosts()
        advanceUntilIdle()

        // The refresh owns the guard the load it replaced was holding, so it has to release it:
        // otherwise the card is stuck on the error until the user taps Retry, and the re-dispatch
        // that follows adding or reordering a card can't load the period again.
        viewModel.onPeriodChangedPosts(StatsPeriod.Last7Days)
        advanceUntilIdle()

        verify(statsRepository, times(3))
            .fetchMostViewed(eq(TEST_SITE_ID), eq(StatsPeriod.Last7Days), eq(POSTS), any())
    }

    private fun success() = MostViewedResult.Success(
        items = emptyList(),
        totalViews = 0L,
        totalViewsChange = 0L,
        totalViewsChangePercent = 0.0
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_token"
        private const val REQUEST_MS = 1_000L
        private val POSTS = MostViewedDataSource.POSTS_AND_PAGES
    }
}
