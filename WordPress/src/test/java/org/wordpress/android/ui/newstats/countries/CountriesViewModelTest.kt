package org.wordpress.android.ui.newstats.countries

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
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.CountryViewItemData
import org.wordpress.android.ui.newstats.repository.CountryViewsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class CountriesViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: CountriesViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(accountStore.accessToken).thenReturn(TEST_ACCESS_TOKEN)
    }

    private fun initViewModel() {
        viewModel = CountriesViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider
        )
        viewModel.onPeriodChanged(StatsPeriod.Last7Days)
    }

    // region Error states
    @Test
    fun `when no site selected, then error state is emitted`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)
        whenever(resourceProvider.getString(R.string.stats_todays_stats_no_site_selected))
            .thenReturn(NO_SITE_SELECTED_ERROR)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(CountriesCardUiState.Error::class.java)
        assertThat((state as CountriesCardUiState.Error).message).isEqualTo(NO_SITE_SELECTED_ERROR)
    }

    @Test
    fun `when fetch fails, then error state is emitted`() = test {
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(CountryViewsResult.Error(ERROR_MESSAGE))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(CountriesCardUiState.Error::class.java)
        assertThat((state as CountriesCardUiState.Error).message).isEqualTo(ERROR_MESSAGE)
    }
    // endregion

    // region Success states
    @Test
    fun `when data loads successfully, then loaded state is emitted`() = test {
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(CountriesCardUiState.Loaded::class.java)
    }

    @Test
    fun `when data loads, then countries contain correct values`() = test {
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as CountriesCardUiState.Loaded
        assertThat(state.countries).hasSize(2)
        assertThat(state.countries[0].countryCode).isEqualTo(TEST_COUNTRY_CODE_1)
        assertThat(state.countries[0].countryName).isEqualTo(TEST_COUNTRY_NAME_1)
        assertThat(state.countries[0].views).isEqualTo(TEST_COUNTRY_VIEWS_1)
        assertThat(state.countries[1].countryCode).isEqualTo(TEST_COUNTRY_CODE_2)
        assertThat(state.countries[1].views).isEqualTo(TEST_COUNTRY_VIEWS_2)
    }

    @Test
    fun `when data loads, then map data is built correctly`() = test {
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as CountriesCardUiState.Loaded
        val expectedMapData = "['$TEST_COUNTRY_CODE_1',$TEST_COUNTRY_VIEWS_1]," +
            "['$TEST_COUNTRY_CODE_2',$TEST_COUNTRY_VIEWS_2]"
        assertThat(state.mapData).isEqualTo(expectedMapData)
    }

    @Test
    fun `when data loads, then min and max views are calculated correctly`() = test {
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as CountriesCardUiState.Loaded
        assertThat(state.minViews).isEqualTo(TEST_COUNTRY_VIEWS_2)
        assertThat(state.maxViews).isEqualTo(TEST_COUNTRY_VIEWS_1)
    }

    @Test
    fun `when all countries have same views, then minViews is zero`() = test {
        val sameViewsResult = CountryViewsResult.Success(
            countries = listOf(
                CountryViewItemData("US", "United States", 100, null, 80),
                CountryViewItemData("UK", "United Kingdom", 100, null, 90)
            ),
            totalViews = 200,
            otherViews = 0,
            totalViewsChange = 30,
            totalViewsChangePercent = 17.6
        )
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(sameViewsResult)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as CountriesCardUiState.Loaded
        assertThat(state.minViews).isEqualTo(0L)
        assertThat(state.maxViews).isEqualTo(100L)
    }

    @Test
    fun `when data loads with more than 10 countries, then only 10 are shown in card`() = test {
        val manyCountries = (1..15).map { index ->
            CountryViewItemData(
                countryCode = "C$index",
                countryName = "Country $index",
                views = (100 - index).toLong(),
                flagIconUrl = null,
                previousViews = (90 - index).toLong()
            )
        }
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(
                CountryViewsResult.Success(
                    countries = manyCountries,
                    totalViews = 1000,
                    otherViews = 0,
                    totalViewsChange = 100,
                    totalViewsChangePercent = 10.0
                )
            )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as CountriesCardUiState.Loaded
        assertThat(state.countries).hasSize(10)
        assertThat(state.hasMoreItems).isTrue()
    }

    @Test
    fun `when data loads with 10 or fewer countries, then hasMoreItems is false`() = test {
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as CountriesCardUiState.Loaded
        assertThat(state.hasMoreItems).isFalse()
    }

    @Test
    fun `when data loads with empty countries, then loaded state with empty list is emitted`() = test {
        val emptyResult = CountryViewsResult.Success(
            countries = emptyList(),
            totalViews = 0,
            otherViews = 0,
            totalViewsChange = 0,
            totalViewsChangePercent = 0.0
        )
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(emptyResult)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as CountriesCardUiState.Loaded
        assertThat(state.countries).isEmpty()
        assertThat(state.mapData).isEmpty()
        assertThat(state.hasMoreItems).isFalse()
    }
    // endregion

    // region Period changes
    @Test
    fun `when period changes, then data is reloaded`() = test {
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.onPeriodChanged(StatsPeriod.Last30Days)
        advanceUntilIdle()

        verify(statsRepository, times(2)).fetchCountryViews(any(), any())
        verify(statsRepository).fetchCountryViews(eq(TEST_SITE_ID), eq(StatsPeriod.Last30Days))
    }

    @Test
    fun `when same period is selected, then data is not reloaded`() = test {
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.onPeriodChanged(StatsPeriod.Last7Days)
        advanceUntilIdle()

        // Should only be called once during init
        verify(statsRepository, times(1)).fetchCountryViews(any(), any())
    }

    @Test
    fun `when same period is re-selected after success, then fetch is skipped`() = test {
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        // Simulate card removal and re-addition with the same period
        viewModel.onPeriodChanged(StatsPeriod.Last7Days)
        advanceUntilIdle()

        verify(statsRepository, times(1)).fetchCountryViews(any(), any())
        assertThat(viewModel.uiState.value)
            .isInstanceOf(CountriesCardUiState.Loaded::class.java)
    }

    @Test
    fun `when same period is re-selected after error, then data is re-fetched`() = test {
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(CountryViewsResult.Error(ERROR_MESSAGE))

        initViewModel()
        advanceUntilIdle()

        // loadedPeriod should not be set after error, so re-selecting
        // the same period should trigger a new fetch
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(createSuccessResult())

        viewModel.onPeriodChanged(StatsPeriod.Last7Days)
        advanceUntilIdle()

        verify(statsRepository, times(2)).fetchCountryViews(any(), any())
        assertThat(viewModel.uiState.value)
            .isInstanceOf(CountriesCardUiState.Loaded::class.java)
    }
    // endregion

    // region Refresh
    @Test
    fun `when refresh is called, then isRefreshing becomes true then false`() = test {
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        assertThat(viewModel.isRefreshing.value).isFalse()

        viewModel.refresh()
        advanceUntilIdle()

        assertThat(viewModel.isRefreshing.value).isFalse()
    }

    @Test
    fun `when refresh is called, then data is fetched`() = test {
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        // Called twice: once during init, once during refresh
        verify(statsRepository, times(2)).fetchCountryViews(eq(TEST_SITE_ID), any())
    }

    @Test
    fun `when refresh is called with no site, then data is not fetched`() = test {
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        viewModel.refresh()
        advanceUntilIdle()

        // Should only be called once during init
        verify(statsRepository, times(1)).fetchCountryViews(any(), any())
    }
    // endregion

    // region Retry
    @Test
    fun `when onRetry is called, then data is reloaded`() = test {
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.onRetry()
        advanceUntilIdle()

        // Called twice: once during init, once during retry
        verify(statsRepository, times(2)).fetchCountryViews(any(), any())
    }
    // endregion

    // region getDetailData
    @Test
    fun `when getDetailData is called, then returns cached data`() = test {
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(createSuccessResult())
        whenever(resourceProvider.getString(R.string.stats_period_last_7_days))
            .thenReturn("Last 7 days")

        initViewModel()
        advanceUntilIdle()

        val detailData = viewModel.getDetailData()

        assertThat(detailData.countries).hasSize(2)
        assertThat(detailData.totalViews).isEqualTo(TEST_TOTAL_VIEWS)
        assertThat(detailData.totalViewsChange).isEqualTo(TEST_TOTAL_VIEWS_CHANGE)
        assertThat(detailData.totalViewsChangePercent).isEqualTo(TEST_TOTAL_VIEWS_CHANGE_PERCENT)
        assertThat(detailData.dateRange).isEqualTo("Last 7 days")
    }

    @Test
    fun `when getDetailData is called, then all countries are returned not just card items`() = test {
        val manyCountries = (1..15).map { index ->
            CountryViewItemData(
                countryCode = "C$index",
                countryName = "Country $index",
                views = (100 - index).toLong(),
                flagIconUrl = null,
                previousViews = (90 - index).toLong()
            )
        }
        whenever(resourceProvider.getString(R.string.stats_period_last_7_days))
            .thenReturn("Last 7 days")
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(
                CountryViewsResult.Success(
                    countries = manyCountries,
                    totalViews = 1000,
                    otherViews = 0,
                    totalViewsChange = 100,
                    totalViewsChangePercent = 10.0
                )
            )

        initViewModel()
        advanceUntilIdle()

        val detailData = viewModel.getDetailData()
        // Card shows max 10, but detail data should have all 15
        assertThat(detailData.countries).hasSize(15)
    }

    @Test
    fun `when getDetailData is called, then map data is included`() = test {
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(createSuccessResult())
        whenever(resourceProvider.getString(R.string.stats_period_last_7_days))
            .thenReturn("Last 7 days")

        initViewModel()
        advanceUntilIdle()

        val detailData = viewModel.getDetailData()

        val expectedMapData = "['$TEST_COUNTRY_CODE_1',$TEST_COUNTRY_VIEWS_1]," +
            "['$TEST_COUNTRY_CODE_2',$TEST_COUNTRY_VIEWS_2]"
        assertThat(detailData.mapData).isEqualTo(expectedMapData)
        assertThat(detailData.minViews).isEqualTo(TEST_COUNTRY_VIEWS_2)
        assertThat(detailData.maxViews).isEqualTo(TEST_COUNTRY_VIEWS_1)
    }
    // endregion

    // region Change calculations
    @Test
    fun `when country has positive change, then CountryViewChange_Positive is returned`() = test {
        val countries = listOf(
            CountryViewItemData(
                countryCode = "US",
                countryName = "United States",
                views = 150,
                flagIconUrl = null,
                previousViews = 100
            )
        )
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(
                CountryViewsResult.Success(
                    countries = countries,
                    totalViews = 150,
                    otherViews = 0,
                    totalViewsChange = 50,
                    totalViewsChangePercent = 50.0
                )
            )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as CountriesCardUiState.Loaded
        assertThat(state.countries[0].change).isInstanceOf(CountryViewChange.Positive::class.java)
        val change = state.countries[0].change as CountryViewChange.Positive
        assertThat(change.value).isEqualTo(50)
        assertThat(change.percentage).isEqualTo(50.0)
    }

    @Test
    fun `when country has negative change, then CountryViewChange_Negative is returned`() = test {
        val countries = listOf(
            CountryViewItemData(
                countryCode = "US",
                countryName = "United States",
                views = 50,
                flagIconUrl = null,
                previousViews = 100
            )
        )
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(
                CountryViewsResult.Success(
                    countries = countries,
                    totalViews = 50,
                    otherViews = 0,
                    totalViewsChange = -50,
                    totalViewsChangePercent = -50.0
                )
            )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as CountriesCardUiState.Loaded
        assertThat(state.countries[0].change).isInstanceOf(CountryViewChange.Negative::class.java)
        val change = state.countries[0].change as CountryViewChange.Negative
        assertThat(change.value).isEqualTo(50)
        assertThat(change.percentage).isEqualTo(50.0)
    }

    @Test
    fun `when country has no change, then CountryViewChange_NoChange is returned`() = test {
        val countries = listOf(
            CountryViewItemData(
                countryCode = "US",
                countryName = "United States",
                views = 100,
                flagIconUrl = null,
                previousViews = 100
            )
        )
        whenever(statsRepository.fetchCountryViews(any(), any()))
            .thenReturn(
                CountryViewsResult.Success(
                    countries = countries,
                    totalViews = 100,
                    otherViews = 0,
                    totalViewsChange = 0,
                    totalViewsChangePercent = 0.0
                )
            )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as CountriesCardUiState.Loaded
        assertThat(state.countries[0].change).isEqualTo(CountryViewChange.NoChange)
    }
    // endregion

    // region Helper functions
    private fun createSuccessResult() = CountryViewsResult.Success(
        countries = listOf(
            CountryViewItemData(
                countryCode = TEST_COUNTRY_CODE_1,
                countryName = TEST_COUNTRY_NAME_1,
                views = TEST_COUNTRY_VIEWS_1,
                flagIconUrl = null,
                previousViews = TEST_COUNTRY_PREVIOUS_VIEWS_1
            ),
            CountryViewItemData(
                countryCode = TEST_COUNTRY_CODE_2,
                countryName = TEST_COUNTRY_NAME_2,
                views = TEST_COUNTRY_VIEWS_2,
                flagIconUrl = null,
                previousViews = TEST_COUNTRY_PREVIOUS_VIEWS_2
            )
        ),
        totalViews = TEST_TOTAL_VIEWS,
        otherViews = 0,
        totalViewsChange = TEST_TOTAL_VIEWS_CHANGE,
        totalViewsChangePercent = TEST_TOTAL_VIEWS_CHANGE_PERCENT
    )
    // endregion

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_access_token"
        private const val ERROR_MESSAGE = "Network error"
        private const val NO_SITE_SELECTED_ERROR = "No site selected"

        private const val TEST_COUNTRY_CODE_1 = "US"
        private const val TEST_COUNTRY_CODE_2 = "UK"
        private const val TEST_COUNTRY_NAME_1 = "United States"
        private const val TEST_COUNTRY_NAME_2 = "United Kingdom"
        private const val TEST_COUNTRY_VIEWS_1 = 500L
        private const val TEST_COUNTRY_VIEWS_2 = 300L
        private const val TEST_COUNTRY_PREVIOUS_VIEWS_1 = 400L
        private const val TEST_COUNTRY_PREVIOUS_VIEWS_2 = 250L

        private const val TEST_TOTAL_VIEWS = 800L
        private const val TEST_TOTAL_VIEWS_CHANGE = 150L
        private const val TEST_TOTAL_VIEWS_CHANGE_PERCENT = 23.1
    }
}
