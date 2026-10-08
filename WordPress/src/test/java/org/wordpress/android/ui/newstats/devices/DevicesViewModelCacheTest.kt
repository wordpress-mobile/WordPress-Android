package org.wordpress.android.ui.newstats.devices

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.DeviceItemData
import org.wordpress.android.ui.newstats.repository.DevicesResult
import org.wordpress.android.ui.newstats.repository.StatsCacheBucket
import org.wordpress.android.ui.newstats.repository.StatsRepository

/**
 * What a background revalidation may and may not write once the user has moved on (CMM-2473).
 *
 * The card hand-rolls the two-phase cached load rather than inheriting it from
 * `BaseStatsCardViewModel`, so it needs its own cancellation coverage: a revalidation outlives the
 * load that started it, and a cache hit renders without suspending, so an uncancelled one would
 * reliably land *after* the period the user actually selected.
 *
 * Runs on a [StandardTestDispatcher] so a fetch can be held in flight across a period change.
 */
@ExperimentalCoroutinesApi
class DevicesViewModelCacheTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    private lateinit var viewModel: DevicesViewModel

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

    private fun cachePeriod(period: StatsPeriod, needsRevalidation: Boolean) {
        whenever(statsRepository.isCached(StatsCacheBucket.DEVICES_SCREENSIZE, TEST_SITE_ID, period))
            .thenReturn(true)
        whenever(
            statsRepository.needsRevalidation(StatsCacheBucket.DEVICES_SCREENSIZE, TEST_SITE_ID, period)
        ).thenReturn(needsRevalidation)
    }

    @Test
    fun `given a revalidation is in flight, when the period changes, then its result is not applied`() = test {
        // Both periods were viewed in an earlier visit, so both are cached and stale.
        cachePeriod(FIRST_PERIOD, needsRevalidation = true)
        cachePeriod(SECOND_PERIOD, needsRevalidation = false)
        val revalidationGate = CompletableDeferred<Unit>()
        whenever(statsRepository.fetchDevicesScreensize(any(), eq(FIRST_PERIOD), eq(false)))
            .thenReturn(result(FIRST_PERIOD_ITEM))
        whenever(statsRepository.fetchDevicesScreensize(any(), eq(FIRST_PERIOD), eq(true)))
            .doSuspendableAnswer {
                revalidationGate.await()
                result(FIRST_PERIOD_ITEM)
            }
        whenever(statsRepository.fetchDevicesScreensize(any(), eq(SECOND_PERIOD), eq(false)))
            .thenReturn(result(SECOND_PERIOD_ITEM))

        viewModel = DevicesViewModel(selectedSiteRepository, accountStore, statsRepository)
        viewModel.onPeriodChanged(FIRST_PERIOD)
        advanceUntilIdle()

        // The user moves on while the first period's revalidation is still waiting on the network.
        viewModel.onPeriodChanged(SECOND_PERIOD)
        advanceUntilIdle()
        revalidationGate.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value as DevicesCardUiState.Loaded
        assertThat(state.items.single().name).isEqualTo(SECOND_PERIOD_ITEM)
    }

    @Test
    fun `given the period changed, when a type the user is not viewing is selected, then it is refetched`() = test {
        // The period change must invalidate every type, not just the visible one.
        whenever(statsRepository.fetchDevicesScreensize(any(), any(), any()))
            .thenReturn(result(FIRST_PERIOD_ITEM))
        whenever(statsRepository.fetchDevicesBrowser(any(), any(), any()))
            .thenReturn(result(FIRST_PERIOD_ITEM))

        viewModel = DevicesViewModel(selectedSiteRepository, accountStore, statsRepository)
        viewModel.onPeriodChanged(FIRST_PERIOD)
        advanceUntilIdle()
        viewModel.onDeviceTypeChanged(DeviceType.BROWSER)
        advanceUntilIdle()
        viewModel.onPeriodChanged(SECOND_PERIOD)
        advanceUntilIdle()

        verify(statsRepository).fetchDevicesBrowser(TEST_SITE_ID, SECOND_PERIOD, false)
    }

    @Test
    fun `given a cached period was already revalidated, when it is selected, then it is not refetched`() = test {
        cachePeriod(FIRST_PERIOD, needsRevalidation = false)
        whenever(statsRepository.fetchDevicesScreensize(any(), any(), any()))
            .thenReturn(result(FIRST_PERIOD_ITEM))

        viewModel = DevicesViewModel(selectedSiteRepository, accountStore, statsRepository)
        viewModel.onPeriodChanged(FIRST_PERIOD)
        advanceUntilIdle()

        verify(statsRepository, never()).fetchDevicesScreensize(any(), any(), eq(true))
    }

    private fun result(name: String) = DevicesResult.Success(
        items = listOf(DeviceItemData(name = name, views = 10.0))
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_access_token"
        private const val FIRST_PERIOD_ITEM = "iPhone"
        private const val SECOND_PERIOD_ITEM = "Android"
        private val FIRST_PERIOD = StatsPeriod.Last7Days
        private val SECOND_PERIOD = StatsPeriod.Last30Days
    }
}
