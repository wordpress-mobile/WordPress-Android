package org.wordpress.android.ui.newstats.alltimestats

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.datasource.StatsSummaryData
import org.wordpress.android.ui.newstats.repository.StatsSummaryResult
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class AllTimeStatsViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository:
        SelectedSiteRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: AllTimeStatsViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    private var mockResult: StatsSummaryResult =
        StatsSummaryResult.Success(createTestData())

    private val testProvider:
        suspend (Long, Boolean) -> StatsSummaryResult =
        { _, _ -> mockResult }

    @Before
    fun setUp() {
        whenever(
            selectedSiteRepository.getSelectedSite()
        ).thenReturn(testSite)
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
            resourceProvider
        )
        viewModel.summaryProvider = testProvider
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
    fun `when provider is null, then error state`() =
        test {
            viewModel = AllTimeStatsViewModel(
                selectedSiteRepository,
                resourceProvider
            )
            viewModel.loadData()
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
            mockResult = StatsSummaryResult.Success(
                data = createTestData()
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
        mockResult =
            StatsSummaryResult.Error("Network error")

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
            val throwingProvider:
                suspend (Long, Boolean) ->
                StatsSummaryResult =
                { _, _ -> throw RuntimeException("Test") }

            viewModel = AllTimeStatsViewModel(
                selectedSiteRepository,
                resourceProvider
            )
            viewModel.summaryProvider = throwingProvider
            viewModel.loadData()
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
            var callCount = 0
            val countingProvider:
                suspend (Long, Boolean) ->
                StatsSummaryResult =
                { _, _ ->
                    callCount++
                    StatsSummaryResult.Success(
                        data = createTestData()
                    )
                }

            viewModel = AllTimeStatsViewModel(
                selectedSiteRepository,
                resourceProvider
            )
            viewModel.summaryProvider = countingProvider
            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            assertThat(callCount).isEqualTo(1)
        }

    @Test
    fun `when onRetry called, then data is reloaded`() =
        test {
            var callCount = 0
            val countingProvider:
                suspend (Long, Boolean) ->
                StatsSummaryResult =
                { _, _ ->
                    callCount++
                    StatsSummaryResult.Success(
                        data = createTestData()
                    )
                }

            viewModel = AllTimeStatsViewModel(
                selectedSiteRepository,
                resourceProvider
            )
            viewModel.summaryProvider = countingProvider
            viewModel.loadData()
            advanceUntilIdle()

            viewModel.onRetry()
            advanceUntilIdle()

            assertThat(callCount).isEqualTo(2)
        }

    @Test
    fun `when refresh called, then data is fetched`() =
        test {
            var callCount = 0
            val countingProvider:
                suspend (Long, Boolean) ->
                StatsSummaryResult =
                { _, _ ->
                    callCount++
                    StatsSummaryResult.Success(
                        data = createTestData()
                    )
                }

            viewModel = AllTimeStatsViewModel(
                selectedSiteRepository,
                resourceProvider
            )
            viewModel.summaryProvider = countingProvider
            viewModel.loadData()
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            assertThat(callCount).isEqualTo(2)
        }

    @Test
    fun `when refresh called, then isRefreshing resets`() =
        test {
            mockResult = StatsSummaryResult.Success(
                data = createTestData()
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
            mockResult = StatsSummaryResult.Success(
                data = createTestData()
            )

            viewModel = AllTimeStatsViewModel(
                selectedSiteRepository,
                resourceProvider
            )
            viewModel.summaryProvider = testProvider
            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    AllTimeStatsCardUiState
                        .Loaded::class.java
                )

            mockResult =
                StatsSummaryResult.Error("Network error")
            viewModel.refresh()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    AllTimeStatsCardUiState
                        .Error::class.java
                )

            mockResult = StatsSummaryResult.Success(
                data = createTestData()
            )
            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    AllTimeStatsCardUiState
                        .Loaded::class.java
                )
        }

    companion object {
        private const val TEST_SITE_ID = 123L
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

        private fun createTestData() = StatsSummaryData(
            views = TEST_VIEWS,
            visitors = TEST_VISITORS,
            posts = TEST_POSTS,
            comments = TEST_COMMENTS,
            viewsBestDay = "2022-02-22",
            viewsBestDayTotal = 4600L
        )
    }
}
