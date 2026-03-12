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
import org.wordpress.android.ui.newstats.repository.StatsSummaryUseCase
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class AllTimeStatsViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository:
        SelectedSiteRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var statsSummaryUseCase:
        StatsSummaryUseCase

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

    private suspend fun initViewModel() {
        whenever(
            statsSummaryUseCase(TEST_SITE_ID)
        ).thenReturn(
            StatsSummaryResult.Success(createTestData())
        )
        viewModel = AllTimeStatsViewModel(
            selectedSiteRepository,
            resourceProvider,
            statsSummaryUseCase
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
    fun `when data loads successfully, then loaded state`() =
        test {
            whenever(
                statsSummaryUseCase(TEST_SITE_ID)
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestData()
                )
            )

            viewModel = AllTimeStatsViewModel(
                selectedSiteRepository,
                resourceProvider,
                statsSummaryUseCase
            )
            viewModel.loadData()
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
            statsSummaryUseCase(TEST_SITE_ID)
        ).thenReturn(
            StatsSummaryResult.Error("Network error")
        )

        viewModel = AllTimeStatsViewModel(
            selectedSiteRepository,
            resourceProvider,
            statsSummaryUseCase
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

    @Suppress("TooGenericExceptionThrown")
    @Test
    fun `when exception thrown, then error state`() =
        test {
            whenever(
                statsSummaryUseCase(TEST_SITE_ID)
            ).thenAnswer {
                throw RuntimeException("Test")
            }

            viewModel = AllTimeStatsViewModel(
                selectedSiteRepository,
                resourceProvider,
                statsSummaryUseCase
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
            ).isEqualTo(UNKNOWN_ERROR)
        }

    @Test
    fun `when loadDataIfNeeded called multiple times, then loads once`() =
        test {
            whenever(
                statsSummaryUseCase(TEST_SITE_ID)
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestData()
                )
            )

            viewModel = AllTimeStatsViewModel(
                selectedSiteRepository,
                resourceProvider,
                statsSummaryUseCase
            )
            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    AllTimeStatsCardUiState
                        .Loaded::class.java
                )
        }

    @Test
    fun `when onRetry called, then data is reloaded`() =
        test {
            whenever(
                statsSummaryUseCase(TEST_SITE_ID)
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestData()
                )
            )

            viewModel = AllTimeStatsViewModel(
                selectedSiteRepository,
                resourceProvider,
                statsSummaryUseCase
            )
            viewModel.loadData()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    AllTimeStatsCardUiState
                        .Loaded::class.java
                )

            viewModel.onRetry()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    AllTimeStatsCardUiState
                        .Loaded::class.java
                )
        }

    @Test
    fun `when refresh called, then data is fetched`() =
        test {
            whenever(
                statsSummaryUseCase(TEST_SITE_ID)
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestData()
                )
            )
            whenever(
                statsSummaryUseCase(
                    TEST_SITE_ID,
                    forceRefresh = true
                )
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestData()
                )
            )

            viewModel = AllTimeStatsViewModel(
                selectedSiteRepository,
                resourceProvider,
                statsSummaryUseCase
            )
            viewModel.loadData()
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    AllTimeStatsCardUiState
                        .Loaded::class.java
                )
        }

    @Test
    fun `when refresh called, then isRefreshing resets`() =
        test {
            whenever(
                statsSummaryUseCase(TEST_SITE_ID)
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestData()
                )
            )
            whenever(
                statsSummaryUseCase(
                    TEST_SITE_ID,
                    forceRefresh = true
                )
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestData()
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
                statsSummaryUseCase(TEST_SITE_ID)
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestData()
                )
            )

            viewModel = AllTimeStatsViewModel(
                selectedSiteRepository,
                resourceProvider,
                statsSummaryUseCase
            )
            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    AllTimeStatsCardUiState
                        .Loaded::class.java
                )

            whenever(
                statsSummaryUseCase(
                    TEST_SITE_ID,
                    forceRefresh = true
                )
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
                statsSummaryUseCase(TEST_SITE_ID)
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestData()
                )
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
