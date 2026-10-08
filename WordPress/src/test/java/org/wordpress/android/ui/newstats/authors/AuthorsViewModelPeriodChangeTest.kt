package org.wordpress.android.ui.newstats.authors

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
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.TopAuthorsResult

/**
 * Covers the guard that keeps the card from requesting a period it is already loading.
 *
 * Needs a [StandardTestDispatcher] rather than the unconfined default: cancelling a coroutine that
 * is suspended in a request only unwinds it when its dispatcher next runs, which is what lets a
 * cancelled job's `finally` run after the load that replaced it has already set the guard.
 */
@ExperimentalCoroutinesApi
class AuthorsViewModelPeriodChangeTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    private lateinit var viewModel: AuthorsViewModel

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
    fun `given a period is loading, when it is requested again, then the request is not repeated`() = test {
        whenever(statsRepository.fetchTopAuthors(eq(TEST_SITE_ID), any(), any())).doSuspendableAnswer {
            delay(REQUEST_MS)
            TopAuthorsResult.Success(
                authors = emptyList(),
                totalViews = 0L,
                totalViewsChange = 0L,
                totalViewsChangePercent = 0.0
            )
        }

        viewModel = AuthorsViewModel(selectedSiteRepository, accountStore, statsRepository)
        viewModel.onPeriodChanged(StatsPeriod.Last7Days)
        // Starts the first request and leaves it waiting on its response.
        runCurrent()

        viewModel.onPeriodChanged(StatsPeriod.Last30Days)
        // Unwinds the cancelled Last7Days load. Its `finally` must leave the guard the Last30Days
        // load set in place.
        runCurrent()
        // Adding or reordering a card re-dispatches the current period to every visible card.
        viewModel.onPeriodChanged(StatsPeriod.Last30Days)
        advanceUntilIdle()

        verify(statsRepository, times(1))
            .fetchTopAuthors(eq(TEST_SITE_ID), eq(StatsPeriod.Last30Days), any())
    }

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_token"
        private const val REQUEST_MS = 1_000L
    }
}
