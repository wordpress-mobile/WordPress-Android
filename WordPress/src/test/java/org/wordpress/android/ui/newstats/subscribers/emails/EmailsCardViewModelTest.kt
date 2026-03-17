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
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.EmailItemData
import org.wordpress.android.ui.newstats.repository.EmailsStatsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner.Silent::class)
class EmailsCardViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: EmailsCardViewModel

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
    }

    private fun initViewModel() {
        viewModel = EmailsCardViewModel(
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
        assertThat(state).isInstanceOf(EmailsCardUiState.Error::class.java)
    }

    @Test
    fun `when access token is null, then error state is emitted`() = test {
        whenever(accountStore.accessToken).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(EmailsCardUiState.Error::class.java)
    }

    @Test
    fun `when access token is empty, then error state is emitted`() = test {
        whenever(accountStore.accessToken).thenReturn("")

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(EmailsCardUiState.Error::class.java)
    }

    @Test
    fun `when data loads successfully, then loaded state has items truncated to 5`() = test {
        val items = (1..10).map {
            EmailItemData(
                title = "Email $it",
                opens = it.toLong() * 100,
                clicks = it.toLong() * 10
            )
        }
        whenever(statsRepository.fetchEmailsSummary(any(), any()))
            .thenReturn(EmailsStatsResult.Success(items))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(EmailsCardUiState.Loaded::class.java)
        assertThat((state as EmailsCardUiState.Loaded).items).hasSize(5)
    }

    @Test
    fun `when data has fewer than 5 items, then all items are shown`() = test {
        val items = (1..3).map {
            EmailItemData(
                title = "Email $it",
                opens = it.toLong() * 100,
                clicks = it.toLong() * 10
            )
        }
        whenever(statsRepository.fetchEmailsSummary(any(), any()))
            .thenReturn(EmailsStatsResult.Success(items))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as EmailsCardUiState.Loaded
        assertThat(state.items).hasSize(3)
    }

    @Test
    fun `when data loads with empty list, then loaded state has empty items`() = test {
        whenever(statsRepository.fetchEmailsSummary(any(), any()))
            .thenReturn(EmailsStatsResult.Success(emptyList()))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as EmailsCardUiState.Loaded
        assertThat(state.items).isEmpty()
    }

    @Test
    fun `when fetch fails, then error state is emitted`() = test {
        whenever(statsRepository.fetchEmailsSummary(any(), any()))
            .thenReturn(
                EmailsStatsResult.Error(
                    messageResId = R.string.stats_error_api
                )
            )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(EmailsCardUiState.Error::class.java)
        assertThat((state as EmailsCardUiState.Error).message)
            .isEqualTo(FAILED_TO_LOAD_ERROR)
    }

    @Test
    fun `when exception is thrown, then error state has unknown error message`() = test {
        whenever(statsRepository.fetchEmailsSummary(any(), any()))
            .thenThrow(RuntimeException("Test exception"))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(EmailsCardUiState.Error::class.java)
        assertThat((state as EmailsCardUiState.Error).message)
            .isEqualTo(UNKNOWN_ERROR)
    }

    @Test
    fun `when loadDataIfNeeded called multiple times, then data is only loaded once`() = test {
        whenever(statsRepository.fetchEmailsSummary(any(), any()))
            .thenReturn(EmailsStatsResult.Success(createTestItems()))

        viewModel = EmailsCardViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider
        )
        viewModel.loadDataIfNeeded()
        advanceUntilIdle()

        viewModel.loadDataIfNeeded()
        advanceUntilIdle()

        verify(statsRepository, times(1)).fetchEmailsSummary(eq(TEST_SITE_ID), any())
    }

    @Test
    fun `when loadData is called again, then repository is called again`() = test {
        whenever(statsRepository.fetchEmailsSummary(any(), any()))
            .thenReturn(EmailsStatsResult.Success(createTestItems()))

        initViewModel()
        advanceUntilIdle()

        viewModel.loadData()
        advanceUntilIdle()

        verify(statsRepository, times(2)).fetchEmailsSummary(eq(TEST_SITE_ID), any())
    }

    @Test
    fun `when refresh is called, then isRefreshing becomes false after completion`() = test {
        whenever(statsRepository.fetchEmailsSummary(any(), any()))
            .thenReturn(EmailsStatsResult.Success(createTestItems()))

        initViewModel()
        advanceUntilIdle()

        assertThat(viewModel.isRefreshing.value).isFalse()

        viewModel.refresh()
        advanceUntilIdle()

        assertThat(viewModel.isRefreshing.value).isFalse()
    }

    @Test
    fun `when data loads, then items map title opens and clicks correctly`() = test {
        val items = listOf(
            EmailItemData(
                title = "My Newsletter",
                opens = 500L,
                clicks = 42L
            )
        )
        whenever(statsRepository.fetchEmailsSummary(any(), any()))
            .thenReturn(EmailsStatsResult.Success(items))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as EmailsCardUiState.Loaded
        assertThat(state.items[0].title).isEqualTo("My Newsletter")
        assertThat(state.items[0].opens).isEqualTo(500L)
        assertThat(state.items[0].clicks).isEqualTo(42L)
    }

    @Test
    fun `when data loads, then statsRepository init is called with token`() = test {
        whenever(statsRepository.fetchEmailsSummary(any(), any()))
            .thenReturn(EmailsStatsResult.Success(createTestItems()))

        initViewModel()
        advanceUntilIdle()

        verify(statsRepository).init(eq(TEST_ACCESS_TOKEN))
    }

    private fun createTestItems() = listOf(
        EmailItemData(title = "Email 1", opens = 100L, clicks = 10L),
        EmailItemData(title = "Email 2", opens = 200L, clicks = 20L)
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_access_token"
        private const val FAILED_TO_LOAD_ERROR = "Failed to load stats"
        private const val UNKNOWN_ERROR = "Unknown error"
    }
}
