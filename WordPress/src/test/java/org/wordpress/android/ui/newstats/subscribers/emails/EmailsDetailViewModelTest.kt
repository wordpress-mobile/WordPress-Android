package org.wordpress.android.ui.newstats.subscribers.emails

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.EmailItemData
import org.wordpress.android.ui.newstats.repository.EmailsStatsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner.Silent::class)
class EmailsDetailViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    private lateinit var viewModel: EmailsDetailViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite())
            .thenReturn(testSite)
        whenever(accountStore.accessToken)
            .thenReturn(TEST_ACCESS_TOKEN)
        viewModel = EmailsDetailViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository
        )
    }

    @Test
    fun `when loadInitialPage succeeds, then items are populated`() =
        test {
            whenever(
                statsRepository.fetchEmailsSummary(any(), any())
            ).thenReturn(
                EmailsStatsResult.Success(createItems(3))
            )

            viewModel.loadInitialPage()
            advanceUntilIdle()

            assertThat(viewModel.items.value).hasSize(3)
            assertThat(viewModel.isLoading.value).isFalse()
        }

    @Test
    fun `when loadInitialPage returns full page, then canLoadMore is true`() =
        test {
            whenever(
                statsRepository.fetchEmailsSummary(any(), any())
            ).thenReturn(
                EmailsStatsResult.Success(
                    createItems(PAGE_SIZE)
                )
            )

            viewModel.loadInitialPage()
            advanceUntilIdle()

            assertThat(viewModel.canLoadMore.value).isTrue()
        }

    @Test
    fun `when loadInitialPage returns fewer than page size, then canLoadMore is false`() =
        test {
            whenever(
                statsRepository.fetchEmailsSummary(any(), any())
            ).thenReturn(
                EmailsStatsResult.Success(createItems(5))
            )

            viewModel.loadInitialPage()
            advanceUntilIdle()

            assertThat(viewModel.canLoadMore.value).isFalse()
        }

    @Test
    fun `when loadMore succeeds, then items are replaced with full set`() =
        test {
            whenever(
                statsRepository.fetchEmailsSummary(
                    any(), eq(PAGE_SIZE)
                )
            ).thenReturn(
                EmailsStatsResult.Success(
                    createItems(PAGE_SIZE)
                )
            )
            whenever(
                statsRepository.fetchEmailsSummary(
                    any(), eq(PAGE_SIZE * 2)
                )
            ).thenReturn(
                EmailsStatsResult.Success(
                    createItems(PAGE_SIZE * 2)
                )
            )

            viewModel.loadInitialPage()
            advanceUntilIdle()

            viewModel.loadMore()
            advanceUntilIdle()

            assertThat(viewModel.items.value)
                .hasSize(PAGE_SIZE * 2)
            assertThat(viewModel.isLoadingMore.value).isFalse()
        }

    @Test
    fun `when loadMore returns fewer than quantity, then canLoadMore becomes false`() =
        test {
            whenever(
                statsRepository.fetchEmailsSummary(
                    any(), eq(PAGE_SIZE)
                )
            ).thenReturn(
                EmailsStatsResult.Success(
                    createItems(PAGE_SIZE)
                )
            )
            whenever(
                statsRepository.fetchEmailsSummary(
                    any(), eq(PAGE_SIZE * 2)
                )
            ).thenReturn(
                EmailsStatsResult.Success(
                    createItems(PAGE_SIZE + 5)
                )
            )

            viewModel.loadInitialPage()
            advanceUntilIdle()

            viewModel.loadMore()
            advanceUntilIdle()

            assertThat(viewModel.canLoadMore.value).isFalse()
        }

    @Test
    fun `when loadInitialPage errors, then canLoadMore is false`() =
        test {
            whenever(
                statsRepository.fetchEmailsSummary(any(), any())
            ).thenReturn(
                EmailsStatsResult.Error(messageResId = 0)
            )

            viewModel.loadInitialPage()
            advanceUntilIdle()

            assertThat(viewModel.items.value).isEmpty()
            assertThat(viewModel.canLoadMore.value).isFalse()
            assertThat(viewModel.hasError.value).isTrue()
        }

    @Test
    fun `when loadMore errors, then quantity is reverted for retry`() =
        test {
            whenever(
                statsRepository.fetchEmailsSummary(
                    any(), eq(PAGE_SIZE)
                )
            ).thenReturn(
                EmailsStatsResult.Success(
                    createItems(PAGE_SIZE)
                )
            )
            whenever(
                statsRepository.fetchEmailsSummary(
                    any(), eq(PAGE_SIZE * 2)
                )
            ).thenReturn(
                EmailsStatsResult.Error(messageResId = 0)
            )

            viewModel.loadInitialPage()
            advanceUntilIdle()

            viewModel.loadMore()
            advanceUntilIdle()

            // Items unchanged from first load
            assertThat(viewModel.items.value)
                .hasSize(PAGE_SIZE)
            // canLoadMore still true so retry is possible
            assertThat(viewModel.canLoadMore.value).isTrue()
        }

    @Test
    fun `when loadInitialPage called twice, then only loads once`() =
        test {
            whenever(
                statsRepository.fetchEmailsSummary(any(), any())
            ).thenReturn(
                EmailsStatsResult.Success(createItems(3))
            )

            viewModel.loadInitialPage()
            advanceUntilIdle()

            viewModel.loadInitialPage()
            advanceUntilIdle()

            verify(statsRepository, times(1))
                .fetchEmailsSummary(any(), any())
        }

    @Test
    fun `when no site selected, then items remain empty`() =
        test {
            whenever(selectedSiteRepository.getSelectedSite())
                .thenReturn(null)

            viewModel.loadInitialPage()
            advanceUntilIdle()

            assertThat(viewModel.items.value).isEmpty()
        }

    @Test
    fun `when access token is empty, then items remain empty`() =
        test {
            whenever(accountStore.accessToken).thenReturn("")

            viewModel.loadInitialPage()
            advanceUntilIdle()

            assertThat(viewModel.items.value).isEmpty()
        }

    @Test
    fun `when items map correctly, then title opens and clicks are set`() =
        test {
            val items = listOf(
                EmailItemData(
                    title = "My Newsletter",
                    opens = 500L,
                    clicks = 42L
                )
            )
            whenever(
                statsRepository.fetchEmailsSummary(any(), any())
            ).thenReturn(EmailsStatsResult.Success(items))

            viewModel.loadInitialPage()
            advanceUntilIdle()

            val item = viewModel.items.value[0]
            assertThat(item.title)
                .isEqualTo("My Newsletter")
            assertThat(item.opens).isEqualTo(500L)
            assertThat(item.clicks).isEqualTo(42L)
        }

    @Test
    fun `when exception thrown, then items remain empty and hasError is true`() =
        test {
            whenever(
                statsRepository.fetchEmailsSummary(any(), any())
            ).thenThrow(RuntimeException("Test exception"))

            viewModel.loadInitialPage()
            advanceUntilIdle()

            assertThat(viewModel.items.value).isEmpty()
            assertThat(viewModel.hasError.value).isTrue()
        }

    private fun createItems(count: Int) =
        (1..count).map {
            EmailItemData(
                title = "Email $it",
                opens = it.toLong() * 100,
                clicks = it.toLong() * 10
            )
        }

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_access_token"
        private const val PAGE_SIZE =
            EMAILS_DETAIL_PAGE_SIZE
    }
}
