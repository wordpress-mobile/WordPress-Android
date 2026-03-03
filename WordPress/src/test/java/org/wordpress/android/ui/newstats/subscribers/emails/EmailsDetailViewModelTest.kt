package org.wordpress.android.ui.newstats.subscribers.emails

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
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
    private lateinit var selectedSiteRepository:
        SelectedSiteRepository

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
    fun `when loadData succeeds, then items are populated`() =
        test {
            whenever(
                statsRepository.fetchEmailsSummary(
                    any(), any()
                )
            ).thenReturn(
                EmailsStatsResult.Success(createItems(3))
            )

            viewModel.loadData()
            advanceUntilIdle()

            assertThat(viewModel.items.value).hasSize(3)
            assertThat(viewModel.isLoading.value).isFalse()
            assertThat(viewModel.hasError.value).isFalse()
        }

    @Test
    fun `when loadData errors, then hasError is true`() =
        test {
            whenever(
                statsRepository.fetchEmailsSummary(
                    any(), any()
                )
            ).thenReturn(
                EmailsStatsResult.Error(messageResId = 0)
            )

            viewModel.loadData()
            advanceUntilIdle()

            assertThat(viewModel.items.value).isEmpty()
            assertThat(viewModel.hasError.value).isTrue()
        }

    @Test
    fun `when loadData called twice, then only loads once`() =
        test {
            whenever(
                statsRepository.fetchEmailsSummary(
                    any(), any()
                )
            ).thenReturn(
                EmailsStatsResult.Success(createItems(3))
            )

            viewModel.loadData()
            advanceUntilIdle()

            viewModel.loadData()
            advanceUntilIdle()

            verify(statsRepository, times(1))
                .fetchEmailsSummary(any(), any())
        }

    @Test
    fun `when no site selected, then hasError is true`() =
        test {
            whenever(
                selectedSiteRepository.getSelectedSite()
            ).thenReturn(null)

            viewModel.loadData()
            advanceUntilIdle()

            assertThat(viewModel.items.value).isEmpty()
            assertThat(viewModel.hasError.value).isTrue()
        }

    @Test
    fun `when access token is empty, then hasError is true`() =
        test {
            whenever(accountStore.accessToken).thenReturn("")

            viewModel.loadData()
            advanceUntilIdle()

            assertThat(viewModel.items.value).isEmpty()
            assertThat(viewModel.hasError.value).isTrue()
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
                statsRepository.fetchEmailsSummary(
                    any(), any()
                )
            ).thenReturn(
                EmailsStatsResult.Success(items)
            )

            viewModel.loadData()
            advanceUntilIdle()

            val item = viewModel.items.value[0]
            assertThat(item.title)
                .isEqualTo("My Newsletter")
            assertThat(item.opens).isEqualTo(500L)
            assertThat(item.clicks).isEqualTo(42L)
        }

    @Test
    fun `when exception thrown, then hasError is true`() =
        test {
            whenever(
                statsRepository.fetchEmailsSummary(
                    any(), any()
                )
            ).thenThrow(
                RuntimeException("Test exception")
            )

            viewModel.loadData()
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
        private const val TEST_ACCESS_TOKEN =
            "test_access_token"
    }
}
