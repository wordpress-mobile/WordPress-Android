package org.wordpress.android.ui.newstats.alltimestats

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
import org.wordpress.android.ui.newstats.datasource.StatsSummaryData
import org.wordpress.android.ui.newstats.repository.StatsSummaryResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class AllTimeStatsViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository:
        SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: AllTimeStatsViewModel

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
                R.string.stats_error_no_site
            )
        ).thenReturn(NO_SITE_SELECTED_ERROR)
        whenever(
            resourceProvider.getString(
                R.string.stats_error_api
            )
        ).thenReturn(FAILED_TO_LOAD_ERROR)
        whenever(
            resourceProvider.getString(
                R.string.stats_error_unknown
            )
        ).thenReturn(UNKNOWN_ERROR)
    }

    private fun initViewModel() {
        viewModel = AllTimeStatsViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider
        )
        viewModel.loadData()
    }

    @Test
    fun `when no site selected, then error state`() =
        test {
            whenever(
                selectedSiteRepository.getSelectedSite()
            ).thenReturn(null)

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                AllTimeStatsCardUiState.Error::class.java
            )
            assertThat(
                (state as AllTimeStatsCardUiState.Error)
                    .message
            ).isEqualTo(NO_SITE_SELECTED_ERROR)
        }

    @Test
    fun `when access token is null, then error state`() =
        test {
            whenever(accountStore.accessToken)
                .thenReturn(null)

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                AllTimeStatsCardUiState.Error::class.java
            )
            assertThat(
                (state as AllTimeStatsCardUiState.Error)
                    .message
            ).isEqualTo(FAILED_TO_LOAD_ERROR)
        }

    @Test
    fun `when data loads successfully, then loaded state`() =
        test {
            whenever(
                statsRepository.fetchStatsSummary(any(), any())
            ).thenReturn(
                StatsSummaryResult.Success(
                    data = createTestData()
                )
            )

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                AllTimeStatsCardUiState.Loaded::class.java
            )
            with(
                state as AllTimeStatsCardUiState.Loaded
            ) {
                assertThat(views)
                    .isEqualTo(TEST_VIEWS)
                assertThat(visitors)
                    .isEqualTo(TEST_VISITORS)
                assertThat(posts)
                    .isEqualTo(TEST_POSTS)
                assertThat(comments)
                    .isEqualTo(TEST_COMMENTS)
            }
        }

    @Test
    fun `when fetch fails, then error state`() = test {
        whenever(
            statsRepository.fetchStatsSummary(any(), any())
        ).thenReturn(
            StatsSummaryResult.Error("Network error")
        )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(
            AllTimeStatsCardUiState.Error::class.java
        )
        assertThat(
            (state as AllTimeStatsCardUiState.Error)
                .message
        ).isEqualTo(FAILED_TO_LOAD_ERROR)
    }

    @Test
    fun `when exception thrown, then error state`() =
        test {
            whenever(
                statsRepository.fetchStatsSummary(any(), any())
            ).thenThrow(RuntimeException("Test"))

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                AllTimeStatsCardUiState.Error::class.java
            )
            assertThat(
                (state as AllTimeStatsCardUiState.Error)
                    .message
            ).isEqualTo(UNKNOWN_ERROR)
        }

    @Test
    fun `when loadDataIfNeeded called multiple times, then loads once`() =
        test {
            whenever(
                statsRepository.fetchStatsSummary(any(), any())
            ).thenReturn(
                StatsSummaryResult.Success(
                    data = createTestData()
                )
            )

            viewModel = AllTimeStatsViewModel(
                selectedSiteRepository,
                accountStore,
                statsRepository,
                resourceProvider
            )
            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            verify(statsRepository, times(1))
                .fetchStatsSummary(eq(TEST_SITE_ID), any())
        }

    @Test
    fun `when onRetry called, then data is reloaded`() =
        test {
            whenever(
                statsRepository.fetchStatsSummary(any(), any())
            ).thenReturn(
                StatsSummaryResult.Success(
                    data = createTestData()
                )
            )

            initViewModel()
            advanceUntilIdle()

            viewModel.onRetry()
            advanceUntilIdle()

            verify(statsRepository, times(2))
                .fetchStatsSummary(eq(TEST_SITE_ID), any())
        }

    @Test
    fun `when refresh called, then data is fetched`() =
        test {
            whenever(
                statsRepository.fetchStatsSummary(any(), any())
            ).thenReturn(
                StatsSummaryResult.Success(
                    data = createTestData()
                )
            )

            initViewModel()
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            verify(statsRepository, times(2))
                .fetchStatsSummary(eq(TEST_SITE_ID), any())
        }

    @Test
    fun `when refresh called, then isRefreshing resets`() =
        test {
            whenever(
                statsRepository.fetchStatsSummary(any(), any())
            ).thenReturn(
                StatsSummaryResult.Success(
                    data = createTestData()
                )
            )

            initViewModel()
            advanceUntilIdle()

            assertThat(viewModel.isRefreshing.value)
                .isFalse()

            viewModel.refresh()
            advanceUntilIdle()

            assertThat(viewModel.isRefreshing.value)
                .isFalse()
        }

    @Test
    fun `when refresh fails after success, then loadDataIfNeeded reloads`() =
        test {
            whenever(
                statsRepository.fetchStatsSummary(any(), any())
            ).thenReturn(
                StatsSummaryResult.Success(
                    data = createTestData()
                )
            )

            viewModel = AllTimeStatsViewModel(
                selectedSiteRepository,
                accountStore,
                statsRepository,
                resourceProvider
            )
            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    AllTimeStatsCardUiState
                        .Loaded::class.java
                )

            whenever(
                statsRepository.fetchStatsSummary(any(), any())
            ).thenReturn(
                StatsSummaryResult.Error("Network error")
            )
            viewModel.refresh()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    AllTimeStatsCardUiState
                        .Error::class.java
                )

            whenever(
                statsRepository.fetchStatsSummary(any(), any())
            ).thenReturn(
                StatsSummaryResult.Success(
                    data = createTestData()
                )
            )
            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    AllTimeStatsCardUiState
                        .Loaded::class.java
                )
            verify(statsRepository, times(3))
                .fetchStatsSummary(eq(TEST_SITE_ID), any())
        }

    private fun createTestData() = StatsSummaryData(
        views = TEST_VIEWS,
        visitors = TEST_VISITORS,
        posts = TEST_POSTS,
        comments = TEST_COMMENTS,
        viewsBestDay = "2022-02-22",
        viewsBestDayTotal = 4600L
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN =
            "test_access_token"
        private const val TEST_VIEWS = 6782856L
        private const val TEST_VISITORS = 154791L
        private const val TEST_POSTS = 42L
        private const val TEST_COMMENTS = 85L
        private const val NO_SITE_SELECTED_ERROR =
            "No site selected"
        private const val FAILED_TO_LOAD_ERROR =
            "Failed to load stats"
        private const val UNKNOWN_ERROR =
            "Unknown error"
    }
}
