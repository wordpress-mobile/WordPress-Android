package org.wordpress.android.ui.newstats.mostpopularday

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.lenient
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
class MostPopularDayViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository:
        SelectedSiteRepository

    @Mock
    private lateinit var resourceProvider:
        ResourceProvider

    @Mock
    private lateinit var statsSummaryUseCase:
        StatsSummaryUseCase

    private lateinit var viewModel:
        MostPopularDayViewModel

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

    private suspend fun initViewModel() {
        whenever(
            statsSummaryUseCase(TEST_SITE_ID)
        ).thenReturn(
            StatsSummaryResult.Success(createTestData())
        )
        viewModel = MostPopularDayViewModel(
            selectedSiteRepository,
            resourceProvider,
            statsSummaryUseCase
        )
        viewModel.loadData()
    }

    @Test
    fun `when data loads, then loaded state has correct day`() =
        test {
            whenever(
                statsSummaryUseCase(TEST_SITE_ID)
            ).thenReturn(
                StatsSummaryResult.Success(
                    data = createTestData()
                )
            )

            viewModel = MostPopularDayViewModel(
                selectedSiteRepository,
                resourceProvider,
                statsSummaryUseCase
            )
            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                MostPopularDayCardUiState
                    .Loaded::class.java
            )
            with(
                state as MostPopularDayCardUiState.Loaded
            ) {
                assertThat(dayAndMonth)
                    .isEqualTo("February 22")
                assertThat(year).isEqualTo("2022")
                assertThat(views)
                    .isEqualTo(TEST_BEST_DAY_TOTAL)
            }
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
                MostPopularDayCardUiState
                    .Error::class.java
            )
        }

    @Test
    fun `when fetch fails, then error state`() = test {
        whenever(
            statsSummaryUseCase(TEST_SITE_ID)
        ).thenReturn(
            StatsSummaryResult.Error("Network error")
        )

        viewModel = MostPopularDayViewModel(
            selectedSiteRepository,
            resourceProvider,
            statsSummaryUseCase
        )
        viewModel.loadData()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value)
            .isInstanceOf(
                MostPopularDayCardUiState
                    .Error::class.java
            )
    }

    @Test
    fun `when loadDataIfNeeded called multiple times, then loads once`() =
        test {
            whenever(
                statsSummaryUseCase(TEST_SITE_ID)
            ).thenReturn(
                StatsSummaryResult.Success(
                    data = createTestData()
                )
            )

            viewModel = MostPopularDayViewModel(
                selectedSiteRepository,
                resourceProvider,
                statsSummaryUseCase
            )
            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    MostPopularDayCardUiState
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
                    data = createTestData()
                )
            )
            whenever(
                statsSummaryUseCase(
                    TEST_SITE_ID,
                    forceRefresh = true
                )
            ).thenReturn(
                StatsSummaryResult.Success(
                    data = createTestData()
                )
            )

            viewModel = MostPopularDayViewModel(
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
                    MostPopularDayCardUiState
                        .Loaded::class.java
                )
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

            viewModel = MostPopularDayViewModel(
                selectedSiteRepository,
                resourceProvider,
                statsSummaryUseCase
            )
            viewModel.loadData()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    MostPopularDayCardUiState
                        .Error::class.java
                )
            assertThat(
                (viewModel.uiState.value
                    as MostPopularDayCardUiState.Error)
                    .message
            ).isEqualTo(UNKNOWN_ERROR)
        }

    @Test
    fun `when mapToUiState called, then percentage is calculated`() {
        val data = StatsSummaryData(
            views = 1000000L,
            visitors = 0L,
            posts = 0L,
            comments = 0L,
            viewsBestDay = "2022-02-22",
            viewsBestDayTotal = 680L
        )
        val state =
            MostPopularDayViewModel.mapToUiState(data)
            as MostPopularDayCardUiState.Loaded
        assertThat(state.viewsPercentage)
            .isEqualTo("0.1")
        assertThat(state.dayAndMonth)
            .isEqualTo("February 22")
        assertThat(state.year).isEqualTo("2022")
    }

    @Test
    fun `when viewsBestDay is empty, then NoData state`() {
        val data = StatsSummaryData(
            views = 100L,
            visitors = 0L,
            posts = 0L,
            comments = 0L,
            viewsBestDay = "",
            viewsBestDayTotal = 0L
        )
        val state =
            MostPopularDayViewModel.mapToUiState(data)
        assertThat(state).isInstanceOf(
            MostPopularDayCardUiState
                .NoData::class.java
        )
    }

    @Test
    fun `when total views is zero, then percentage is zero`() {
        val data = StatsSummaryData(
            views = 0L,
            visitors = 0L,
            posts = 0L,
            comments = 0L,
            viewsBestDay = "2022-02-22",
            viewsBestDayTotal = 0L
        )
        val state =
            MostPopularDayViewModel.mapToUiState(data)
            as MostPopularDayCardUiState.Loaded
        assertThat(state.viewsPercentage)
            .isEqualTo("0")
    }

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_VIEWS = 6782856L
        private const val TEST_BEST_DAY = "2022-02-22"
        private const val TEST_BEST_DAY_TOTAL = 4600L
        private const val NO_SITE_SELECTED_ERROR =
            "No site selected"
        private const val FAILED_TO_LOAD_ERROR =
            "Failed to load stats"
        private const val UNKNOWN_ERROR =
            "Unknown error"

        private fun createTestData() = StatsSummaryData(
            views = TEST_VIEWS,
            visitors = 154791L,
            posts = 42L,
            comments = 85L,
            viewsBestDay = TEST_BEST_DAY,
            viewsBestDayTotal = TEST_BEST_DAY_TOTAL
        )
    }
}
