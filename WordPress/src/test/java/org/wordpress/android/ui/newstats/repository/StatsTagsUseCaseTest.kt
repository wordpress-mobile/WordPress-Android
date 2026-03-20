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
import org.wordpress.android.ui.newstats.datasource.StatsTagsData

@ExperimentalCoroutinesApi
class StatsTagsUseCaseTest : BaseUnitTest() {
    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var accountStore: AccountStore

    private lateinit var useCase: StatsTagsUseCase

    @Before
    fun setUp() {
        whenever(accountStore.accessToken)
            .thenReturn(TEST_ACCESS_TOKEN)
        useCase = StatsTagsUseCase(
            statsRepository,
            accountStore
        )
    }

    @Test
    fun `when called, then returns cached on second call`() =
        test {
            whenever(
                statsRepository.fetchTags(
                    siteId = TEST_SITE_ID,
                    max = DEFAULT_MAX
                )
            ).thenReturn(
                TagsResult.Success(createTestTagsData())
            )

            val first = useCase(TEST_SITE_ID)
            val second = useCase(TEST_SITE_ID)

            assertThat(first).isInstanceOf(
                TagsResult.Success::class.java
            )
            assertThat(second).isInstanceOf(
                TagsResult.Success::class.java
            )
            verify(statsRepository, times(1))
                .fetchTags(
                    siteId = TEST_SITE_ID,
                    max = DEFAULT_MAX
                )
        }

    @Test
    fun `when called with forceRefresh, then fetches again`() =
        test {
            whenever(
                statsRepository.fetchTags(
                    siteId = TEST_SITE_ID,
                    max = DEFAULT_MAX
                )
            ).thenReturn(
                TagsResult.Success(createTestTagsData())
            )

            useCase(TEST_SITE_ID)
            useCase(
                TEST_SITE_ID,
                forceRefresh = true
            )

            verify(statsRepository, times(2))
                .fetchTags(
                    siteId = TEST_SITE_ID,
                    max = DEFAULT_MAX
                )
        }

    @Test
    fun `when called without token, then returns error`() =
        test {
            whenever(accountStore.accessToken)
                .thenReturn(null)
            useCase = StatsTagsUseCase(
                statsRepository,
                accountStore
            )

            val result = useCase(TEST_SITE_ID)

            assertThat(result).isInstanceOf(
                TagsResult.Error::class.java
            )
            verify(statsRepository, never())
                .fetchTags(
                    siteId = any(),
                    max = any()
                )
        }

    @Test
    fun `when clearCache called, then next call fetches again`() =
        test {
            whenever(
                statsRepository.fetchTags(
                    siteId = TEST_SITE_ID,
                    max = DEFAULT_MAX
                )
            ).thenReturn(
                TagsResult.Success(createTestTagsData())
            )

            useCase(TEST_SITE_ID)
            useCase.clearCache()
            useCase(TEST_SITE_ID)

            verify(statsRepository, times(2))
                .fetchTags(
                    siteId = TEST_SITE_ID,
                    max = DEFAULT_MAX
                )
        }

    @Test
    fun `when errors, then cache is not populated`() =
        test {
            whenever(
                statsRepository.fetchTags(
                    siteId = TEST_SITE_ID,
                    max = DEFAULT_MAX
                )
            ).thenReturn(
                TagsResult.Error("Network error")
            )

            val first = useCase(TEST_SITE_ID)
            assertThat(first).isInstanceOf(
                TagsResult.Error::class.java
            )

            whenever(
                statsRepository.fetchTags(
                    siteId = TEST_SITE_ID,
                    max = DEFAULT_MAX
                )
            ).thenReturn(
                TagsResult.Success(createTestTagsData())
            )

            val second = useCase(TEST_SITE_ID)
            assertThat(second).isInstanceOf(
                TagsResult.Success::class.java
            )
            verify(statsRepository, times(2))
                .fetchTags(
                    siteId = TEST_SITE_ID,
                    max = DEFAULT_MAX
                )
        }

    private fun createTestTagsData() = StatsTagsData(
        tagGroups = emptyList()
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN =
            "test_access_token"
        private const val DEFAULT_MAX = 10
    }
}
