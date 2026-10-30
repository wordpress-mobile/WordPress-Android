package org.wordpress.android.ui.newstats.poststats

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.LikeModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.engagement.GetLikesUseCase
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState
import org.wordpress.android.ui.engagement.GetLikesUseCase.PagingInfo
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.datasource.PostViewsDailyView
import org.wordpress.android.ui.newstats.datasource.PostViewsData
import org.wordpress.android.ui.newstats.datasource.PostViewsPost
import org.wordpress.android.ui.newstats.repository.PostViewsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.utils.UiString.UiStringText
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

    @Mock
    private lateinit var getLikesUseCase: GetLikesUseCase

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
        lenient().`when`(
            resourceProvider.getString(
                R.string.stats_error_unknown
            )
        ).thenReturn(ERROR)
        viewModel = PostStatsDetailViewModel(
            selectedSiteRepository,
            statsRepository,
            accountStore,
            resourceProvider,
            getLikesUseCase
        )
    }

    @Test
    fun `when there is no post id, then nothing is fetched`() =
        test {
            viewModel.loadData(NO_POST_ID)

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    PostStatsDetailUiState.Error::class.java
                )
            verify(statsRepository, never())
                .fetchPostViews(any(), any())
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
            assertThat(state.hasPreviousDay).isTrue()
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
            // History continues past the window, so a previous day exists but is out of reach.
            assertThat(state.previousDay).isNotNull
            assertThat(state.hasPreviousDay).isFalse()
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
            assertThat(state.hasPreviousDay).isFalse()
            assertThat(state.hasNextDay).isFalse()
        }

    private fun loadedState() =
        viewModel.uiState.value as PostStatsDetailUiState.Loaded

    @Test
    fun `when the item has likes, then their faces are added to the loaded state`() =
        test {
            val post = createPost(likeCount = 46L)
            whenever(
                getLikesUseCase.getLikesForPost(any(), any())
            ).thenReturn(
                flowOf(
                    GetLikesState.LikesData(
                        likes = listOf(
                            createLike("avatar-1", "liker one"),
                            createLike("avatar-2", "liker two")
                        ),
                        expectedNumLikes = 46,
                        pageInfo = PagingInfo(
                            pageLength = 10,
                            page = 1
                        )
                    )
                )
            )
            givenFetchReturns(
                PostViewsResult.Success(
                    createPostViewsData(1).copy(post = post)
                )
            )

            viewModel.loadData(TEST_POST_ID)

            assertThat(loadedState().likerAvatarUrls)
                .containsExactly("avatar-1", "avatar-2")
        }

    @Test
    fun `when a liker has no avatar, then no face is drawn for them`() =
        test {
            whenever(
                getLikesUseCase.getLikesForPost(any(), any())
            ).thenReturn(
                flowOf(
                    GetLikesState.LikesData(
                        likes = listOf(
                            createLike("", "no avatar"),
                            createLike("avatar-2", "liker two")
                        ),
                        expectedNumLikes = 2,
                        pageInfo = PagingInfo(
                            pageLength = 10,
                            page = 1
                        )
                    )
                )
            )
            givenFetchReturns(
                PostViewsResult.Success(
                    createPostViewsData(1)
                        .copy(post = createPost(likeCount = 2L))
                )
            )

            viewModel.loadData(TEST_POST_ID)

            assertThat(loadedState().likerAvatarUrls)
                .containsExactly("avatar-2")
        }

    @Test
    fun `when the item has no likes, then the likers are not fetched`() =
        test {
            givenFetchReturns(
                PostViewsResult.Success(
                    createPostViewsData(1)
                        .copy(post = createPost(likeCount = 0L))
                )
            )

            viewModel.loadData(TEST_POST_ID)

            verify(getLikesUseCase, never())
                .getLikesForPost(any(), any())
            assertThat(loadedState().likerAvatarUrls)
                .isEmpty()
        }

    @Test
    fun `when the likers request fails, then whatever was cached is still shown`() =
        test {
            whenever(
                getLikesUseCase.getLikesForPost(any(), any())
            ).thenReturn(
                flowOf(
                    GetLikesState.Failure(
                        failureType = GetLikesUseCase
                            .FailureType.GENERIC,
                        error = UiStringText(ERROR),
                        cachedLikes = listOf(
                            createLike("avatar-1", "liker one")
                        ),
                        emptyStateData = GetLikesState.Failure
                            .EmptyStateData(false),
                        expectedNumLikes = 46,
                        hasMore = false,
                        pageInfo = PagingInfo(
                            pageLength = 10,
                            page = 1
                        )
                    )
                )
            )
            givenFetchReturns(
                PostViewsResult.Success(
                    createPostViewsData(1)
                        .copy(post = createPost(likeCount = 46L))
                )
            )

            viewModel.loadData(TEST_POST_ID)

            assertThat(loadedState().likerAvatarUrls)
                .containsExactly("avatar-1")
        }

    private fun createPost(likeCount: Long) = PostViewsPost(
        title = "A post",
        date = "2026-08-01 10:00:00",
        likeCount = likeCount,
        commentCount = 0L,
        url = "https://example.com/a-post",
        type = "post",
        status = "publish"
    )

    private fun createLike(avatarUrl: String, name: String) =
        LikeModel().apply {
            likerAvatarUrl = avatarUrl
            likerName = name
        }

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
