package org.wordpress.android.ui.newstats.subscribers.subscriberslist

import android.content.Context
import android.content.res.Resources
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
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.SubscriberItemData
import org.wordpress.android.ui.newstats.repository.SubscribersListResult
import org.wordpress.android.viewmodel.ContextProvider

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner.Silent::class)
class SubscribersListDetailViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var contextProvider: ContextProvider

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var resources: Resources

    private lateinit var viewModel: SubscribersListDetailViewModel

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
        whenever(contextProvider.getContext())
            .thenReturn(context)
        whenever(context.resources).thenReturn(resources)
        viewModel = SubscribersListDetailViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            contextProvider
        )
    }

    @Test
    fun `when loadInitialPage succeeds, then items are populated`() =
        test {
            whenever(
                statsRepository.fetchSubscribersList(
                    any(), any(), any()
                )
            ).thenReturn(
                SubscribersListResult.Success(createItems(3))
            )

            viewModel.loadInitialPage()
            advanceUntilIdle()

            assertThat(viewModel.items.value).hasSize(3)
            assertThat(viewModel.isLoading.value).isFalse()
        }

    @Test
    fun `when loadInitialPage succeeds with full page, then canLoadMore is true`() =
        test {
            whenever(
                statsRepository.fetchSubscribersList(
                    any(), any(), any()
                )
            ).thenReturn(
                SubscribersListResult.Success(createItems(PAGE_SIZE))
            )

            viewModel.loadInitialPage()
            advanceUntilIdle()

            assertThat(viewModel.canLoadMore.value).isTrue()
        }

    @Test
    fun `when loadInitialPage returns fewer than page size, then canLoadMore is false`() =
        test {
            whenever(
                statsRepository.fetchSubscribersList(
                    any(), any(), any()
                )
            ).thenReturn(
                SubscribersListResult.Success(createItems(5))
            )

            viewModel.loadInitialPage()
            advanceUntilIdle()

            assertThat(viewModel.canLoadMore.value).isFalse()
        }

    @Test
    fun `when loadMore succeeds, then items are appended`() =
        test {
            whenever(
                statsRepository.fetchSubscribersList(
                    any(), any(), eq(1)
                )
            ).thenReturn(
                SubscribersListResult.Success(createItems(PAGE_SIZE))
            )
            whenever(
                statsRepository.fetchSubscribersList(
                    any(), any(), eq(2)
                )
            ).thenReturn(
                SubscribersListResult.Success(
                    createItems(5, startIndex = PAGE_SIZE)
                )
            )

            viewModel.loadInitialPage()
            advanceUntilIdle()

            viewModel.loadMore()
            advanceUntilIdle()

            assertThat(viewModel.items.value)
                .hasSize(PAGE_SIZE + 5)
            assertThat(viewModel.isLoadingMore.value).isFalse()
        }

    @Test
    fun `when loadMore returns fewer than page size, then canLoadMore becomes false`() =
        test {
            whenever(
                statsRepository.fetchSubscribersList(
                    any(), any(), eq(1)
                )
            ).thenReturn(
                SubscribersListResult.Success(createItems(PAGE_SIZE))
            )
            whenever(
                statsRepository.fetchSubscribersList(
                    any(), any(), eq(2)
                )
            ).thenReturn(
                SubscribersListResult.Success(createItems(3))
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
                statsRepository.fetchSubscribersList(
                    any(), any(), any()
                )
            ).thenReturn(
                SubscribersListResult.Error(
                    messageResId = 0
                )
            )

            viewModel.loadInitialPage()
            advanceUntilIdle()

            assertThat(viewModel.items.value).isEmpty()
            assertThat(viewModel.canLoadMore.value).isFalse()
            assertThat(viewModel.hasError.value).isTrue()
        }

    @Test
    fun `when loadMore errors, then page is decremented for retry`() =
        test {
            whenever(
                statsRepository.fetchSubscribersList(
                    any(), any(), eq(1)
                )
            ).thenReturn(
                SubscribersListResult.Success(createItems(PAGE_SIZE))
            )
            whenever(
                statsRepository.fetchSubscribersList(
                    any(), any(), eq(2)
                )
            ).thenReturn(
                SubscribersListResult.Error(messageResId = 0)
            )

            viewModel.loadInitialPage()
            advanceUntilIdle()

            viewModel.loadMore()
            advanceUntilIdle()

            // Items unchanged from first page
            assertThat(viewModel.items.value).hasSize(PAGE_SIZE)
            // canLoadMore still true so retry is possible
            assertThat(viewModel.canLoadMore.value).isTrue()
        }

    @Test
    fun `when loadInitialPage called twice, then only loads once`() =
        test {
            whenever(
                statsRepository.fetchSubscribersList(
                    any(), any(), any()
                )
            ).thenReturn(
                SubscribersListResult.Success(createItems(3))
            )

            viewModel.loadInitialPage()
            advanceUntilIdle()

            viewModel.loadInitialPage()
            advanceUntilIdle()

            verify(statsRepository, times(1))
                .fetchSubscribersList(any(), any(), any())
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
    fun `when items map correctly, then displayName and subscribedSince are set`() =
        test {
            val items = listOf(
                SubscriberItemData(
                    displayName = "John Doe",
                    subscribedSince = "2024-06-15T10:00:00"
                )
            )
            whenever(
                statsRepository.fetchSubscribersList(
                    any(), any(), any()
                )
            ).thenReturn(SubscribersListResult.Success(items))

            viewModel.loadInitialPage()
            advanceUntilIdle()

            val item = viewModel.items.value[0]
            assertThat(item.displayName)
                .isEqualTo("John Doe")
            assertThat(item.subscribedSince)
                .isEqualTo("2024-06-15T10:00:00")
        }

    @Test
    fun `when exception thrown, then items remain empty and hasError is true`() =
        test {
            whenever(
                statsRepository.fetchSubscribersList(
                    any(), any(), any()
                )
            ).thenThrow(RuntimeException("Test exception"))

            viewModel.loadInitialPage()
            advanceUntilIdle()

            assertThat(viewModel.items.value).isEmpty()
            assertThat(viewModel.hasError.value).isTrue()
        }

    private fun createItems(
        count: Int,
        startIndex: Int = 0
    ) = (startIndex until startIndex + count).map {
        SubscriberItemData(
            displayName = "User $it",
            subscribedSince = "2024-01-01"
        )
    }

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_access_token"
        private const val PAGE_SIZE =
            SUBSCRIBERS_DETAIL_PAGE_SIZE
    }
}
