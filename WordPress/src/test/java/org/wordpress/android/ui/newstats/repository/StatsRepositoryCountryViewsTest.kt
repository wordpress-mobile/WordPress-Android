package org.wordpress.android.ui.newstats.repository

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
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.datasource.CountryViewItem
import org.wordpress.android.ui.newstats.datasource.CountryViewsData
import org.wordpress.android.ui.newstats.datasource.CountryViewsDataResult
import org.wordpress.android.ui.newstats.datasource.StatsDataSource

@ExperimentalCoroutinesApi
class StatsRepositoryCountryViewsTest : BaseUnitTest() {
    @Mock
    private lateinit var statsDataSource: StatsDataSource

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    private lateinit var repository: StatsRepository

    @Before
    fun setUp() {
        repository = StatsRepository(
            statsDataSource = statsDataSource,
            appLogWrapper = appLogWrapper,
            ioDispatcher = testDispatcher()
        )
    }

    @Test
    fun `given successful response, when fetchCountryViews, then success result is returned`() = test {
        whenever(statsDataSource.fetchCountryViews(any(), any(), any()))
            .thenReturn(CountryViewsDataResult.Success(createCountryViewsData()))

        val result = repository.fetchCountryViews(TEST_SITE_ID, StatsPeriod.Last7Days)

        assertThat(result).isInstanceOf(CountryViewsResult.Success::class.java)
        val success = result as CountryViewsResult.Success
        assertThat(success.countries).hasSize(2)
        assertThat(success.countries[0].countryCode).isEqualTo(TEST_COUNTRY_CODE_1)
        assertThat(success.countries[0].countryName).isEqualTo(TEST_COUNTRY_NAME_1)
        assertThat(success.countries[0].views).isEqualTo(TEST_COUNTRY_VIEWS_1)
        assertThat(success.countries[1].countryCode).isEqualTo(TEST_COUNTRY_CODE_2)
        assertThat(success.countries[1].views).isEqualTo(TEST_COUNTRY_VIEWS_2)
    }

    @Test
    fun `given successful response, when fetchCountryViews, then totalViews is sum of country views`() = test {
        whenever(statsDataSource.fetchCountryViews(any(), any(), any()))
            .thenReturn(CountryViewsDataResult.Success(createCountryViewsData()))

        val result = repository.fetchCountryViews(TEST_SITE_ID, StatsPeriod.Last7Days)

        assertThat(result).isInstanceOf(CountryViewsResult.Success::class.java)
        val success = result as CountryViewsResult.Success
        // totalViews should be calculated from countries, not from API's summary field
        assertThat(success.totalViews).isEqualTo(TEST_COUNTRY_VIEWS_1 + TEST_COUNTRY_VIEWS_2)
    }

    @Test
    fun `given API totalViews is zero, when fetchCountryViews, then totalViews is sum of country views`() = test {
        // Simulate API returning 0 for totalViews but having country data
        val countryViewsData = CountryViewsData(
            countries = listOf(
                CountryViewItem(TEST_COUNTRY_CODE_1, TEST_COUNTRY_NAME_1, TEST_COUNTRY_VIEWS_1, null),
                CountryViewItem(TEST_COUNTRY_CODE_2, TEST_COUNTRY_NAME_2, TEST_COUNTRY_VIEWS_2, null)
            ),
            totalViews = 0L,  // API returns 0
            otherViews = 0L
        )
        whenever(statsDataSource.fetchCountryViews(any(), any(), any()))
            .thenReturn(CountryViewsDataResult.Success(countryViewsData))

        val result = repository.fetchCountryViews(TEST_SITE_ID, StatsPeriod.Last7Days)

        assertThat(result).isInstanceOf(CountryViewsResult.Success::class.java)
        val success = result as CountryViewsResult.Success
        // totalViews should still be calculated from country views
        assertThat(success.totalViews).isEqualTo(TEST_COUNTRY_VIEWS_1 + TEST_COUNTRY_VIEWS_2)
    }

    @Test
    fun `given current and previous data, when fetchCountryViews, then change is calculated correctly`() = test {
        val currentData = CountryViewsData(
            countries = listOf(
                CountryViewItem("US", "United States", 150, null),
                CountryViewItem("UK", "United Kingdom", 100, null)
            ),
            totalViews = 250L,
            otherViews = 0L
        )
        val previousData = CountryViewsData(
            countries = listOf(
                CountryViewItem("US", "United States", 100, null),
                CountryViewItem("UK", "United Kingdom", 100, null)
            ),
            totalViews = 200L,
            otherViews = 0L
        )

        whenever(statsDataSource.fetchCountryViews(any(), any(), any()))
            .thenReturn(CountryViewsDataResult.Success(currentData))
            .thenReturn(CountryViewsDataResult.Success(previousData))

        val result = repository.fetchCountryViews(TEST_SITE_ID, StatsPeriod.Last7Days)

        assertThat(result).isInstanceOf(CountryViewsResult.Success::class.java)
        val success = result as CountryViewsResult.Success
        // Current total: 250, Previous total: 200, Change: 50
        assertThat(success.totalViews).isEqualTo(250)
        assertThat(success.totalViewsChange).isEqualTo(50)
        assertThat(success.totalViewsChangePercent).isEqualTo(25.0)
    }

