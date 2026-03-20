package org.wordpress.android.ui.newstats.yearinreview

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.datasource.StatsInsightsData
import org.wordpress.android.ui.newstats.datasource.YearInsightsData
import org.wordpress.android.ui.newstats.repository.InsightsResult
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.Year

@ExperimentalCoroutinesApi
class YearInReviewViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var resourceProvider:
        ResourceProvider

    private lateinit var viewModel: YearInReviewViewModel

    @Before
    fun setUp() {
        whenever(
            resourceProvider.getString(
                R.string.stats_error_api
            )
        ).thenReturn(FAILED_TO_LOAD_ERROR)
        viewModel = YearInReviewViewModel(
            resourceProvider
        )
    }

    @Test
    fun `initial state is Loading`() {
        assertThat(viewModel.uiState.value)
            .isInstanceOf(
                YearInReviewCardUiState
                    .Loading::class.java
            )
    }

    @Test
    fun `when handleResult with success, then loaded state is emitted`() {
        viewModel.handleResult(
            createSuccessResult(),
        )

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
    fun `when handleResult with error, then error state is emitted`() {
        viewModel.handleResult(
            InsightsResult.Error("Network error"),
        )

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
    fun `when showLoading called, then loading state`() {
        viewModel.handleResult(
            createSuccessResult(),
        )
        viewModel.showLoading()

        assertThat(viewModel.uiState.value)
            .isInstanceOf(
                YearInReviewCardUiState
                    .Loading::class.java
            )
    }

    @Test
    fun `when data loads with empty years, then current year is added`() {
        viewModel.handleResult(
            InsightsResult.Success(
                data = createTestInsightsData(
                    emptyList()
                )
            ),
        )

        val state = viewModel.uiState.value
            as YearInReviewCardUiState.Loaded
        assertThat(state.years).hasSize(1)
        assertThat(state.years[0].year)
            .isEqualTo(CURRENT_YEAR)
        assertThat(state.years[0].totalPosts)
            .isEqualTo(0L)
    }

    @Test
    fun `when current year exists in data, then no duplicate is added`() {
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
        viewModel.handleResult(
            InsightsResult.Success(
                data = createTestInsightsData(
                    yearsWithCurrent
                )
            ),
        )

        val state = viewModel.uiState.value
            as YearInReviewCardUiState.Loaded
        assertThat(state.years).hasSize(1)
        assertThat(state.years[0].year)
            .isEqualTo(CURRENT_YEAR)
        assertThat(state.years[0].totalPosts)
            .isEqualTo(TEST_TOTAL_POSTS)
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
        private const val TEST_TOTAL_POSTS = 42L
        private const val TEST_TOTAL_WORDS = 15000L
        private const val TEST_AVG_WORDS = 357.1
        private const val TEST_TOTAL_LIKES = 230L
        private const val TEST_AVG_LIKES = 5.5
        private const val TEST_TOTAL_COMMENTS = 85L
        private const val TEST_AVG_COMMENTS = 2.0
        private const val FAILED_TO_LOAD_ERROR =
            "Failed to load stats"
        private val CURRENT_YEAR =
            Year.now().toString()
    }
}
