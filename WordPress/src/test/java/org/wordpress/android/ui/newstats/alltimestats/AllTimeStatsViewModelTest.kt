package org.wordpress.android.ui.newstats.alltimestats

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.datasource.StatsSummaryData
import org.wordpress.android.ui.newstats.repository.StatsSummaryResult
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class AllTimeStatsViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: AllTimeStatsViewModel

    @Before
    fun setUp() {
        whenever(
            resourceProvider.getString(
                R.string.stats_error_api
            )
        ).thenReturn(FAILED_TO_LOAD_ERROR)
        viewModel = AllTimeStatsViewModel(
            resourceProvider
        )
    }

    @Test
    fun `initial state is Loading`() {
        assertThat(viewModel.uiState.value)
            .isInstanceOf(
                AllTimeStatsCardUiState
                    .Loading::class.java
            )
    }

    @Test
    fun `when handleResult with success, then loaded state`() {
        viewModel.handleResult(
            StatsSummaryResult.Success(createTestData())
        )

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
    fun `when handleResult with error, then error state`() {
        viewModel.handleResult(
            StatsSummaryResult.Error("Network error")
        )

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
    fun `when showLoading called, then loading state`() {
        viewModel.handleResult(
            StatsSummaryResult.Success(createTestData())
        )
        assertThat(viewModel.uiState.value)
            .isInstanceOf(
                AllTimeStatsCardUiState
                    .Loaded::class.java
            )

        viewModel.showLoading()

        assertThat(viewModel.uiState.value)
            .isInstanceOf(
                AllTimeStatsCardUiState
                    .Loading::class.java
            )
    }

    @Test
    fun `when handleResult after error, then loaded state`() {
        viewModel.handleResult(
            StatsSummaryResult.Error("error")
        )
        assertThat(viewModel.uiState.value)
            .isInstanceOf(
                AllTimeStatsCardUiState
                    .Error::class.java
            )

        viewModel.handleResult(
            StatsSummaryResult.Success(createTestData())
        )
        assertThat(viewModel.uiState.value)
            .isInstanceOf(
                AllTimeStatsCardUiState
                    .Loaded::class.java
            )
    }

    companion object {
        private const val TEST_VIEWS = 6782856L
        private const val TEST_VISITORS = 154791L
        private const val TEST_POSTS = 42L
        private const val TEST_COMMENTS = 85L
        private const val FAILED_TO_LOAD_ERROR =
            "Failed to load stats"

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
