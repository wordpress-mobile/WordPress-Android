package org.wordpress.android.ui.newstats.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.newstats.datasource.StatsInsightsData
import org.wordpress.android.ui.newstats.datasource.YearInsightsData

@ExperimentalCoroutinesApi
class StatsInsightsUseCaseTest : BaseUnitTest() {
    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var accountStore: AccountStore

    private lateinit var useCase: StatsInsightsUseCase

    @Before
    fun setUp() {
        whenever(accountStore.accessToken)
            .thenReturn(TEST_ACCESS_TOKEN)
        useCase = StatsInsightsUseCase(
            statsRepository,
            accountStore
        )
    }

    @Test
    fun `when called, then returns cached on second call`() =
        test {
            whenever(
                statsRepository.fetchInsights(
                    TEST_SITE_ID
                )
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )

            val first = useCase(TEST_SITE_ID)
            val second = useCase(TEST_SITE_ID)

            assertThat(first).isInstanceOf(
                InsightsResult.Success::class.java
            )
            assertThat(second).isInstanceOf(
                InsightsResult.Success::class.java
            )
            verify(statsRepository, times(1))
                .fetchInsights(TEST_SITE_ID)
        }

    @Test
    fun `when called with forceRefresh, then fetches again`() =
        test {
            whenever(
                statsRepository.fetchInsights(
                    TEST_SITE_ID
                )
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )

            useCase(TEST_SITE_ID)
            useCase(
                TEST_SITE_ID,
                forceRefresh = true
            )

            verify(statsRepository, times(2))
                .fetchInsights(TEST_SITE_ID)
        }

    @Test
    fun `when called without token, then returns error`() =
        test {
            whenever(accountStore.accessToken)
                .thenReturn(null)
            useCase = StatsInsightsUseCase(
                statsRepository,
                accountStore
            )

            val result = useCase(TEST_SITE_ID)

            assertThat(result).isInstanceOf(
                InsightsResult.Error::class.java
            )
            verify(statsRepository, never())
                .fetchInsights(any())
        }

    @Test
    fun `when called with empty token, then returns error`() =
        test {
            whenever(accountStore.accessToken)
                .thenReturn("")
            useCase = StatsInsightsUseCase(
                statsRepository,
                accountStore
            )

            val result = useCase(TEST_SITE_ID)

            assertThat(result).isInstanceOf(
                InsightsResult.Error::class.java
            )
            verify(statsRepository, never())
                .fetchInsights(any())
        }

    @Test
    fun `when errors, then cache is not populated`() =
        test {
            whenever(
                statsRepository.fetchInsights(
                    TEST_SITE_ID
                )
            ).thenReturn(
                InsightsResult.Error("Network error")
            )

            val first = useCase(TEST_SITE_ID)
            assertThat(first).isInstanceOf(
                InsightsResult.Error::class.java
            )

            whenever(
                statsRepository.fetchInsights(
                    TEST_SITE_ID
                )
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )

            val second = useCase(TEST_SITE_ID)
            assertThat(second).isInstanceOf(
                InsightsResult.Success::class.java
            )
            verify(statsRepository, times(2))
                .fetchInsights(TEST_SITE_ID)
        }

    @Test
    fun `when called for different site, then fetches again`() =
        test {
            whenever(
                statsRepository.fetchInsights(any())
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )

            useCase(TEST_SITE_ID)
            useCase(OTHER_SITE_ID)

            verify(statsRepository, times(1))
                .fetchInsights(TEST_SITE_ID)
            verify(statsRepository, times(1))
                .fetchInsights(OTHER_SITE_ID)
        }

    @Test
    fun `when success, then data is returned correctly`() =
        test {
            val testData = createTestInsightsData()
            whenever(
                statsRepository.fetchInsights(
                    TEST_SITE_ID
                )
            ).thenReturn(
                InsightsResult.Success(testData)
            )

            val result = useCase(TEST_SITE_ID)

            assertThat(result).isInstanceOf(
                InsightsResult.Success::class.java
            )
            val success =
                result as InsightsResult.Success
            assertThat(success.data.highestHour)
                .isEqualTo(TEST_HIGHEST_HOUR)
            assertThat(success.data.highestDayOfWeek)
                .isEqualTo(TEST_DAY_OF_WEEK)
            assertThat(success.data.years).hasSize(1)
        }

    @Test
    fun `when clearCache called, then next call fetches again`() =
        test {
            whenever(
                statsRepository.fetchInsights(
                    TEST_SITE_ID
                )
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )

            useCase(TEST_SITE_ID)
            useCase.clearCache()
            useCase(TEST_SITE_ID)

            verify(statsRepository, times(2))
                .fetchInsights(TEST_SITE_ID)
        }

    private fun createTestInsightsData() =
        StatsInsightsData(
            highestHour = TEST_HIGHEST_HOUR,
            highestHourPercent = 15.5,
            highestDayOfWeek = TEST_DAY_OF_WEEK,
            highestDayPercent = 25.0,
            years = listOf(
                YearInsightsData(
                    year = "2025",
                    totalPosts = 42L,
                    totalWords = 15000L,
                    avgWords = 357.1,
                    totalLikes = 230L,
                    avgLikes = 5.5,
                    totalComments = 85L,
                    avgComments = 2.0
                )
            )
        )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val OTHER_SITE_ID = 456L
        private const val TEST_ACCESS_TOKEN =
            "test_access_token"
        private const val TEST_HIGHEST_HOUR = 16
        private const val TEST_DAY_OF_WEEK = 3
    }
}