    @Test
    fun `given country in both periods, when fetchCountryViews, then previousViews is set correctly`() = test {
        val currentData = CountryViewsData(
            countries = listOf(CountryViewItem("US", "United States", 150, null)),
            totalViews = 150L,
            otherViews = 0L
        )
        val previousData = CountryViewsData(
            countries = listOf(CountryViewItem("US", "United States", 100, null)),
            totalViews = 100L,
            otherViews = 0L
        )

        whenever(statsDataSource.fetchCountryViews(any(), any(), any()))
            .thenReturn(CountryViewsDataResult.Success(currentData))
            .thenReturn(CountryViewsDataResult.Success(previousData))

        val result = repository.fetchCountryViews(TEST_SITE_ID, StatsPeriod.Last7Days)

        assertThat(result).isInstanceOf(CountryViewsResult.Success::class.java)
        val success = result as CountryViewsResult.Success
        assertThat(success.countries[0].previousViews).isEqualTo(100)
        assertThat(success.countries[0].viewsChange).isEqualTo(50)
        assertThat(success.countries[0].viewsChangePercent).isEqualTo(50.0)
    }

    @Test
    fun `given new country not in previous period, when fetchCountryViews, then previousViews is zero`() = test {
        val currentData = CountryViewsData(
            countries = listOf(CountryViewItem("US", "United States", 100, null)),
            totalViews = 100L,
            otherViews = 0L
        )
        val previousData = CountryViewsData(
            countries = emptyList(),
            totalViews = 0L,
            otherViews = 0L
        )

        whenever(statsDataSource.fetchCountryViews(any(), any(), any()))
            .thenReturn(CountryViewsDataResult.Success(currentData))
            .thenReturn(CountryViewsDataResult.Success(previousData))

        val result = repository.fetchCountryViews(TEST_SITE_ID, StatsPeriod.Last7Days)

        assertThat(result).isInstanceOf(CountryViewsResult.Success::class.java)
        val success = result as CountryViewsResult.Success
        assertThat(success.countries[0].previousViews).isEqualTo(0)
        assertThat(success.countries[0].viewsChange).isEqualTo(100)
        assertThat(success.countries[0].viewsChangePercent).isEqualTo(100.0)
    }

    @Test
    fun `given previous fetch fails, when fetchCountryViews, then previousViews defaults to zero`() = test {
        val currentData = CountryViewsData(
            countries = listOf(CountryViewItem("US", "United States", 100, null)),
            totalViews = 100L,
            otherViews = 0L
        )

        whenever(statsDataSource.fetchCountryViews(any(), any(), any()))
            .thenReturn(CountryViewsDataResult.Success(currentData))
            .thenReturn(CountryViewsDataResult.Error("Network error"))

        val result = repository.fetchCountryViews(TEST_SITE_ID, StatsPeriod.Last7Days)

        assertThat(result).isInstanceOf(CountryViewsResult.Success::class.java)
        val success = result as CountryViewsResult.Success
        assertThat(success.countries[0].previousViews).isEqualTo(0)
        assertThat(success.totalViewsChange).isEqualTo(100)
        assertThat(success.totalViewsChangePercent).isEqualTo(100.0)
    }

    @Test
    fun `given error response, when fetchCountryViews, then error result is returned`() = test {
        whenever(statsDataSource.fetchCountryViews(any(), any(), any()))
            .thenReturn(CountryViewsDataResult.Error(ERROR_MESSAGE))

        val result = repository.fetchCountryViews(TEST_SITE_ID, StatsPeriod.Last7Days)

        assertThat(result).isInstanceOf(CountryViewsResult.Error::class.java)
        assertThat((result as CountryViewsResult.Error).message).isEqualTo(ERROR_MESSAGE)
    }

    @Test
    fun `when fetchCountryViews is called, then data source is called twice for comparison`() = test {
        whenever(statsDataSource.fetchCountryViews(any(), any(), any()))
            .thenReturn(CountryViewsDataResult.Success(createCountryViewsData()))

        repository.fetchCountryViews(TEST_SITE_ID, StatsPeriod.Last7Days)

        // Verify data source is called twice (current and previous period)
        verify(statsDataSource, times(2)).fetchCountryViews(
            siteId = eq(TEST_SITE_ID),
            dateRange = any(),
            max = any()
        )
    }

    private fun createCountryViewsData() = CountryViewsData(
        countries = listOf(
            CountryViewItem(
                countryCode = TEST_COUNTRY_CODE_1,
                countryName = TEST_COUNTRY_NAME_1,
                views = TEST_COUNTRY_VIEWS_1,
                flagIconUrl = null
            ),
            CountryViewItem(
                countryCode = TEST_COUNTRY_CODE_2,
                countryName = TEST_COUNTRY_NAME_2,
                views = TEST_COUNTRY_VIEWS_2,
                flagIconUrl = null
            )
        ),
        totalViews = TEST_COUNTRY_VIEWS_1 + TEST_COUNTRY_VIEWS_2,
        otherViews = 0L
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val ERROR_MESSAGE = "Test error message"

        private const val TEST_COUNTRY_CODE_1 = "US"
        private const val TEST_COUNTRY_CODE_2 = "UK"
        private const val TEST_COUNTRY_NAME_1 = "United States"
        private const val TEST_COUNTRY_NAME_2 = "United Kingdom"
        private const val TEST_COUNTRY_VIEWS_1 = 500L
        private const val TEST_COUNTRY_VIEWS_2 = 300L
    }
}
