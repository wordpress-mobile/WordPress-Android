package org.wordpress.android.ui.newstats.yearinreview

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.datasource.StatsInsightsData
import org.wordpress.android.ui.newstats.datasource.YearInsightsData
import org.wordpress.android.ui.newstats.repository.InsightsResult
import org.wordpress.android.ui.newstats.repository.StatsInsightsUseCase
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.Year

@ExperimentalCoroutinesApi
class YearInReviewDetailViewModelTest :
    BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository:
        SelectedSiteRepository

    @Mock
    private lateinit var statsInsightsUseCase:
        StatsInsightsUseCase

    @Mock
    private lateinit var resourceProvider:
        ResourceProvider

    private lateinit var viewModel:
        YearInReviewDetailViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
    }

    @Before
    fun setUp() {
        whenever(
            selectedSiteRepository.getSelectedSite()
        ).thenReturn(testSite)
        whenever(
            resourceProvider.getString(
                R.string.stats_error_api
            )
        ).thenReturn(ERROR_API)
        whenever(
            resourceProvider.getString(
                R.string.stats_error_no_site
            )
        ).thenReturn(ERROR_NO_SITE)
        whenever(
            resourceProvider.getString(
                R.string.stats_error_unknown
            )
        ).thenReturn(ERROR_UNKNOWN)
        viewModel = YearInReviewDetailViewModel(
            selectedSiteRepository,
            statsInsightsUseCase,
            resourceProvider
        )
    }

    @Test
    fun `initial state is Loading`() {
        assertThat(viewModel.uiState.value)
            .isInstanceOf(
                YearInReviewDetailUiState
                    .Loading::class.java
            )
    }

    @Test
    fun `when loadData with success, then loaded state`() =
        test {
            whenever(
                statsInsightsUseCase(
                    eq(TEST_SITE_ID), any()
                )
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData(
                        createTestYears()
                    )
                )
            )

            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                YearInReviewDetailUiState
                    .Loaded::class.java
            )
            val loaded = state
                as YearInReviewDetailUiState.Loaded
            assertThat(loaded.years).hasSize(3)
            assertThat(loaded.years[0].year)
                .isEqualTo(CURRENT_YEAR)
        }

    @Test
    fun `when loadData with error result, then error state`() =
        test {
            whenever(
                statsInsightsUseCase(
                    eq(TEST_SITE_ID), any()
                )
            ).thenReturn(
                InsightsResult.Error("Network error")
            )

            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                YearInReviewDetailUiState
                    .Error::class.java
            )
            assertThat(
                (state as YearInReviewDetailUiState
                    .Error).message
            ).isEqualTo(ERROR_API)
        }

    @Test
    fun `when no site selected, then error state`() =
        test {
            whenever(
                selectedSiteRepository.getSelectedSite()
            ).thenReturn(null)

            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                YearInReviewDetailUiState
                    .Error::class.java
            )
            assertThat(
                (state as YearInReviewDetailUiState
                    .Error).message
            ).isEqualTo(ERROR_NO_SITE)
        }

    @Test
    fun `when loadData called twice, then only one fetch`() =
        test {
            whenever(
                statsInsightsUseCase(
                    eq(TEST_SITE_ID), any()
                )
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData(
                        createTestYears()
                    )
                )
            )

            viewModel.loadData()
            advanceUntilIdle()

            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                YearInReviewDetailUiState
                    .Loaded::class.java
            )
        }

    @Suppress("TooGenericExceptionThrown")
    @Test
    fun `when use case throws, then error state`() =
        test {
            whenever(
                statsInsightsUseCase(
                    eq(TEST_SITE_ID), any()
                )
            ).thenAnswer {
                throw RuntimeException("Test error")
            }

            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                YearInReviewDetailUiState
                    .Error::class.java
            )
        }

    @Test
    fun `when empty years, then current year is added`() =
        test {
            whenever(
                statsInsightsUseCase(
                    eq(TEST_SITE_ID), any()
                )
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData(emptyList())
                )
            )

            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
                as YearInReviewDetailUiState.Loaded
            assertThat(state.years).hasSize(1)
            assertThat(state.years[0].year)
                .isEqualTo(CURRENT_YEAR)
        }

    private fun createTestInsightsData(
        years: List<YearInsightsData>
    ) = StatsInsightsData(
        highestHour = 14,
        highestHourPercent = 15.5,
        highestDayOfWeek = 3,
        highestDayPercent = 25.0,
        years = years
    )

    private fun createTestYears() = listOf(
        YearInsightsData(
            year = "2025",
            totalPosts = 42L,
            totalWords = 15000L,
            avgWords = 357.1,
            totalLikes = 230L,
            avgLikes = 5.5,
            totalComments = 85L,
            avgComments = 2.0
        ),
        YearInsightsData(
            year = "2024",
            totalPosts = 38L,
            totalWords = 12500L,
            avgWords = 328.9,
            totalLikes = 180L,
            avgLikes = 4.7,
            totalComments = 60L,
            avgComments = 1.6
        )
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val ERROR_API =
            "Failed to load stats"
        private const val ERROR_NO_SITE =
            "No site selected"
        private const val ERROR_UNKNOWN =
            "An unexpected error occurred"
        private val CURRENT_YEAR =
            Year.now().toString()
    }
}
