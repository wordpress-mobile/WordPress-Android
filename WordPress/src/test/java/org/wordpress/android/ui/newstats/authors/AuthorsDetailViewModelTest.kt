package org.wordpress.android.ui.newstats.authors

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
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.TopAuthorItemData
import org.wordpress.android.ui.newstats.repository.TopAuthorsResult
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class AuthorsDetailViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: AuthorsDetailViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
        adminUrl = TEST_ADMIN_URL
    }

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(accountStore.accessToken).thenReturn(TEST_ACCESS_TOKEN)
        whenever(resourceProvider.getString(R.string.stats_period_last_7_days)).thenReturn(DATE_RANGE)
        whenever(resourceProvider.getString(R.string.stats_error_api)).thenReturn(API_ERROR)
        whenever(resourceProvider.getString(R.string.stats_error_unknown)).thenReturn(UNKNOWN_ERROR)
        viewModel = AuthorsDetailViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider
        )
    }

    @Test
    fun `when load succeeds, then loaded state with mapped authors and totals is emitted`() = test {
        whenever(statsRepository.fetchTopAuthors(any(), any())).thenReturn(createSuccessResult())

        viewModel.load(StatsPeriod.Last7Days)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AuthorsDetailUiState.Loaded::class.java)
        val loaded = state as AuthorsDetailUiState.Loaded
        assertThat(loaded.authors).hasSize(2)
        assertThat(loaded.authors[0].name).isEqualTo("John Doe")
        assertThat(loaded.authors[0].views).isEqualTo(TOP_AUTHOR_VIEWS)
        assertThat(loaded.authors[1].name).isEqualTo("Jane Smith")
        assertThat(loaded.totalViews).isEqualTo(TEST_TOTAL_VIEWS)
        assertThat(loaded.totalViewsChange).isEqualTo(TEST_TOTAL_VIEWS_CHANGE)
        assertThat(loaded.totalViewsChangePercent).isEqualTo(TEST_TOTAL_VIEWS_CHANGE_PERCENT)
        assertThat(loaded.dateRange).isEqualTo(DATE_RANGE)
    }

    @Test
    fun `when load succeeds, then maxViewsForBar is the first (highest) author's views`() = test {
        whenever(statsRepository.fetchTopAuthors(any(), any())).thenReturn(createSuccessResult())

        viewModel.load(StatsPeriod.Last7Days)
        advanceUntilIdle()

        val loaded = viewModel.uiState.value as AuthorsDetailUiState.Loaded
        assertThat(loaded.maxViewsForBar).isEqualTo(TOP_AUTHOR_VIEWS)
    }

    @Test
    fun `when the success result has no authors, then maxViewsForBar is zero`() = test {
        whenever(statsRepository.fetchTopAuthors(any(), any())).thenReturn(
            TopAuthorsResult.Success(
                authors = emptyList(),
                totalViews = 0L,
                totalViewsChange = 0L,
                totalViewsChangePercent = 0.0
            )
        )

        viewModel.load(StatsPeriod.Last7Days)
        advanceUntilIdle()

        val loaded = viewModel.uiState.value as AuthorsDetailUiState.Loaded
        assertThat(loaded.authors).isEmpty()
        assertThat(loaded.maxViewsForBar).isEqualTo(0L)
    }

    @Test
    fun `when no site is selected, then error state is emitted and no fetch happens`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        viewModel.load(StatsPeriod.Last7Days)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AuthorsDetailUiState.Error::class.java)
        assertThat((state as AuthorsDetailUiState.Error).message).isEqualTo(API_ERROR)
        verify(statsRepository, never()).fetchTopAuthors(any(), any())
    }

    @Test
    fun `when access token is empty, then error state is emitted and no fetch happens`() = test {
        whenever(accountStore.accessToken).thenReturn("")

        viewModel.load(StatsPeriod.Last7Days)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AuthorsDetailUiState.Error::class.java)
        assertThat((state as AuthorsDetailUiState.Error).message).isEqualTo(API_ERROR)
        verify(statsRepository, never()).fetchTopAuthors(any(), any())
    }

    @Test
    fun `when the repository returns an error, then its message resId is resolved into the error state`() = test {
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenReturn(TopAuthorsResult.Error(R.string.stats_error_api))

        viewModel.load(StatsPeriod.Last7Days)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AuthorsDetailUiState.Error::class.java)
        assertThat((state as AuthorsDetailUiState.Error).message).isEqualTo(API_ERROR)
    }

    @Test
    fun `when the repository returns an auth error, then the error state carries the auth flag`() = test {
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenReturn(TopAuthorsResult.Error(R.string.stats_error_api, isAuthError = true))

        viewModel.load(StatsPeriod.Last7Days)
        advanceUntilIdle()

        val state = viewModel.uiState.value as AuthorsDetailUiState.Error
        assertThat(state.message).isEqualTo(API_ERROR)
        assertThat(state.isAuthError).isTrue()
    }

    @Test
    fun `when getAdminUrl is called, then the selected site's admin url is returned`() {
        assertThat(viewModel.getAdminUrl()).isEqualTo(TEST_ADMIN_URL)
    }

    @Test
    fun `when the fetch throws, then the generic unknown error state is emitted`() = test {
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenThrow(RuntimeException(EXCEPTION_MESSAGE))

        viewModel.load(StatsPeriod.Last7Days)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AuthorsDetailUiState.Error::class.java)
        assertThat((state as AuthorsDetailUiState.Error).message).isEqualTo(UNKNOWN_ERROR)
    }

    @Test
    fun `when load is called twice, then the data is fetched only once`() = test {
        whenever(statsRepository.fetchTopAuthors(any(), any())).thenReturn(createSuccessResult())

        viewModel.load(StatsPeriod.Last7Days)
        advanceUntilIdle()
        viewModel.load(StatsPeriod.Last7Days)
        advanceUntilIdle()

        verify(statsRepository, times(1)).fetchTopAuthors(any(), any())
    }

    @Test
    fun `when retry is called after a load, then the data is fetched again`() = test {
        whenever(statsRepository.fetchTopAuthors(any(), any())).thenReturn(createSuccessResult())

        viewModel.load(StatsPeriod.Last7Days)
        advanceUntilIdle()
        viewModel.retry()
        advanceUntilIdle()

        verify(statsRepository, times(2)).fetchTopAuthors(any(), any())
    }

    private fun createSuccessResult() = TopAuthorsResult.Success(
        authors = listOf(
            TopAuthorItemData(name = "John Doe", avatarUrl = null, views = TOP_AUTHOR_VIEWS, previousViews = 100L),
            TopAuthorItemData(name = "Jane Smith", avatarUrl = null, views = 200L, previousViews = 220L)
        ),
        totalViews = TEST_TOTAL_VIEWS,
        totalViewsChange = TEST_TOTAL_VIEWS_CHANGE,
        totalViewsChangePercent = TEST_TOTAL_VIEWS_CHANGE_PERCENT
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_access_token"
        private const val TEST_ADMIN_URL = "https://example.com/wp-admin"
        private const val DATE_RANGE = "Last 7 days"
        private const val API_ERROR = "Failed to load stats"
        private const val UNKNOWN_ERROR = "Something went wrong"
        private const val EXCEPTION_MESSAGE = "boom"

        private const val TOP_AUTHOR_VIEWS = 500L
        private const val TEST_TOTAL_VIEWS = 700L
        private const val TEST_TOTAL_VIEWS_CHANGE = 60L
        private const val TEST_TOTAL_VIEWS_CHANGE_PERCENT = 9.4
    }
}
