package org.wordpress.android.ui.newstats.yearinreview

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
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.datasource.StatsInsightsData
import org.wordpress.android.ui.newstats.datasource.YearInsightsData
import org.wordpress.android.ui.newstats.repository.InsightsResult
import org.wordpress.android.ui.newstats.repository.StatsInsightsUseCase
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.Year

@ExperimentalCoroutinesApi
class YearInReviewViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository:
        SelectedSiteRepository

    @Mock
    private lateinit var statsInsightsUseCase:
        StatsInsightsUseCase

    @Mock
    private lateinit var resourceProvider:
        ResourceProvider

    private lateinit var viewModel: YearInReviewViewModel

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

    private fun initViewModel() {
        viewModel = YearInReviewViewModel(
            selectedSiteRepository,
            statsInsightsUseCase,
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
                YearInReviewCardUiState.Error::class.java
            )
            assertThat(
                (state as YearInReviewCardUiState.Error)
                    .message
            ).isEqualTo(NO_SITE_SELECTED_ERROR)
        }

    @Test
    fun `when data loads successfully, then loaded state is emitted`() =
        test {
            whenever(statsInsightsUseCase(any(), any()))
                .thenReturn(createSuccessResult())

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                YearInReviewCardUiState.Loaded::class.java
            )
            with(
                state as YearInReviewCardUiState.Loaded
            ) {
                assertThat(years).hasSize(3)
                assertThat(years[0].year)
                    .isEqualTo(CURRENT_YEAR)
                assertThat(years[1].year)
                    .isEqualTo("2025")
                assertThat(years[1].totalPosts)
                    .isEqualTo(TEST_TOTAL_POSTS)
                assertThat(years[1].totalWords)
                    .isEqualTo(TEST_TOTAL_WORDS)
                assertThat(years[1].totalLikes)
                    .isEqualTo(TEST_TOTAL_LIKES)
                assertThat(years[1].totalComments)
                    .isEqualTo(TEST_TOTAL_COMMENTS)
            }
        }

    @Test
    fun `when fetch fails with error, then error state is emitted`() =
        test {
            whenever(statsInsightsUseCase(any(), any()))
                .thenReturn(
                    InsightsResult.Error("Network error")
                )

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                YearInReviewCardUiState.Error::class.java
            )
            assertThat(
                (state as YearInReviewCardUiState.Error)
                    .message
            ).isEqualTo(FAILED_TO_LOAD_ERROR)
        }

    @Test
    fun `when exception is thrown during fetch, then error state is emitted`() =
        test {
            whenever(statsInsightsUseCase(any(), any()))
                .thenThrow(
                    RuntimeException("Test exception")
                )

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                YearInReviewCardUiState.Error::class.java
            )
            assertThat(
                (state as YearInReviewCardUiState.Error)
                    .message
            ).isEqualTo(UNKNOWN_ERROR)
        }

    @Test
    fun `when exception with null message, then unknown error message`() =
        test {
            whenever(statsInsightsUseCase(any(), any()))
                .thenThrow(RuntimeException())

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                YearInReviewCardUiState.Error::class.java
            )
            assertThat(
                (state as YearInReviewCardUiState.Error)
                    .message
            ).isEqualTo(UNKNOWN_ERROR)
        }

    @Test
    fun `when onRetry is called, then data is reloaded`() =
        test {
            whenever(statsInsightsUseCase(any(), any()))
                .thenReturn(createSuccessResult())

            initViewModel()
            advanceUntilIdle()

            viewModel.onRetry()
            advanceUntilIdle()

            verify(statsInsightsUseCase, times(2))
                .invoke(eq(TEST_SITE_ID), any())
        }

    @Test
    fun `when refresh is called, then isRefreshing becomes true then false`() =
        test {
            whenever(statsInsightsUseCase(any(), any()))
                .thenReturn(createSuccessResult())

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
    fun `when refresh is called, then data is fetched`() =
        test {
            whenever(statsInsightsUseCase(any(), any()))
                .thenReturn(createSuccessResult())

            initViewModel()
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            verify(statsInsightsUseCase, times(2))
                .invoke(eq(TEST_SITE_ID), any())
        }

    @Test
    fun `when use case returns auth error, then error state is emitted`() =
        test {
            whenever(statsInsightsUseCase(any(), any()))
                .thenReturn(
                    InsightsResult.Error(
                        "No access token"
                    )
                )

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                YearInReviewCardUiState.Error::class.java
            )
            assertThat(
                (state as YearInReviewCardUiState.Error)
                    .message
            ).isEqualTo(FAILED_TO_LOAD_ERROR)
        }

    @Test
    fun `when loadDataIfNeeded is called multiple times, then data is only loaded once`() =
        test {
            whenever(statsInsightsUseCase(any(), any()))
                .thenReturn(createSuccessResult())

            viewModel = YearInReviewViewModel(
                selectedSiteRepository,
                statsInsightsUseCase,
                resourceProvider
            )
            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            verify(statsInsightsUseCase, times(1))
                .invoke(eq(TEST_SITE_ID), any())
        }

    @Test
    fun `when error state retry is clicked, then data is reloaded`() =
        test {
            whenever(
                selectedSiteRepository.getSelectedSite()
            ).thenReturn(null)

            initViewModel()
            advanceUntilIdle()

            val errorState = viewModel.uiState.value
                as YearInReviewCardUiState.Error

            whenever(
                selectedSiteRepository.getSelectedSite()
            ).thenReturn(testSite)
            whenever(statsInsightsUseCase(any(), any()))
                .thenReturn(createSuccessResult())

            errorState.onRetry()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    YearInReviewCardUiState
                        .Loaded::class.java
                )
        }

    @Test
    fun `when data loads with empty years, then current year is added`() =
        test {
            whenever(statsInsightsUseCase(any(), any()))
                .thenReturn(
                    InsightsResult.Success(
                        data = createTestInsightsData(
                            emptyList()
                        )
                    )
                )

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
                as YearInReviewCardUiState.Loaded
            assertThat(state.years).hasSize(1)
            assertThat(state.years[0].year)
                .isEqualTo(CURRENT_YEAR)
            assertThat(state.years[0].totalPosts)
                .isEqualTo(0L)
        }

    @Test
    fun `when current year exists in data, then no duplicate is added`() =
        test {
            val yearsWithCurrent = listOf(
                YearInsightsData(
                    year = CURRENT_YEAR,
                    totalPosts = TEST_TOTAL_POSTS,
                    totalWords = TEST_TOTAL_WORDS,
                    avgWords = TEST_AVG_WORDS,
                    totalLikes = TEST_TOTAL_LIKES,
                    avgLikes = TEST_AVG_LIKES,
                    totalComments = TEST_TOTAL_COMMENTS,
                    avgComments = TEST_AVG_COMMENTS
                )
            )
            whenever(statsInsightsUseCase(any(), any()))
                .thenReturn(
                    InsightsResult.Success(
                        data = createTestInsightsData(
                            yearsWithCurrent
                        )
                    )
                )

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
                as YearInReviewCardUiState.Loaded
            assertThat(state.years).hasSize(1)
            assertThat(state.years[0].year)
                .isEqualTo(CURRENT_YEAR)
            assertThat(state.years[0].totalPosts)
                .isEqualTo(TEST_TOTAL_POSTS)
        }

    @Test
    fun `when state is loaded, then getDetailData returns years`() =
        test {
            whenever(statsInsightsUseCase(any(), any()))
                .thenReturn(createSuccessResult())

            initViewModel()
            advanceUntilIdle()

            val detailData = viewModel.getDetailData()
            assertThat(detailData).hasSize(3)
            assertThat(detailData[0].year)
                .isEqualTo(CURRENT_YEAR)
            assertThat(detailData[1].year)
                .isEqualTo("2025")
            assertThat(detailData[1].totalPosts)
                .isEqualTo(TEST_TOTAL_POSTS)
            assertThat(detailData[2].year)
                .isEqualTo("2024")
        }

    @Test
    fun `when state is loading, then getDetailData returns empty list`() =
        test {
            viewModel = YearInReviewViewModel(
                selectedSiteRepository,
                statsInsightsUseCase,
                resourceProvider
            )

            val detailData = viewModel.getDetailData()
            assertThat(detailData).isEmpty()
        }

    @Test
    fun `when state is error, then getDetailData returns empty list`() =
        test {
            whenever(
                selectedSiteRepository.getSelectedSite()
            ).thenReturn(null)

            initViewModel()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    YearInReviewCardUiState
                        .Error::class.java
                )
            val detailData = viewModel.getDetailData()
            assertThat(detailData).isEmpty()
        }

    @Test
    fun `when refresh fails after success, then loadDataIfNeeded reloads`() =
        test {
            whenever(statsInsightsUseCase(any(), any()))
                .thenReturn(createSuccessResult())

            viewModel = YearInReviewViewModel(
                selectedSiteRepository,
                statsInsightsUseCase,
                resourceProvider
            )
            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    YearInReviewCardUiState
                        .Loaded::class.java
                )

            whenever(statsInsightsUseCase(any(), any()))
                .thenReturn(
                    InsightsResult.Error("Network error")
                )
            viewModel.refresh()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    YearInReviewCardUiState
                        .Error::class.java
                )

            whenever(statsInsightsUseCase(any(), any()))
                .thenReturn(createSuccessResult())
            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    YearInReviewCardUiState
                        .Loaded::class.java
                )
            verify(statsInsightsUseCase, times(3))
                .invoke(eq(TEST_SITE_ID), any())
        }

    private fun createSuccessResult() =
        InsightsResult.Success(
            data = createTestInsightsData(
                createTestYears()
            )
        )

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
            totalPosts = TEST_TOTAL_POSTS,
            totalWords = TEST_TOTAL_WORDS,
            avgWords = TEST_AVG_WORDS,
            totalLikes = TEST_TOTAL_LIKES,
            avgLikes = TEST_AVG_LIKES,
            totalComments = TEST_TOTAL_COMMENTS,
            avgComments = TEST_AVG_COMMENTS
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
        private const val TEST_TOTAL_POSTS = 42L
        private const val TEST_TOTAL_WORDS = 15000L
        private const val TEST_AVG_WORDS = 357.1
        private const val TEST_TOTAL_LIKES = 230L
        private const val TEST_AVG_LIKES = 5.5
        private const val TEST_TOTAL_COMMENTS = 85L
        private const val TEST_AVG_COMMENTS = 2.0
        private const val NO_SITE_SELECTED_ERROR =
            "No site selected"
        private const val FAILED_TO_LOAD_ERROR =
            "Failed to load stats"
        private const val UNKNOWN_ERROR =
            "Unknown error"
        private val CURRENT_YEAR =
            Year.now().toString()
    }
}
