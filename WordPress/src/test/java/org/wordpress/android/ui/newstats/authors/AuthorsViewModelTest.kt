package org.wordpress.android.ui.newstats.authors

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
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.components.StatsViewChange
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.TopAuthorItemData
import org.wordpress.android.ui.newstats.repository.TopAuthorsResult
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class AuthorsViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: AuthorsViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(accountStore.accessToken).thenReturn(TEST_ACCESS_TOKEN)
    }

    private fun initViewModel() {
        viewModel = AuthorsViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider
        )
        viewModel.onPeriodChanged(StatsPeriod.Last7Days)
    }

    // region Error states
    @Test
    fun `when no site selected, then error state is emitted`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)
        whenever(resourceProvider.getString(R.string.stats_todays_stats_no_site_selected))
            .thenReturn(NO_SITE_SELECTED_ERROR)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AuthorsCardUiState.Error::class.java)
        assertThat((state as AuthorsCardUiState.Error).message).isEqualTo(NO_SITE_SELECTED_ERROR)
    }

    @Test
    fun `when fetch fails, then error state is emitted`() = test {
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenReturn(TopAuthorsResult.Error(ERROR_MESSAGE))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AuthorsCardUiState.Error::class.java)
        assertThat((state as AuthorsCardUiState.Error).message).isEqualTo(ERROR_MESSAGE)
    }
    // endregion

    // region Success states
    @Test
    fun `when data loads successfully, then loaded state is emitted`() = test {
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AuthorsCardUiState.Loaded::class.java)
    }

    @Test
    fun `when data loads, then authors contain correct values`() = test {
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as AuthorsCardUiState.Loaded
        assertThat(state.authors).hasSize(2)
        assertThat(state.authors[0].name).isEqualTo(TEST_AUTHOR_NAME_1)
        assertThat(state.authors[0].views).isEqualTo(TEST_AUTHOR_VIEWS_1)
        assertThat(state.authors[0].avatarUrl).isEqualTo(TEST_AUTHOR_AVATAR_1)
        assertThat(state.authors[1].name).isEqualTo(TEST_AUTHOR_NAME_2)
        assertThat(state.authors[1].views).isEqualTo(TEST_AUTHOR_VIEWS_2)
    }

    @Test
    fun `when data loads, then maxViewsForBar is set to first author views`() = test {
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as AuthorsCardUiState.Loaded
        assertThat(state.maxViewsForBar).isEqualTo(TEST_AUTHOR_VIEWS_1)
    }

    @Test
    fun `when data loads with more than 10 authors, then only 10 are shown in card`() = test {
        val manyAuthors = (1..15).map { index ->
            TopAuthorItemData(
                name = "Author $index",
                avatarUrl = "https://example.com/avatar$index.jpg",
                views = (100 - index).toLong(),
                previousViews = (90 - index).toLong()
            )
        }
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenReturn(
                TopAuthorsResult.Success(
                    authors = manyAuthors,
                    totalViews = 1000,
                    totalViewsChange = 100,
                    totalViewsChangePercent = 10.0
                )
            )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as AuthorsCardUiState.Loaded
        assertThat(state.authors).hasSize(10)
        assertThat(state.hasMoreItems).isTrue()
    }

    @Test
    fun `when data loads with 10 or fewer authors, then hasMoreItems is false`() = test {
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as AuthorsCardUiState.Loaded
        assertThat(state.hasMoreItems).isFalse()
    }

    @Test
    fun `when data loads with empty authors, then loaded state with empty list is emitted`() = test {
        val emptyResult = TopAuthorsResult.Success(
            authors = emptyList(),
            totalViews = 0,
            totalViewsChange = 0,
            totalViewsChangePercent = 0.0
        )
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenReturn(emptyResult)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as AuthorsCardUiState.Loaded
        assertThat(state.authors).isEmpty()
        assertThat(state.maxViewsForBar).isEqualTo(0L)
        assertThat(state.hasMoreItems).isFalse()
    }
    // endregion

    // region Period changes
    @Test
    fun `when period changes, then data is reloaded`() = test {
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.onPeriodChanged(StatsPeriod.Last30Days)
        advanceUntilIdle()

        verify(statsRepository, times(2)).fetchTopAuthors(any(), any())
        verify(statsRepository).fetchTopAuthors(eq(TEST_SITE_ID), eq(StatsPeriod.Last30Days))
    }

    @Test
    fun `when same period is selected, then data is not reloaded`() = test {
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.onPeriodChanged(StatsPeriod.Last7Days)
        advanceUntilIdle()

        // Should only be called once during init
        verify(statsRepository, times(1)).fetchTopAuthors(any(), any())
    }
    // endregion

    // region Refresh
    @Test
    fun `when refresh is called, then isRefreshing becomes true then false`() = test {
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        assertThat(viewModel.isRefreshing.value).isFalse()

        viewModel.refresh()
        advanceUntilIdle()

        assertThat(viewModel.isRefreshing.value).isFalse()
    }

    @Test
    fun `when refresh is called, then data is fetched`() = test {
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        // Called twice: once during init, once during refresh
        verify(statsRepository, times(2)).fetchTopAuthors(eq(TEST_SITE_ID), any())
    }

    @Test
    fun `when refresh is called with no site, then data is not fetched`() = test {
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        viewModel.refresh()
        advanceUntilIdle()

        // Should only be called once during init
        verify(statsRepository, times(1)).fetchTopAuthors(any(), any())
    }
    // endregion

    // region Retry
    @Test
    fun `when onRetry is called, then data is reloaded`() = test {
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.onRetry()
        advanceUntilIdle()

        // Called twice: once during init, once during retry
        verify(statsRepository, times(2)).fetchTopAuthors(any(), any())
    }
    // endregion

    // region getDetailData
    @Test
    fun `when getDetailData is called, then returns cached data`() = test {
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenReturn(createSuccessResult())
        whenever(resourceProvider.getString(R.string.stats_period_last_7_days))
            .thenReturn("Last 7 days")

        initViewModel()
        advanceUntilIdle()

        val detailData = viewModel.getDetailData()

        assertThat(detailData.authors).hasSize(2)
        assertThat(detailData.totalViews).isEqualTo(TEST_TOTAL_VIEWS)
        assertThat(detailData.totalViewsChange).isEqualTo(TEST_TOTAL_VIEWS_CHANGE)
        assertThat(detailData.totalViewsChangePercent).isEqualTo(TEST_TOTAL_VIEWS_CHANGE_PERCENT)
        assertThat(detailData.dateRange).isEqualTo("Last 7 days")
    }

    @Test
    fun `when getDetailData is called, then all authors are returned not just card items`() = test {
        val manyAuthors = (1..15).map { index ->
            TopAuthorItemData(
                name = "Author $index",
                avatarUrl = "https://example.com/avatar$index.jpg",
                views = (100 - index).toLong(),
                previousViews = (90 - index).toLong()
            )
        }
        whenever(resourceProvider.getString(R.string.stats_period_last_7_days))
            .thenReturn("Last 7 days")
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenReturn(
                TopAuthorsResult.Success(
                    authors = manyAuthors,
                    totalViews = 1000,
                    totalViewsChange = 100,
                    totalViewsChangePercent = 10.0
                )
            )

        initViewModel()
        advanceUntilIdle()

        val detailData = viewModel.getDetailData()
        // Card shows max 10, but detail data should have all 15
        assertThat(detailData.authors).hasSize(15)
    }
    // endregion

    // region Change calculations
    @Test
    fun `when author has positive change, then StatsViewChange_Positive is returned`() = test {
        val authors = listOf(
            TopAuthorItemData(
                name = "Author 1",
                avatarUrl = null,
                views = 150,
                previousViews = 100
            )
        )
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenReturn(
                TopAuthorsResult.Success(
                    authors = authors,
                    totalViews = 150,
                    totalViewsChange = 50,
                    totalViewsChangePercent = 50.0
                )
            )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as AuthorsCardUiState.Loaded
        assertThat(state.authors[0].change).isInstanceOf(StatsViewChange.Positive::class.java)
        val change = state.authors[0].change as StatsViewChange.Positive
        assertThat(change.value).isEqualTo(50)
        assertThat(change.percentage).isEqualTo(50.0)
    }

    @Test
    fun `when author has negative change, then StatsViewChange_Negative is returned`() = test {
        val authors = listOf(
            TopAuthorItemData(
                name = "Author 1",
                avatarUrl = null,
                views = 50,
                previousViews = 100
            )
        )
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenReturn(
                TopAuthorsResult.Success(
                    authors = authors,
                    totalViews = 50,
                    totalViewsChange = -50,
                    totalViewsChangePercent = -50.0
                )
            )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as AuthorsCardUiState.Loaded
        assertThat(state.authors[0].change).isInstanceOf(StatsViewChange.Negative::class.java)
        val change = state.authors[0].change as StatsViewChange.Negative
        assertThat(change.value).isEqualTo(50)
        assertThat(change.percentage).isEqualTo(50.0)
    }

    @Test
    fun `when author has no change, then StatsViewChange_NoChange is returned`() = test {
        val authors = listOf(
            TopAuthorItemData(
                name = "Author 1",
                avatarUrl = null,
                views = 100,
                previousViews = 100
            )
        )
        whenever(statsRepository.fetchTopAuthors(any(), any()))
            .thenReturn(
                TopAuthorsResult.Success(
                    authors = authors,
                    totalViews = 100,
                    totalViewsChange = 0,
                    totalViewsChangePercent = 0.0
                )
            )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as AuthorsCardUiState.Loaded
        assertThat(state.authors[0].change).isEqualTo(StatsViewChange.NoChange)
    }
    // endregion

    // region Helper functions
    private fun createSuccessResult() = TopAuthorsResult.Success(
        authors = listOf(
            TopAuthorItemData(
                name = TEST_AUTHOR_NAME_1,
                avatarUrl = TEST_AUTHOR_AVATAR_1,
                views = TEST_AUTHOR_VIEWS_1,
                previousViews = TEST_AUTHOR_PREVIOUS_VIEWS_1
            ),
            TopAuthorItemData(
                name = TEST_AUTHOR_NAME_2,
                avatarUrl = TEST_AUTHOR_AVATAR_2,
                views = TEST_AUTHOR_VIEWS_2,
                previousViews = TEST_AUTHOR_PREVIOUS_VIEWS_2
            )
        ),
        totalViews = TEST_TOTAL_VIEWS,
        totalViewsChange = TEST_TOTAL_VIEWS_CHANGE,
        totalViewsChangePercent = TEST_TOTAL_VIEWS_CHANGE_PERCENT
    )
    // endregion

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_access_token"
        private const val ERROR_MESSAGE = "Network error"
        private const val NO_SITE_SELECTED_ERROR = "No site selected"

        private const val TEST_AUTHOR_NAME_1 = "John Doe"
        private const val TEST_AUTHOR_NAME_2 = "Jane Smith"
        private const val TEST_AUTHOR_AVATAR_1 = "https://example.com/avatar1.jpg"
        private const val TEST_AUTHOR_AVATAR_2 = "https://example.com/avatar2.jpg"
        private const val TEST_AUTHOR_VIEWS_1 = 500L
        private const val TEST_AUTHOR_VIEWS_2 = 300L
        private const val TEST_AUTHOR_PREVIOUS_VIEWS_1 = 400L
        private const val TEST_AUTHOR_PREVIOUS_VIEWS_2 = 250L

        private const val TEST_TOTAL_VIEWS = 800L
        private const val TEST_TOTAL_VIEWS_CHANGE = 150L
        private const val TEST_TOTAL_VIEWS_CHANGE_PERCENT = 23.1
    }
}
