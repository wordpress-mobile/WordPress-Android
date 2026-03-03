package org.wordpress.android.ui.newstats.subscribers.subscriberslist

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
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.SubscriberItemData
import org.wordpress.android.ui.newstats.repository.SubscribersListResult
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner.Silent::class)
class SubscribersListViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var contextProvider: ContextProvider

    @Mock
    private lateinit var context: android.content.Context

    @Mock
    private lateinit var resources: android.content.res.Resources

    private lateinit var viewModel: SubscribersListViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(accountStore.accessToken).thenReturn(TEST_ACCESS_TOKEN)
        whenever(resourceProvider.getString(R.string.stats_error_api))
            .thenReturn(FAILED_TO_LOAD_ERROR)
        whenever(resourceProvider.getString(R.string.stats_error_no_site))
            .thenReturn("No site selected")
        whenever(
            resourceProvider.getString(R.string.stats_error_not_authenticated)
        ).thenReturn("Not authenticated")
        whenever(resourceProvider.getString(R.string.stats_error_unknown))
            .thenReturn("Unknown error")
        whenever(contextProvider.getContext()).thenReturn(context)
        whenever(context.resources).thenReturn(resources)
        whenever(
            resources.getQuantityString(any(), any(), any())
        ).thenReturn("1 year")
        whenever(
            resources.getString(
                eq(R.string.stats_subscriber_years_and_days),
                any(), any()
            )
        ).thenReturn("1 year, 1 day")
    }

    private fun initViewModel() {
        viewModel = SubscribersListViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider,
            contextProvider
        )
        viewModel.loadData()
    }

    @Test
    fun `when no site selected, then error state is emitted`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(SubscribersListUiState.Error::class.java)
    }

    @Test
    fun `when access token is null, then error state is emitted`() = test {
        whenever(accountStore.accessToken).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(SubscribersListUiState.Error::class.java)
    }

    @Test
    fun `when access token is empty, then error state is emitted`() = test {
        whenever(accountStore.accessToken).thenReturn("")

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(SubscribersListUiState.Error::class.java)
    }

    @Test
    fun `when data loads successfully, then loaded state has items truncated to 5`() = test {
        val items = (1..10).map {
            SubscriberItemData(
                displayName = "User $it",
                subscribedSince = "2024-01-0$it"
            )
        }
        whenever(statsRepository.fetchSubscribersList(any(), any(), any()))
            .thenReturn(SubscribersListResult.Success(items))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(SubscribersListUiState.Loaded::class.java)
        assertThat((state as SubscribersListUiState.Loaded).items).hasSize(5)
    }

    @Test
    fun `when data has fewer than 5 items, then all items are shown`() = test {
        val items = (1..3).map {
            SubscriberItemData(
                displayName = "User $it",
                subscribedSince = "2024-01-0$it"
            )
        }
        whenever(statsRepository.fetchSubscribersList(any(), any(), any()))
            .thenReturn(SubscribersListResult.Success(items))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as SubscribersListUiState.Loaded
        assertThat(state.items).hasSize(3)
    }

    @Test
    fun `when data loads with empty list, then loaded state has empty items`() = test {
        whenever(statsRepository.fetchSubscribersList(any(), any(), any()))
            .thenReturn(SubscribersListResult.Success(emptyList()))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as SubscribersListUiState.Loaded
        assertThat(state.items).isEmpty()
    }

    @Test
    fun `when fetch fails, then error state is emitted`() = test {
        whenever(statsRepository.fetchSubscribersList(any(), any(), any()))
            .thenReturn(
                SubscribersListResult.Error(
                    messageResId = R.string.stats_error_api
                )
            )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(SubscribersListUiState.Error::class.java)
        assertThat((state as SubscribersListUiState.Error).message)
            .isEqualTo(FAILED_TO_LOAD_ERROR)
    }

    @Test
    fun `when exception is thrown, then error state has unknown error message`() = test {
        whenever(statsRepository.fetchSubscribersList(any(), any(), any()))
            .thenThrow(RuntimeException("Test exception"))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(SubscribersListUiState.Error::class.java)
        assertThat((state as SubscribersListUiState.Error).message)
            .isEqualTo(UNKNOWN_ERROR)
    }

    @Test
    fun `when loadDataIfNeeded called multiple times, then data is only loaded once`() = test {
        whenever(statsRepository.fetchSubscribersList(any(), any(), any()))
            .thenReturn(SubscribersListResult.Success(createTestItems()))

        viewModel = SubscribersListViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider,
            contextProvider
        )
        viewModel.loadDataIfNeeded()
        advanceUntilIdle()

        viewModel.loadDataIfNeeded()
        advanceUntilIdle()

        verify(statsRepository, times(1)).fetchSubscribersList(eq(TEST_SITE_ID), any(), any())
    }

    @Test
    fun `when loadData is called again, then repository is called again`() = test {
        whenever(statsRepository.fetchSubscribersList(any(), any(), any()))
            .thenReturn(SubscribersListResult.Success(createTestItems()))

        initViewModel()
        advanceUntilIdle()

        viewModel.loadData()
        advanceUntilIdle()

        verify(statsRepository, times(2)).fetchSubscribersList(eq(TEST_SITE_ID), any(), any())
    }

    @Test
    fun `when refresh is called, then isRefreshing becomes false after completion`() = test {
        whenever(statsRepository.fetchSubscribersList(any(), any(), any()))
            .thenReturn(SubscribersListResult.Success(createTestItems()))

        initViewModel()
        advanceUntilIdle()

        assertThat(viewModel.isRefreshing.value).isFalse()

        viewModel.refresh()
        advanceUntilIdle()

        assertThat(viewModel.isRefreshing.value).isFalse()
    }

    @Test
    fun `when data loads, then items map displayName and subscribedSince correctly`() = test {
        val items = listOf(
            SubscriberItemData(
                displayName = "John Doe",
                subscribedSince = "2024-06-15T10:00:00"
            )
        )
        whenever(statsRepository.fetchSubscribersList(any(), any(), any()))
            .thenReturn(SubscribersListResult.Success(items))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as SubscribersListUiState.Loaded
        assertThat(state.items[0].displayName).isEqualTo("John Doe")
        assertThat(state.items[0].subscribedSince).isEqualTo("2024-06-15T10:00:00")
    }

    @Test
    fun `when data loads, then statsRepository init is called with token`() = test {
        whenever(statsRepository.fetchSubscribersList(any(), any(), any()))
            .thenReturn(SubscribersListResult.Success(createTestItems()))

        initViewModel()
        advanceUntilIdle()

        verify(statsRepository).init(eq(TEST_ACCESS_TOKEN))
    }

    private fun createTestItems() = listOf(
        SubscriberItemData(displayName = "User 1", subscribedSince = "2024-01-01"),
        SubscriberItemData(displayName = "User 2", subscribedSince = "2024-01-02")
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_access_token"
        private const val FAILED_TO_LOAD_ERROR = "Failed to load stats"
        private const val UNKNOWN_ERROR = "Unknown error"
    }
}
