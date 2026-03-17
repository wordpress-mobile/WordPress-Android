package org.wordpress.android.ui.newstats.subscribers.subscribersgraph

import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.SubscribersGraphDataPoint
import org.wordpress.android.ui.newstats.repository.SubscribersGraphResult
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class SubscribersGraphViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository:
        SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var resourceProvider:
        ResourceProvider

    private lateinit var viewModel:
        SubscribersGraphViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    @Before
    fun setUp() {
        whenever(
            selectedSiteRepository.getSelectedSite()
        ).thenReturn(testSite)
        whenever(accountStore.accessToken)
            .thenReturn(TEST_ACCESS_TOKEN)
        whenever(
            resourceProvider.getString(
                R.string.stats_error_api
            )
        ).thenReturn(FAILED_TO_LOAD_ERROR)
        whenever(
            resourceProvider.getString(
                R.string.stats_error_no_site
            )
        ).thenReturn("No site selected")
        whenever(
            resourceProvider.getString(
                R.string.stats_error_not_authenticated
            )
        ).thenReturn("Not authenticated")
        whenever(
            resourceProvider.getString(
                R.string.stats_error_unknown
            )
        ).thenReturn("Unknown error")
    }

    private fun initViewModel() {
        viewModel = SubscribersGraphViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider
        )
        viewModel.loadData()
    }

    @Test
    fun `when no site selected, then error state is emitted`() =
        test {
            whenever(
                selectedSiteRepository.getSelectedSite()
            ).thenReturn(null)

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                SubscribersGraphUiState.Error::class.java
            )
        }

    @Test
    fun `when access token is null, then error state is emitted`() =
        test {
            whenever(accountStore.accessToken)
                .thenReturn(null)

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                SubscribersGraphUiState.Error::class.java
            )
        }

    @Test
    fun `when data loads successfully, then loaded state has data points`() =
        test {
            whenever(
                statsRepository.fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
            ).thenReturn(createSuccessResult())

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                SubscribersGraphUiState.Loaded::class.java
            )
            val loaded =
                state as SubscribersGraphUiState.Loaded
            assertThat(loaded.dataPoints).hasSize(3)
        }

    @Test
    fun `when fetch fails, then error state is emitted`() =
        test {
            whenever(
                statsRepository.fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
            ).thenReturn(
                SubscribersGraphResult.Error(
                    messageResId = R.string.stats_error_api
                )
            )

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                SubscribersGraphUiState.Error::class.java
            )
            assertThat(
                (state as SubscribersGraphUiState.Error)
                    .message
            ).isEqualTo(FAILED_TO_LOAD_ERROR)
        }

    @Test
    fun `when exception is thrown, then error state has message`() =
        test {
            whenever(
                statsRepository.fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
            ).thenThrow(RuntimeException("Test error"))

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                SubscribersGraphUiState.Error::class.java
            )
            assertThat(
                (state as SubscribersGraphUiState.Error)
                    .message
            ).isEqualTo(UNKNOWN_ERROR)
        }

    @Test
    fun `when tab is selected, then data reloads with new tab`() =
        test {
            whenever(
                statsRepository.fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
            ).thenReturn(createSuccessResult())

            initViewModel()
            advanceUntilIdle()

            viewModel.onTabSelected(
                SubscribersGraphTab.WEEKS
            )
            advanceUntilIdle()

            assertThat(viewModel.selectedTab.value)
                .isEqualTo(SubscribersGraphTab.WEEKS)
            verify(statsRepository, times(2))
                .fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
        }

    @Test
    fun `when same tab is selected, then data does not reload`() =
        test {
            whenever(
                statsRepository.fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
            ).thenReturn(createSuccessResult())

            initViewModel()
            advanceUntilIdle()

            viewModel.onTabSelected(
                SubscribersGraphTab.DAYS
            )
            advanceUntilIdle()

            verify(statsRepository, times(1))
                .fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
        }

    @Test
    fun `when loadDataIfNeeded called twice, data loads once`() =
        test {
            whenever(
                statsRepository.fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
            ).thenReturn(createSuccessResult())

            viewModel = SubscribersGraphViewModel(
                selectedSiteRepository,
                accountStore,
                statsRepository,
                resourceProvider
            )
            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            verify(statsRepository, times(1))
                .fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
        }

    @Test
    fun `when refresh called, isRefreshing is false after`() =
        test {
            whenever(
                statsRepository.fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
            ).thenReturn(createSuccessResult())

            initViewModel()
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            assertThat(viewModel.isRefreshing.value)
                .isFalse()
        }

    @Test
    fun `when initialized, default tab is DAYS`() =
        test {
            viewModel = SubscribersGraphViewModel(
                selectedSiteRepository,
                accountStore,
                statsRepository,
                resourceProvider
            )

            assertThat(viewModel.selectedTab.value)
                .isEqualTo(SubscribersGraphTab.DAYS)
        }

    @Test
    fun `when access token is empty, then error state is emitted`() =
        test {
            whenever(accountStore.accessToken)
                .thenReturn("")

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                SubscribersGraphUiState.Error::class.java
            )
        }

    @Test
    fun `when exception with null message, then unknown error`() =
        test {
            whenever(
                statsRepository.fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
            ).thenThrow(RuntimeException())

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                SubscribersGraphUiState.Error::class.java
            )
            assertThat(
                (state as SubscribersGraphUiState.Error)
                    .message
            ).isEqualTo("Unknown error")
        }

    @Test
    fun `when data loads, correct unit param is used`() =
        test {
            whenever(
                statsRepository.fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
            ).thenReturn(createSuccessResult())

            initViewModel()
            advanceUntilIdle()

            verify(statsRepository)
                .fetchSubscribersGraph(
                    eq(TEST_SITE_ID),
                    eq("day"),
                    eq(DAYS_QUANTITY),
                    any()
                )
        }

    @Test
    fun `when weeks tab selected, correct params are used`() =
        test {
            whenever(
                statsRepository.fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
            ).thenReturn(createSuccessResult())

            initViewModel()
            advanceUntilIdle()

            viewModel.onTabSelected(
                SubscribersGraphTab.WEEKS
            )
            advanceUntilIdle()

            verify(statsRepository)
                .fetchSubscribersGraph(
                    eq(TEST_SITE_ID),
                    eq("week"),
                    eq(WEEKS_QUANTITY),
                    any()
                )
        }

    @Test
    fun `when months tab selected, correct params are used`() =
        test {
            whenever(
                statsRepository.fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
            ).thenReturn(createSuccessResult())

            initViewModel()
            advanceUntilIdle()

            viewModel.onTabSelected(
                SubscribersGraphTab.MONTHS
            )
            advanceUntilIdle()

            verify(statsRepository)
                .fetchSubscribersGraph(
                    eq(TEST_SITE_ID),
                    eq("month"),
                    eq(MONTHS_QUANTITY),
                    any()
                )
        }

    @Test
    fun `when years tab selected, correct params are used`() =
        test {
            whenever(
                statsRepository.fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
            ).thenReturn(createSuccessResult())

            initViewModel()
            advanceUntilIdle()

            viewModel.onTabSelected(
                SubscribersGraphTab.YEARS
            )
            advanceUntilIdle()

            verify(statsRepository)
                .fetchSubscribersGraph(
                    eq(TEST_SITE_ID),
                    eq("year"),
                    eq(YEARS_QUANTITY),
                    any()
                )
        }

    @Test
    fun `when data loads, points are sorted chronologically`() =
        test {
            whenever(
                statsRepository.fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
            ).thenReturn(
                SubscribersGraphResult.Success(
                    dataPoints = listOf(
                        SubscribersGraphDataPoint(
                            "2026-02-27", TEST_COUNT_3
                        ),
                        SubscribersGraphDataPoint(
                            "2026-02-25", TEST_COUNT_1
                        ),
                        SubscribersGraphDataPoint(
                            "2026-02-26", TEST_COUNT_2
                        )
                    )
                )
            )

            initViewModel()
            advanceUntilIdle()

            val loaded = viewModel.uiState.value
                as SubscribersGraphUiState.Loaded
            assertThat(loaded.dataPoints[0].count)
                .isEqualTo(TEST_COUNT_1)
            assertThat(loaded.dataPoints[1].count)
                .isEqualTo(TEST_COUNT_2)
            assertThat(loaded.dataPoints[2].count)
                .isEqualTo(TEST_COUNT_3)
        }

    @Test
    fun `when data loads with empty list, then loaded state has no points`() =
        test {
            whenever(
                statsRepository.fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
            ).thenReturn(
                SubscribersGraphResult.Success(
                    dataPoints = emptyList()
                )
            )

            initViewModel()
            advanceUntilIdle()

            val loaded = viewModel.uiState.value
                as SubscribersGraphUiState.Loaded
            assertThat(loaded.dataPoints).isEmpty()
        }

    @Test
    fun `when refresh called, data is fetched again`() =
        test {
            whenever(
                statsRepository.fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
            ).thenReturn(createSuccessResult())

            initViewModel()
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            verify(statsRepository, times(2))
                .fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
        }

    @Test
    fun `when error has auth error flag, then state reflects it`() =
        test {
            whenever(
                statsRepository.fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
            ).thenReturn(
                SubscribersGraphResult.Error(
                    messageResId =
                        R.string.stats_error_api,
                    isAuthError = true
                )
            )

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
                as SubscribersGraphUiState.Error
            assertThat(state.isAuthError).isTrue()
        }

    @Test
    fun `when data loads, then statsRepository init is called with token`() =
        test {
            whenever(
                statsRepository.fetchSubscribersGraph(
                    any(), any(), any(), any()
                )
            ).thenReturn(createSuccessResult())

            initViewModel()
            advanceUntilIdle()

            verify(statsRepository)
                .init(eq(TEST_ACCESS_TOKEN))
        }

    private fun createSuccessResult() =
        SubscribersGraphResult.Success(
            dataPoints = listOf(
                SubscribersGraphDataPoint(
                    "2026-02-25", TEST_COUNT_1
                ),
                SubscribersGraphDataPoint(
                    "2026-02-26", TEST_COUNT_2
                ),
                SubscribersGraphDataPoint(
                    "2026-02-27", TEST_COUNT_3
                )
            )
        )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN =
            "test_access_token"
        private const val FAILED_TO_LOAD_ERROR =
            "Failed to load stats"
        private const val UNKNOWN_ERROR = "Unknown error"
        private const val TEST_COUNT_1 = 100L
        private const val TEST_COUNT_2 = 150L
        private const val TEST_COUNT_3 = 200L
        private const val DAYS_QUANTITY = 30
        private const val WEEKS_QUANTITY = 12
        private const val MONTHS_QUANTITY = 6
        private const val YEARS_QUANTITY = 3
    }
}
