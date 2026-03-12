package org.wordpress.android.ui.newstats.mostpopulartime

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.datasource.StatsInsightsData
import org.wordpress.android.ui.newstats.repository.InsightsResult
import org.wordpress.android.ui.newstats.repository.StatsInsightsUseCase
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class MostPopularTimeViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository:
        SelectedSiteRepository

    @Mock
    private lateinit var resourceProvider:
        ResourceProvider

    @Mock
    private lateinit var statsInsightsUseCase:
        StatsInsightsUseCase

    private lateinit var viewModel:
        MostPopularTimeViewModel

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
        lenient().`when`(
            resourceProvider.getString(
                R.string.stats_error_no_site
            )
        ).thenReturn(NO_SITE_SELECTED_ERROR)
        lenient().`when`(
            resourceProvider.getString(
                R.string.stats_error_api
            )
        ).thenReturn(FAILED_TO_LOAD_ERROR)
        lenient().`when`(
            resourceProvider.getString(
                R.string.stats_error_unknown
            )
        ).thenReturn(UNKNOWN_ERROR)
    }

    @Test
    fun `when data loads, then loaded state has correct values`() =
        test {
            whenever(
                statsInsightsUseCase(any(), any())
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )

            viewModel = MostPopularTimeViewModel(
                selectedSiteRepository,
                resourceProvider,
                statsInsightsUseCase
            )
            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                MostPopularTimeCardUiState
                    .Loaded::class.java
            )
            with(
                state as MostPopularTimeCardUiState.Loaded
            ) {
                assertThat(bestDayPercent)
                    .isEqualTo("25")
                assertThat(bestHourPercent)
                    .isEqualTo("16")
            }
        }

    @Test
    fun `when no site selected, then error state`() =
        test {
            whenever(
                selectedSiteRepository.getSelectedSite()
            ).thenReturn(null)

            viewModel = MostPopularTimeViewModel(
                selectedSiteRepository,
                resourceProvider,
                statsInsightsUseCase
            )
            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                MostPopularTimeCardUiState
                    .Error::class.java
            )
            assertThat(
                (state as MostPopularTimeCardUiState.Error)
                    .message
            ).isEqualTo(NO_SITE_SELECTED_ERROR)
        }

    @Test
    fun `when fetch fails, then error state`() =
        test {
            whenever(
                statsInsightsUseCase(any(), any())
            ).thenReturn(
                InsightsResult.Error("Network error")
            )

            viewModel = MostPopularTimeViewModel(
                selectedSiteRepository,
                resourceProvider,
                statsInsightsUseCase
            )
            viewModel.loadData()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    MostPopularTimeCardUiState
                        .Error::class.java
                )
            assertThat(
                (viewModel.uiState.value
                    as MostPopularTimeCardUiState.Error)
                    .message
            ).isEqualTo(FAILED_TO_LOAD_ERROR)
        }

    @Suppress("TooGenericExceptionThrown")
    @Test
    fun `when exception thrown, then error state`() =
        test {
            whenever(
                statsInsightsUseCase(any(), any())
            ).thenAnswer {
                throw RuntimeException("Test")
            }

            viewModel = MostPopularTimeViewModel(
                selectedSiteRepository,
                resourceProvider,
                statsInsightsUseCase
            )
            viewModel.loadData()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    MostPopularTimeCardUiState
                        .Error::class.java
                )
            assertThat(
                (viewModel.uiState.value
                    as MostPopularTimeCardUiState.Error)
                    .message
            ).isEqualTo(UNKNOWN_ERROR)
        }

    @Test
    fun `when loadDataIfNeeded called multiple times, then loads once`() =
        test {
            whenever(
                statsInsightsUseCase(any(), any())
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )

            viewModel = MostPopularTimeViewModel(
                selectedSiteRepository,
                resourceProvider,
                statsInsightsUseCase
            )
            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    MostPopularTimeCardUiState
                        .Loaded::class.java
                )
        }

    @Test
    fun `when refresh called, then data is fetched`() =
        test {
            whenever(
                statsInsightsUseCase(any(), any())
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )

            viewModel = MostPopularTimeViewModel(
                selectedSiteRepository,
                resourceProvider,
                statsInsightsUseCase
            )
            viewModel.loadData()
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    MostPopularTimeCardUiState
                        .Loaded::class.java
                )
            verify(statsInsightsUseCase, times(1))
                .invoke(eq(TEST_SITE_ID), eq(true))
        }

    @Test
    fun `when onRetry called, then data is reloaded`() =
        test {
            whenever(
                statsInsightsUseCase(any(), any())
            ).thenReturn(
                InsightsResult.Error("error")
            )

            viewModel = MostPopularTimeViewModel(
                selectedSiteRepository,
                resourceProvider,
                statsInsightsUseCase
            )
            viewModel.loadData()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    MostPopularTimeCardUiState
                        .Error::class.java
                )

            whenever(
                statsInsightsUseCase(any(), any())
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )

            viewModel.onRetry()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    MostPopularTimeCardUiState
                        .Loaded::class.java
                )
        }

    @Test
    fun `when refresh is called, then isRefreshing becomes true then false`() =
        test {
            whenever(
                statsInsightsUseCase(any(), any())
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )

            viewModel = MostPopularTimeViewModel(
                selectedSiteRepository,
                resourceProvider,
                statsInsightsUseCase
            )
            viewModel.loadData()
            advanceUntilIdle()

            assertThat(viewModel.isRefreshing.value)
                .isFalse()

            viewModel.refresh()
            advanceUntilIdle()

            assertThat(viewModel.isRefreshing.value)
                .isFalse()
        }

    @Test
    fun `when mapToUiState with zero percents, then NoData`() {
        val data = StatsInsightsData(
            highestHour = 0,
            highestHourPercent = 0.0,
            highestDayOfWeek = 0,
            highestDayPercent = 0.0,
            years = emptyList()
        )
        val state =
            MostPopularTimeViewModel.mapToUiState(data)
        assertThat(state).isInstanceOf(
            MostPopularTimeCardUiState
                .NoData::class.java
        )
    }

    @Test
    fun `when mapToUiState with valid data, then Loaded state`() {
        val data = StatsInsightsData(
            highestHour = TEST_HIGHEST_HOUR,
            highestHourPercent = TEST_HOUR_PERCENT,
            highestDayOfWeek = TEST_DAY_OF_WEEK,
            highestDayPercent = TEST_DAY_PERCENT,
            years = emptyList()
        )
        val state =
            MostPopularTimeViewModel.mapToUiState(data)
            as MostPopularTimeCardUiState.Loaded
        assertThat(state.bestDayPercent)
            .isEqualTo("25")
        assertThat(state.bestHourPercent)
            .isEqualTo("16")
    }

    @Test
    fun `when mapToUiState with day 0, then Monday`() {
        val data = StatsInsightsData(
            highestHour = 10,
            highestHourPercent = 10.0,
            highestDayOfWeek = 0,
            highestDayPercent = 20.0,
            years = emptyList()
        )
        val state =
            MostPopularTimeViewModel.mapToUiState(data)
            as MostPopularTimeCardUiState.Loaded
        assertThat(state.bestDay).isEqualTo("Monday")
    }

    @Test
    fun `when mapToUiState with day 5, then Saturday`() {
        val data = StatsInsightsData(
            highestHour = 10,
            highestHourPercent = 10.0,
            highestDayOfWeek = 5,
            highestDayPercent = 20.0,
            years = emptyList()
        )
        val state =
            MostPopularTimeViewModel.mapToUiState(data)
            as MostPopularTimeCardUiState.Loaded
        assertThat(state.bestDay).isEqualTo("Saturday")
    }

    @Test
    fun `when mapToUiState with day 6, then Sunday`() {
        val data = StatsInsightsData(
            highestHour = 10,
            highestHourPercent = 10.0,
            highestDayOfWeek = 6,
            highestDayPercent = 20.0,
            years = emptyList()
        )
        val state =
            MostPopularTimeViewModel.mapToUiState(data)
            as MostPopularTimeCardUiState.Loaded
        assertThat(state.bestDay).isEqualTo("Sunday")
    }

    @Test
    fun `when mapToUiState with zero day percent, then NoData`() {
        val data = StatsInsightsData(
            highestHour = 14,
            highestHourPercent = 10.0,
            highestDayOfWeek = 3,
            highestDayPercent = 0.0,
            years = emptyList()
        )
        val state =
            MostPopularTimeViewModel.mapToUiState(data)
        assertThat(state).isInstanceOf(
            MostPopularTimeCardUiState
                .NoData::class.java
        )
    }

    @Test
    fun `when mapToUiState with zero hour percent, then NoData`() {
        val data = StatsInsightsData(
            highestHour = 14,
            highestHourPercent = 0.0,
            highestDayOfWeek = 3,
            highestDayPercent = 10.0,
            years = emptyList()
        )
        val state =
            MostPopularTimeViewModel.mapToUiState(data)
        assertThat(state).isInstanceOf(
            MostPopularTimeCardUiState
                .NoData::class.java
        )
    }

    @Test
    fun `when mapToUiState with invalid day, then empty day name`() {
        val data = StatsInsightsData(
            highestHour = 10,
            highestHourPercent = 10.0,
            highestDayOfWeek = 7,
            highestDayPercent = 20.0,
            years = emptyList()
        )
        val state =
            MostPopularTimeViewModel.mapToUiState(data)
            as MostPopularTimeCardUiState.Loaded
        assertThat(state.bestDay).isEmpty()
    }

    @Test
    fun `when mapToUiState with fractional percent, then rounded`() {
        val data = StatsInsightsData(
            highestHour = 14,
            highestHourPercent = 11.18,
            highestDayOfWeek = 3,
            highestDayPercent = 22.66,
            years = emptyList()
        )
        val state =
            MostPopularTimeViewModel.mapToUiState(data)
            as MostPopularTimeCardUiState.Loaded
        assertThat(state.bestDayPercent)
            .isEqualTo("23")
        assertThat(state.bestHourPercent)
            .isEqualTo("11")
    }

    @Test
    fun `when refresh fails after success, then loadDataIfNeeded reloads`() =
        test {
            whenever(
                statsInsightsUseCase(any(), any())
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )

            viewModel = MostPopularTimeViewModel(
                selectedSiteRepository,
                resourceProvider,
                statsInsightsUseCase
            )
            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    MostPopularTimeCardUiState
                        .Loaded::class.java
                )

            whenever(
                statsInsightsUseCase(any(), any())
            ).thenReturn(
                InsightsResult.Error("Network error")
            )
            viewModel.refresh()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    MostPopularTimeCardUiState
                        .Error::class.java
                )

            whenever(
                statsInsightsUseCase(any(), any())
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )
            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    MostPopularTimeCardUiState
                        .Loaded::class.java
                )
        }

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_HIGHEST_HOUR = 16
        private const val TEST_HOUR_PERCENT = 15.5
        private const val TEST_DAY_OF_WEEK = 3
        private const val TEST_DAY_PERCENT = 25.0
        private const val NO_SITE_SELECTED_ERROR =
            "No site selected"
        private const val FAILED_TO_LOAD_ERROR =
            "Failed to load stats"
        private const val UNKNOWN_ERROR =
            "Unknown error"

        private fun createTestInsightsData() =
            StatsInsightsData(
                highestHour = TEST_HIGHEST_HOUR,
                highestHourPercent = TEST_HOUR_PERCENT,
                highestDayOfWeek = TEST_DAY_OF_WEEK,
                highestDayPercent = TEST_DAY_PERCENT,
                years = emptyList()
            )
    }
}
