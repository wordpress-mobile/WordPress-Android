package org.wordpress.android.ui.newstats.mostpopulartime

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.datasource.StatsInsightsData
import org.wordpress.android.ui.newstats.repository.InsightsResult
import org.wordpress.android.util.DateFormatWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class MostPopularTimeViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var resourceProvider:
        ResourceProvider

    @Mock
    private lateinit var dateFormatWrapper:
        DateFormatWrapper

    private lateinit var viewModel:
        MostPopularTimeViewModel

    @Before
    fun setUp() {
        lenient().`when`(
            resourceProvider.getString(
                R.string.stats_error_api
            )
        ).thenReturn(FAILED_TO_LOAD_ERROR)
        viewModel = MostPopularTimeViewModel(
            dateFormatWrapper, resourceProvider
        )
    }

    @Test
    fun `initial state is Loading`() {
        assertThat(viewModel.uiState.value)
            .isInstanceOf(
                MostPopularTimeCardUiState
                    .Loading::class.java
            )
    }

    @Test
    fun `when handleResult with error, then error state`() {
        viewModel.handleResult(
            InsightsResult.Error("Network error")
        )

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

    @Test
    fun `when showLoading called, then loading state`() {
        viewModel.handleResult(
            InsightsResult.Success(
                createTestInsightsData()
            )
        )
        viewModel.showLoading()

        assertThat(viewModel.uiState.value)
            .isInstanceOf(
                MostPopularTimeCardUiState
                    .Loading::class.java
            )
    }

    @Test
    fun `when handleResult after error, then loaded state`() {
        viewModel.handleResult(
            InsightsResult.Error("error")
        )
        assertThat(viewModel.uiState.value)
            .isInstanceOf(
                MostPopularTimeCardUiState
                    .Error::class.java
            )

        viewModel.handleResult(
            InsightsResult.Success(
                createTestInsightsData()
            )
        )
        assertThat(viewModel.uiState.value)
            .isInstanceOf(
                MostPopularTimeCardUiState
                    .Loaded::class.java
            )
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
        val state = MostPopularTimeViewModel
            .mapToUiState(data, USE_24H)
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
        val state = MostPopularTimeViewModel
            .mapToUiState(data, USE_24H)
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
        val state = MostPopularTimeViewModel
            .mapToUiState(data, USE_24H)
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
        val state = MostPopularTimeViewModel
            .mapToUiState(data, USE_24H)
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
        val state = MostPopularTimeViewModel
            .mapToUiState(data, USE_24H)
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
        val state = MostPopularTimeViewModel
            .mapToUiState(data, USE_24H)
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
        val state = MostPopularTimeViewModel
            .mapToUiState(data, USE_24H)
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
        val state = MostPopularTimeViewModel
            .mapToUiState(data, USE_24H)
            as MostPopularTimeCardUiState.Loaded
        assertThat(state.bestDay).isEmpty()
    }

    @Test
    fun `when mapToUiState with invalid hour, then empty hour`() {
        val data = StatsInsightsData(
            highestHour = 25,
            highestHourPercent = 10.0,
            highestDayOfWeek = 3,
            highestDayPercent = 20.0,
            years = emptyList()
        )
        val state = MostPopularTimeViewModel
            .mapToUiState(data, USE_24H)
            as MostPopularTimeCardUiState.Loaded
        assertThat(state.bestHour).isEmpty()
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
        val state = MostPopularTimeViewModel
            .mapToUiState(data, USE_24H)
            as MostPopularTimeCardUiState.Loaded
        assertThat(state.bestDayPercent)
            .isEqualTo("23")
        assertThat(state.bestHourPercent)
            .isEqualTo("11")
    }

    @Test
    fun `when 24h format, then hour shows 24h style`() {
        val data = StatsInsightsData(
            highestHour = 16,
            highestHourPercent = 10.0,
            highestDayOfWeek = 3,
            highestDayPercent = 20.0,
            years = emptyList()
        )
        val state = MostPopularTimeViewModel
            .mapToUiState(data, use24HourFormat = true)
            as MostPopularTimeCardUiState.Loaded
        assertThat(state.bestHour).isEqualTo("16:00")
    }

    @Test
    fun `when 12h format, then hour shows AM PM style`() {
        val data = StatsInsightsData(
            highestHour = 16,
            highestHourPercent = 10.0,
            highestDayOfWeek = 3,
            highestDayPercent = 20.0,
            years = emptyList()
        )
        val state = MostPopularTimeViewModel
            .mapToUiState(
                data, use24HourFormat = false
            )
            as MostPopularTimeCardUiState.Loaded
        assertThat(state.bestHour)
            .contains("4:00")
            .containsIgnoringCase("pm")
    }

    @Test
    fun `when 24h format with morning hour, then padded`() {
        val data = StatsInsightsData(
            highestHour = 9,
            highestHourPercent = 10.0,
            highestDayOfWeek = 3,
            highestDayPercent = 20.0,
            years = emptyList()
        )
        val state = MostPopularTimeViewModel
            .mapToUiState(data, use24HourFormat = true)
            as MostPopularTimeCardUiState.Loaded
        assertThat(state.bestHour).isEqualTo("09:00")
    }

    @Test
    fun `when 12h format with midnight, then 12 AM`() {
        val data = StatsInsightsData(
            highestHour = 0,
            highestHourPercent = 10.0,
            highestDayOfWeek = 3,
            highestDayPercent = 20.0,
            years = emptyList()
        )
        val state = MostPopularTimeViewModel
            .mapToUiState(
                data, use24HourFormat = false
            )
            as MostPopularTimeCardUiState.Loaded
        assertThat(state.bestHour)
            .contains("12:00")
            .containsIgnoringCase("am")
    }

    companion object {
        private const val TEST_HIGHEST_HOUR = 16
        private const val TEST_HOUR_PERCENT = 15.5
        private const val TEST_DAY_OF_WEEK = 3
        private const val TEST_DAY_PERCENT = 25.0
        private const val FAILED_TO_LOAD_ERROR =
            "Failed to load stats"
        private const val USE_24H = true

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
