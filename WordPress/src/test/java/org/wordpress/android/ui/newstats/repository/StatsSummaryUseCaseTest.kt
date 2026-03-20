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
import org.wordpress.android.ui.newstats.datasource.StatsSummaryData

@ExperimentalCoroutinesApi
class StatsSummaryUseCaseTest : BaseUnitTest() {
    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var accountStore: AccountStore

    private lateinit var useCase: StatsSummaryUseCase

    @Before
    fun setUp() {
        whenever(accountStore.accessToken)
            .thenReturn(TEST_ACCESS_TOKEN)
        useCase = StatsSummaryUseCase(
            statsRepository,
            accountStore
        )
    }

    @Test
    fun `when called, then returns cached on second call`() =
        test {
            whenever(
                statsRepository.fetchStatsSummary(
                    TEST_SITE_ID
                )
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestSummary()
                )
            )

            val first = useCase(TEST_SITE_ID)
            val second = useCase(TEST_SITE_ID)

            assertThat(first).isInstanceOf(
                StatsSummaryResult.Success::class.java
            )
            assertThat(second).isInstanceOf(
                StatsSummaryResult.Success::class.java
            )
            verify(statsRepository, times(1))
                .fetchStatsSummary(TEST_SITE_ID)
        }

    @Test
    fun `when called with forceRefresh, then fetches again`() =
        test {
            whenever(
                statsRepository.fetchStatsSummary(
                    TEST_SITE_ID
                )
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestSummary()
                )
            )

            useCase(TEST_SITE_ID)
            useCase(
                TEST_SITE_ID,
                forceRefresh = true
            )

            verify(statsRepository, times(2))
                .fetchStatsSummary(TEST_SITE_ID)
        }

    @Test
    fun `when called without token, then returns error`() =
        test {
            whenever(accountStore.accessToken)
                .thenReturn(null)
            useCase = StatsSummaryUseCase(
                statsRepository,
                accountStore
            )

            val result = useCase(TEST_SITE_ID)

            assertThat(result).isInstanceOf(
                StatsSummaryResult.Error::class.java
            )
            verify(statsRepository, never())
                .fetchStatsSummary(any())
        }

    @Test
    fun `when errors, then cache is not populated`() =
        test {
            whenever(
                statsRepository.fetchStatsSummary(
                    TEST_SITE_ID
                )
            ).thenReturn(
                StatsSummaryResult.Error("Network error")
            )

            val first = useCase(TEST_SITE_ID)
            assertThat(first).isInstanceOf(
                StatsSummaryResult.Error::class.java
            )

            whenever(
                statsRepository.fetchStatsSummary(
                    TEST_SITE_ID
                )
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestSummary()
                )
            )

            val second = useCase(TEST_SITE_ID)
            assertThat(second).isInstanceOf(
                StatsSummaryResult.Success::class.java
            )
            verify(statsRepository, times(2))
                .fetchStatsSummary(TEST_SITE_ID)
        }

    @Test
    fun `when clearCache called, then next call fetches again`() =
        test {
            whenever(
                statsRepository.fetchStatsSummary(
                    TEST_SITE_ID
                )
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestSummary()
                )
            )

            useCase(TEST_SITE_ID)
            useCase.clearCache()
            useCase(TEST_SITE_ID)

            verify(statsRepository, times(2))
                .fetchStatsSummary(TEST_SITE_ID)
        }

    private fun createTestSummary() = StatsSummaryData(
        views = 100L,
        visitors = 50L,
        posts = 10L,
        comments = 5L,
        viewsBestDay = "2022-02-22",
        viewsBestDayTotal = 20L
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN =
            "test_access_token"
    }
}
