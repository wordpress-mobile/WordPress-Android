package org.wordpress.android.ui.newstats.subscribers.alltimestats

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
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.SubscribersAllTimeResult
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class AllTimeSubscribersViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: AllTimeSubscribersViewModel

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
        whenever(resourceProvider.getString(R.string.stats_error_not_authenticated))
            .thenReturn("Not authenticated")
        whenever(resourceProvider.getString(R.string.stats_error_unknown))
            .thenReturn("Unknown error")
    }

    private fun initViewModel() {
        viewModel = AllTimeSubscribersViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider
        )
        viewModel.loadData()
    }

    @Test
    fun `when no site selected, then error state is emitted`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AllTimeSubscribersUiState.Error::class.java)
    }

    @Test
    fun `when access token is null, then error state is emitted`() = test {
        whenever(accountStore.accessToken).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AllTimeSubscribersUiState.Error::class.java)
    }

    @Test
    fun `when access token is empty, then error state is emitted`() = test {
        whenever(accountStore.accessToken).thenReturn("")

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AllTimeSubscribersUiState.Error::class.java)
    }

    @Test
    fun `when data loads successfully, then loaded state has correct counts`() = test {
        whenever(statsRepository.fetchSubscribersAllTime(any()))
            .thenReturn(
                SubscribersAllTimeResult.Success(
                    currentCount = TEST_CURRENT,
                    count30DaysAgo = TEST_30D,
                    count60DaysAgo = TEST_60D,
                    count90DaysAgo = TEST_90D
                )
            )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AllTimeSubscribersUiState.Loaded::class.java)
        with(state as AllTimeSubscribersUiState.Loaded) {
            assertThat(currentCount).isEqualTo(TEST_CURRENT)
            assertThat(count30DaysAgo).isEqualTo(TEST_30D)
            assertThat(count60DaysAgo).isEqualTo(TEST_60D)
            assertThat(count90DaysAgo).isEqualTo(TEST_90D)
        }
    }

    @Test
    fun `when fetch fails with api error, then error state is emitted`() = test {
        whenever(statsRepository.fetchSubscribersAllTime(any()))
            .thenReturn(
                SubscribersAllTimeResult.Error(
                    messageResId = R.string.stats_error_api
                )
            )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AllTimeSubscribersUiState.Error::class.java)
        assertThat((state as AllTimeSubscribersUiState.Error).message)
            .isEqualTo(FAILED_TO_LOAD_ERROR)
    }

    @Test
    fun `when exception is thrown, then error state has unknown error message`() = test {
        whenever(statsRepository.fetchSubscribersAllTime(any()))
            .thenThrow(RuntimeException("Test exception"))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AllTimeSubscribersUiState.Error::class.java)
        assertThat((state as AllTimeSubscribersUiState.Error).message)
            .isEqualTo(UNKNOWN_ERROR)
    }

    @Test
    fun `when exception with null message is thrown, then error has unknown error`() = test {
        whenever(statsRepository.fetchSubscribersAllTime(any()))
            .thenThrow(RuntimeException())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AllTimeSubscribersUiState.Error::class.java)
        assertThat((state as AllTimeSubscribersUiState.Error).message)
            .isEqualTo(UNKNOWN_ERROR)
    }

    @Test
    fun `when loadDataIfNeeded called multiple times, then data is only loaded once`() = test {
        whenever(statsRepository.fetchSubscribersAllTime(any()))
            .thenReturn(createSuccessResult())

        viewModel = AllTimeSubscribersViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider
        )
        viewModel.loadDataIfNeeded()
        advanceUntilIdle()

        viewModel.loadDataIfNeeded()
        advanceUntilIdle()

        viewModel.loadDataIfNeeded()
        advanceUntilIdle()

        verify(statsRepository, times(1)).fetchSubscribersAllTime(eq(TEST_SITE_ID))
    }

    @Test
    fun `when loadData is called again, then repository is called again`() = test {
        whenever(statsRepository.fetchSubscribersAllTime(any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.loadData()
        advanceUntilIdle()

        verify(statsRepository, times(2)).fetchSubscribersAllTime(eq(TEST_SITE_ID))
    }

    @Test
    fun `when refresh is called, then isRefreshing becomes false after completion`() = test {
        whenever(statsRepository.fetchSubscribersAllTime(any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        assertThat(viewModel.isRefreshing.value).isFalse()

        viewModel.refresh()
        advanceUntilIdle()

        assertThat(viewModel.isRefreshing.value).isFalse()
    }

    @Test
    fun `when refresh is called, then data is fetched again`() = test {
        whenever(statsRepository.fetchSubscribersAllTime(any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        verify(statsRepository, times(2)).fetchSubscribersAllTime(eq(TEST_SITE_ID))
    }

    @Test
    fun `when data loads with zero values, then loaded state shows zeros`() = test {
        whenever(statsRepository.fetchSubscribersAllTime(any()))
            .thenReturn(
                SubscribersAllTimeResult.Success(
                    currentCount = 0L,
                    count30DaysAgo = 0L,
                    count60DaysAgo = 0L,
                    count90DaysAgo = 0L
                )
            )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as AllTimeSubscribersUiState.Loaded
        assertThat(state.currentCount).isEqualTo(0L)
        assertThat(state.count30DaysAgo).isEqualTo(0L)
        assertThat(state.count60DaysAgo).isEqualTo(0L)
        assertThat(state.count90DaysAgo).isEqualTo(0L)
    }

    @Test
    fun `when data loads, then statsRepository init is called with token`() = test {
        whenever(statsRepository.fetchSubscribersAllTime(any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        verify(statsRepository).init(eq(TEST_ACCESS_TOKEN))
    }

    private fun createSuccessResult() = SubscribersAllTimeResult.Success(
        currentCount = TEST_CURRENT,
        count30DaysAgo = TEST_30D,
        count60DaysAgo = TEST_60D,
        count90DaysAgo = TEST_90D
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_access_token"
        private const val TEST_CURRENT = 1000L
        private const val TEST_30D = 950L
        private const val TEST_60D = 900L
        private const val TEST_90D = 850L
        private const val FAILED_TO_LOAD_ERROR = "Failed to load stats"
        private const val UNKNOWN_ERROR = "Unknown error"
    }
}
