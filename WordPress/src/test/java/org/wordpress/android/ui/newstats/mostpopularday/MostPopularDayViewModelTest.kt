package org.wordpress.android.ui.newstats.mostpopularday

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.datasource.StatsSummaryData
import org.wordpress.android.ui.newstats.repository.StatsSummaryResult
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Locale

@ExperimentalCoroutinesApi
class MostPopularDayViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var resourceProvider:
        ResourceProvider

    private lateinit var viewModel:
        MostPopularDayViewModel

    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        lenient().`when`(
            resourceProvider.getString(
                R.string.stats_error_api
            )
        ).thenReturn(FAILED_TO_LOAD_ERROR)
        viewModel = MostPopularDayViewModel(
            resourceProvider
        )
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `initial state is Loading`() {
        assertThat(viewModel.uiState.value)
            .isInstanceOf(
                MostPopularDayCardUiState
                    .Loading::class.java
            )
    }

    @Test
    fun `when handleResult with success, then loaded state has correct day`() {
        viewModel.handleResult(
            StatsSummaryResult.Success(
                data = createTestData()
            )
        )

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
    fun `when handleResult with error, then error state`() {
        viewModel.handleResult(
            StatsSummaryResult.Error("Network error")
        )

        assertThat(viewModel.uiState.value)
            .isInstanceOf(
                MostPopularDayCardUiState
                    .Error::class.java
            )
    }

    @Test
    fun `when showLoading called, then loading state`() {
        viewModel.handleResult(
            StatsSummaryResult.Success(createTestData())
        )
        viewModel.showLoading()

        assertThat(viewModel.uiState.value)
            .isInstanceOf(
                MostPopularDayCardUiState
                    .Loading::class.java
            )
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
        private const val TEST_VIEWS = 6782856L
        private const val TEST_BEST_DAY = "2022-02-22"
        private const val TEST_BEST_DAY_TOTAL = 4600L
        private const val FAILED_TO_LOAD_ERROR =
            "Failed to load stats"

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
