package org.wordpress.android.ui.newstats.poststats

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.datasource.PostViewsDailyView
import org.wordpress.android.ui.newstats.datasource.PostViewsData
import org.wordpress.android.ui.newstats.repository.PostViewsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class PostStatsDetailViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository:
        SelectedSiteRepository

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: PostStatsDetailViewModel

    private val site = SiteModel().apply {
        siteId = TEST_SITE_ID
    }

    @Before
    fun setUp() {
        lenient().`when`(accountStore.accessToken)
            .thenReturn(TEST_ACCESS_TOKEN)
        lenient().`when`(
            selectedSiteRepository.getSelectedSite()
        ).thenReturn(site)
        lenient().`when`(
            resourceProvider.getString(
                R.string.stats_error_api
            )
        ).thenReturn(ERROR)
        lenient().`when`(
            resourceProvider.getString(
                R.string.stats_error_no_site
            )
        ).thenReturn(ERROR)
        viewModel = PostStatsDetailViewModel(
            selectedSiteRepository,
            statsRepository,
            accountStore,
            resourceProvider
        )
    }

    @Test
    fun `when no site is selected, then the state is an error`() =
        test {
            whenever(
                selectedSiteRepository.getSelectedSite()
            ).thenReturn(null)

            viewModel.loadData(TEST_POST_ID)

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    PostStatsDetailUiState.Error::class.java
                )
        }

    @Test
    fun `when the fetch fails, then the state is an error`() =
        test {
            givenFetchReturns(
                PostViewsResult.Error("boom")
            )

            viewModel.loadData(TEST_POST_ID)

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    PostStatsDetailUiState.Error::class.java
                )
        }

    @Test
    fun `when loaded, then the newest day is selected`() =
        test {
            givenLoaded(days = 30)

            val state = loadedState()

            assertThat(state.selectedDay?.day)
                .isEqualTo(dayLabel(29))
            assertThat(state.hasNextDay).isFalse()
        }

    @Test
    fun `when history is longer than the window, then the chart is capped`() =
        test {
            givenLoaded(days = 30)

            assertThat(loadedState().chartDays)
                .hasSize(CHART_DAYS)
        }

    @Test
    fun `when history is shorter than the window, then all of it charts`() =
        test {
            givenLoaded(days = 4)

            val state = loadedState()

            assertThat(state.chartDays).hasSize(4)
            assertThat(state.selectedChartIndex)
                .isEqualTo(3)
        }

    @Test
    fun `when stepping back, then the window holds and the highlight moves`() =
        test {
            givenLoaded(days = 30)
            val before = loadedState().chartDays

            viewModel.selectPreviousDay()
            viewModel.selectPreviousDay()

            val state = loadedState()
            assertThat(state.chartDays).isEqualTo(before)
            assertThat(state.selectedChartIndex)
                .isEqualTo(CHART_DAYS - 3)
            assertThat(state.selectedDay?.day)
                .isEqualTo(dayLabel(27))
            assertThat(state.hasNextDay).isTrue()
        }

    @Test
    fun `when at the newest day, then stepping forward does nothing`() =
        test {
            givenLoaded(days = 30)

            viewModel.selectNextDay()

            assertThat(loadedState().selectedDayIndex)
                .isEqualTo(29)
        }

    @Test
    fun `when at the oldest charted day, then stepping back does nothing`() =
        test {
            givenLoaded(days = 30)
            repeat(CHART_DAYS) { viewModel.selectPreviousDay() }

            val state = loadedState()

            // The arrows are bounded by the chart window, not the whole history.
            assertThat(state.selectedChartIndex).isEqualTo(0)
            assertThat(state.selectedDay?.day)
                .isEqualTo(dayLabel(30 - CHART_DAYS))
        }

    @Test
    fun `when the post has no history, then there is no selected day`() =
        test {
            givenLoaded(days = 0)

            val state = loadedState()

            assertThat(state.selectedDayIndex).isEqualTo(-1)
            assertThat(state.selectedDay).isNull()
            assertThat(state.previousDay).isNull()
            assertThat(state.chartDays).isEmpty()
        }

    @Test
    fun `when the oldest day is selected, then there is no previous day`() =
        test {
            givenLoaded(days = 1)

            val state = loadedState()

            assertThat(state.selectedDay).isNotNull
            assertThat(state.previousDay).isNull()
        }

    private fun loadedState() =
        viewModel.uiState.value as PostStatsDetailUiState.Loaded

    private suspend fun givenLoaded(days: Int) {
        givenFetchReturns(
            PostViewsResult.Success(
                createPostViewsData(days)
            )
        )
        viewModel.loadData(TEST_POST_ID)
    }

    private suspend fun givenFetchReturns(
        result: PostViewsResult
    ) {
        whenever(
            statsRepository.fetchPostViews(
                TEST_SITE_ID,
                TEST_POST_ID
            )
        ).thenReturn(result)
    }

    private fun createPostViewsData(days: Int) =
        PostViewsData(
            postId = TEST_POST_ID,
            totalViews = 100L,
            dailyViews = (0 until days).map { index ->
                PostViewsDailyView(
                    day = dayLabel(index),
                    views = index.toLong()
                )
            },
            weeks = emptyList(),
            years = emptyList(),
            averages = emptyList(),
            post = null
        )

    private fun dayLabel(index: Int) =
        "2026-08-%02d".format(index + 1)

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_POST_ID = 42L
        private const val TEST_ACCESS_TOKEN =
            "test_access_token"
        private const val ERROR = "error"

        // Mirrors the window the view model charts.
        private const val CHART_DAYS = 14
    }
}
