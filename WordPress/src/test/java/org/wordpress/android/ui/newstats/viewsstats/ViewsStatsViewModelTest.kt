package org.wordpress.android.ui.newstats.viewsstats

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.runner.RunWith
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.DailyViewsDataPoint
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.WeeklyAggregates
import org.wordpress.android.ui.newstats.repository.WeeklyStatsWithDailyDataResult
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
@Suppress("LargeClass")
@RunWith(MockitoJUnitRunner.Silent::class)
class ViewsStatsViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: ViewsStatsViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(accountStore.accessToken).thenReturn(TEST_ACCESS_TOKEN)
        whenever(resourceProvider.getString(R.string.stats_todays_stats_no_site_selected))
            .thenReturn(NO_SITE_SELECTED_ERROR)
        whenever(resourceProvider.getString(R.string.stats_todays_stats_failed_to_load))
            .thenReturn(FAILED_TO_LOAD_ERROR)
        whenever(resourceProvider.getString(R.string.stats_todays_stats_unknown_error))
            .thenReturn(UNKNOWN_ERROR)
        whenever(resourceProvider.getString(R.string.stats_views))
            .thenReturn("Views")
        whenever(resourceProvider.getString(R.string.stats_visitors))
            .thenReturn("Visitors")
        whenever(resourceProvider.getString(R.string.stats_likes))
            .thenReturn("Likes")
        whenever(resourceProvider.getString(R.string.stats_comments))
            .thenReturn("Comments")
        whenever(resourceProvider.getString(R.string.posts))
            .thenReturn("Posts")
    }

    private fun initViewModel() {
        viewModel = ViewsStatsViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider
        )
    }

    @Test
    fun `when no site selected, then error state is emitted`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ViewsStatsCardUiState.Error::class.java)
        assertThat((state as ViewsStatsCardUiState.Error).message).isEqualTo(NO_SITE_SELECTED_ERROR)
    }

    @Test
    fun `when data loads successfully, then loaded state is emitted with correct values`() = test {
        val currentWeekResult = createWeeklyStatsWithDailyDataResult(
            views = TEST_CURRENT_WEEK_VIEWS,
            visitors = TEST_CURRENT_WEEK_VISITORS,
            likes = TEST_CURRENT_WEEK_LIKES,
            comments = TEST_CURRENT_WEEK_COMMENTS,
            posts = TEST_CURRENT_WEEK_POSTS
        )
        val previousWeekResult = createWeeklyStatsWithDailyDataResult(
            views = TEST_PREVIOUS_WEEK_VIEWS,
            visitors = TEST_PREVIOUS_WEEK_VISITORS,
            likes = TEST_PREVIOUS_WEEK_LIKES,
            comments = TEST_PREVIOUS_WEEK_COMMENTS,
            posts = TEST_PREVIOUS_WEEK_POSTS
        )

        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(0)))
            .thenReturn(currentWeekResult)
        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(1)))
            .thenReturn(previousWeekResult)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ViewsStatsCardUiState.Loaded::class.java)
        with(state as ViewsStatsCardUiState.Loaded) {
            assertThat(currentWeekViews).isEqualTo(TEST_CURRENT_WEEK_VIEWS)
            assertThat(previousWeekViews).isEqualTo(TEST_PREVIOUS_WEEK_VIEWS)
        }
    }

    @Test
    fun `when weekly stats fetch fails, then error state is emitted`() = test {
        whenever(statsRepository.fetchWeeklyStatsWithDailyData(any(), any()))
            .thenReturn(WeeklyStatsWithDailyDataResult.Error("Network error"))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ViewsStatsCardUiState.Error::class.java)
        assertThat((state as ViewsStatsCardUiState.Error).message).isEqualTo(FAILED_TO_LOAD_ERROR)
    }

    @Test
    fun `when daily views data is empty, then chart data is empty but state is loaded`() = test {
        val currentWeekResult = createWeeklyStatsWithDailyDataResult(dailyDataPoints = emptyList())
        val previousWeekResult = createWeeklyStatsWithDailyDataResult(dailyDataPoints = emptyList())

        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(0)))
            .thenReturn(currentWeekResult)
        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(1)))
            .thenReturn(previousWeekResult)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ViewsStatsCardUiState.Loaded::class.java)
        with(state as ViewsStatsCardUiState.Loaded) {
            assertThat(chartData.currentWeek).isEmpty()
            assertThat(chartData.previousWeek).isEmpty()
        }
    }

    @Test
    fun `when loadData is called with forced true, then repository is called`() = test {
        val currentWeekResult = createWeeklyStatsWithDailyDataResult()
        val previousWeekResult = createWeeklyStatsWithDailyDataResult()

        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(0)))
            .thenReturn(currentWeekResult)
        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(1)))
            .thenReturn(previousWeekResult)

        initViewModel()
        advanceUntilIdle()

        viewModel.loadData()
        advanceUntilIdle()

        // Called twice for each week (current and previous): 2 during init, 2 during loadData
        verify(statsRepository, times(2)).fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(0))
        verify(statsRepository, times(2)).fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(1))
    }

    @Test
    fun `when onRetry is called, then loadData is called with forced true`() = test {
        val currentWeekResult = createWeeklyStatsWithDailyDataResult()
        val previousWeekResult = createWeeklyStatsWithDailyDataResult()

        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(0)))
            .thenReturn(currentWeekResult)
        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(1)))
            .thenReturn(previousWeekResult)

        initViewModel()
        advanceUntilIdle()

        viewModel.onRetry()
        advanceUntilIdle()

        // Called twice for each week: once during init, once during onRetry
        verify(statsRepository, times(2)).fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(0))
        verify(statsRepository, times(2)).fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(1))
    }

    @Test
    fun `when data loads, then views difference is calculated correctly`() = test {
        val currentWeekResult = createWeeklyStatsWithDailyDataResult(views = 7000L)
        val previousWeekResult = createWeeklyStatsWithDailyDataResult(views = 8000L)

        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(0)))
            .thenReturn(currentWeekResult)
        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(1)))
            .thenReturn(previousWeekResult)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ViewsStatsCardUiState.Loaded
        assertThat(state.viewsDifference).isEqualTo(-1000L)
    }

    @Test
    fun `when data loads, then percentage change is calculated correctly`() = test {
        val currentWeekResult = createWeeklyStatsWithDailyDataResult(views = 9000L)
        val previousWeekResult = createWeeklyStatsWithDailyDataResult(views = 10000L)

        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(0)))
            .thenReturn(currentWeekResult)
        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(1)))
            .thenReturn(previousWeekResult)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ViewsStatsCardUiState.Loaded
        assertThat(state.viewsPercentageChange).isEqualTo(-10.0)
    }

    @Test
    fun `when previous week has zero views, then percentage change is 100 percent`() = test {
        val currentWeekResult = createWeeklyStatsWithDailyDataResult(views = 1000L)
        val previousWeekResult = createWeeklyStatsWithDailyDataResult(views = 0L)

        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(0)))
            .thenReturn(currentWeekResult)
        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(1)))
            .thenReturn(previousWeekResult)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ViewsStatsCardUiState.Loaded
        assertThat(state.viewsPercentageChange).isEqualTo(100.0)
    }

    @Test
    fun `when refresh is called, then isRefreshing becomes true then false`() = test {
        val currentWeekResult = createWeeklyStatsWithDailyDataResult()
        val previousWeekResult = createWeeklyStatsWithDailyDataResult()

        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(0)))
            .thenReturn(currentWeekResult)
        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(1)))
            .thenReturn(previousWeekResult)

        initViewModel()
        advanceUntilIdle()

        assertThat(viewModel.isRefreshing.value).isFalse()

        viewModel.refresh()
        advanceUntilIdle()

        assertThat(viewModel.isRefreshing.value).isFalse()
    }

    @Test
    fun `when access token is null, then error state is emitted`() = test {
        whenever(accountStore.accessToken).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ViewsStatsCardUiState.Error::class.java)
        assertThat((state as ViewsStatsCardUiState.Error).message).isEqualTo(FAILED_TO_LOAD_ERROR)
    }

    @Test
    fun `when bottom stats are built, then they contain all stat types`() = test {
        val currentWeekResult = createWeeklyStatsWithDailyDataResult()
        val previousWeekResult = createWeeklyStatsWithDailyDataResult()

        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(0)))
            .thenReturn(currentWeekResult)
        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(1)))
            .thenReturn(previousWeekResult)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ViewsStatsCardUiState.Loaded
        assertThat(state.bottomStats).hasSize(5)
        assertThat(state.bottomStats.map { it.label }).containsExactly(
            "Views", "Visitors", "Likes", "Comments", "Posts"
        )
    }

    @Test
    fun `when stat increases, then positive change is calculated`() = test {
        val currentWeekResult = createWeeklyStatsWithDailyDataResult(views = 1000L)
        val previousWeekResult = createWeeklyStatsWithDailyDataResult(views = 800L)

        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(0)))
            .thenReturn(currentWeekResult)
        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(1)))
            .thenReturn(previousWeekResult)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ViewsStatsCardUiState.Loaded
        val viewsStat = state.bottomStats.first { it.label == "Views" }
        assertThat(viewsStat.change).isInstanceOf(StatChange.Positive::class.java)
        assertThat((viewsStat.change as StatChange.Positive).percentage).isEqualTo(25.0)
    }

    @Test
    fun `when stat decreases, then negative change is calculated`() = test {
        val currentWeekResult = createWeeklyStatsWithDailyDataResult(views = 800L)
        val previousWeekResult = createWeeklyStatsWithDailyDataResult(views = 1000L)

        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(0)))
            .thenReturn(currentWeekResult)
        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(1)))
            .thenReturn(previousWeekResult)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ViewsStatsCardUiState.Loaded
        val viewsStat = state.bottomStats.first { it.label == "Views" }
        assertThat(viewsStat.change).isInstanceOf(StatChange.Negative::class.java)
        assertThat((viewsStat.change as StatChange.Negative).percentage).isEqualTo(20.0)
    }

    @Test
    fun `when stat is unchanged, then no change is calculated`() = test {
        val currentWeekResult = createWeeklyStatsWithDailyDataResult(views = 1000L)
        val previousWeekResult = createWeeklyStatsWithDailyDataResult(views = 1000L)

        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(0)))
            .thenReturn(currentWeekResult)
        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(1)))
            .thenReturn(previousWeekResult)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ViewsStatsCardUiState.Loaded
        val viewsStat = state.bottomStats.first { it.label == "Views" }
        assertThat(viewsStat.change).isInstanceOf(StatChange.NoChange::class.java)
    }

    @Test
    fun `when weekly average is calculated, then it is based on daily views count`() = test {
        val currentWeekResult = createWeeklyStatsWithDailyDataResult(views = 7000L)
        val previousWeekResult = createWeeklyStatsWithDailyDataResult()

        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(0)))
            .thenReturn(currentWeekResult)
        whenever(statsRepository.fetchWeeklyStatsWithDailyData(eq(TEST_SITE_ID), eq(1)))
            .thenReturn(previousWeekResult)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ViewsStatsCardUiState.Loaded
        // 7000 views / 2 data points = 3500 average
        assertThat(state.weeklyAverage).isEqualTo(3500L)
    }

    @Test
    fun `when exception is thrown during fetch, then error state is emitted`() = test {
        whenever(statsRepository.fetchWeeklyStatsWithDailyData(any(), any()))
            .thenThrow(RuntimeException("Test exception"))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ViewsStatsCardUiState.Error::class.java)
        assertThat((state as ViewsStatsCardUiState.Error).message).isEqualTo("Test exception")
    }

    private fun createWeeklyStatsWithDailyDataResult(
        views: Long = TEST_CURRENT_WEEK_VIEWS,
        visitors: Long = TEST_CURRENT_WEEK_VISITORS,
        likes: Long = TEST_CURRENT_WEEK_LIKES,
        comments: Long = TEST_CURRENT_WEEK_COMMENTS,
        posts: Long = TEST_CURRENT_WEEK_POSTS,
        dailyDataPoints: List<DailyViewsDataPoint> = createDefaultDailyDataPoints()
    ): WeeklyStatsWithDailyDataResult.Success {
        val aggregates = WeeklyAggregates(
            views = views,
            visitors = visitors,
            likes = likes,
            comments = comments,
            posts = posts,
            startDate = "2024-01-14",
            endDate = "2024-01-20"
        )
        return WeeklyStatsWithDailyDataResult.Success(aggregates, dailyDataPoints)
    }

    private fun createDefaultDailyDataPoints() = listOf(
        DailyViewsDataPoint(
            period = "2024-01-14",
            views = 1000L
        ),
        DailyViewsDataPoint(
            period = "2024-01-15",
            views = 1500L
        )
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_access_token"
        private const val TEST_CURRENT_WEEK_VIEWS = 7000L
        private const val TEST_CURRENT_WEEK_VISITORS = 700L
        private const val TEST_CURRENT_WEEK_LIKES = 50L
        private const val TEST_CURRENT_WEEK_COMMENTS = 25L
        private const val TEST_CURRENT_WEEK_POSTS = 5L
        private const val TEST_PREVIOUS_WEEK_VIEWS = 8000L
        private const val TEST_PREVIOUS_WEEK_VISITORS = 800L
        private const val TEST_PREVIOUS_WEEK_LIKES = 60L
        private const val TEST_PREVIOUS_WEEK_COMMENTS = 30L
        private const val TEST_PREVIOUS_WEEK_POSTS = 4L
        private const val NO_SITE_SELECTED_ERROR = "No site selected"
        private const val FAILED_TO_LOAD_ERROR = "Failed to load stats"
        private const val UNKNOWN_ERROR = "Unknown error"
    }
}
