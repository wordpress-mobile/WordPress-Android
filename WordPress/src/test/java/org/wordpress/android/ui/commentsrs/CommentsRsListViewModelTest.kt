package org.wordpress.android.ui.commentsrs

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource.RsCommentListItem
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource.RsCommentsPageResult
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.WPAvatarUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import uniffi.wp_api.CommentListParams
import java.util.Date

@ExperimentalCoroutinesApi
class CommentsRsListViewModelTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var commentsRsDataSource: CommentsRsDataSource
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper
    @Mock lateinit var avatarUtilsWrapper: WPAvatarUtilsWrapper

    private lateinit var site: SiteModel
    private var activeViewModel: CommentsRsListViewModel? = null

    @Before
    fun setUp() = test {
        site = SiteModel().apply {
            id = 1
            siteId = 123L
        }
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(resourceProvider.getString(any())).thenReturn("string")
        whenever(dateTimeUtilsWrapper.javaDateToTimeSpan(any())).thenReturn("2 hours ago")
        whenever(avatarUtilsWrapper.rewriteAvatarUrlWithResource(any(), any())).thenAnswer { it.arguments[0] }
        whenever(commentsRsDataSource.firstPageParams(any(), anyOrNull())).thenReturn(FIRST_PAGE)
        whenever(commentsRsDataSource.fetchPostTitles(any(), any())).thenReturn(emptyMap())
    }

    @After
    fun tearDown() {
        activeViewModel?.viewModelScope?.cancel()
        activeViewModel = null
    }

    private fun createViewModel() = CommentsRsListViewModel(
        selectedSiteRepository = selectedSiteRepository,
        commentsRsDataSource = commentsRsDataSource,
        resourceProvider = resourceProvider,
        networkUtilsWrapper = networkUtilsWrapper,
        dateTimeUtilsWrapper = dateTimeUtilsWrapper,
        avatarUtilsWrapper = avatarUtilsWrapper,
        bgDispatcher = testDispatcher()
    ).also { activeViewModel = it }

    private suspend fun givenPage(
        comments: List<RsCommentListItem>,
        nextPageParams: CommentListParams? = null
    ) {
        whenever(commentsRsDataSource.fetchCommentsPage(eq(site), any()))
            .thenReturn(RsCommentsPageResult.Success(comments, nextPageParams))
    }

    @Test
    fun `when no site selected, emits ShowToast and Finish`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        val viewModel = createViewModel()

        viewModel.events.test {
            val first = awaitItem()
            assertThat(first).isInstanceOf(CommentsRsListEvent.ShowToast::class.java)
            assertThat((first as CommentsRsListEvent.ShowToast).messageResId).isEqualTo(R.string.blog_not_found)
            assertThat(awaitItem()).isEqualTo(CommentsRsListEvent.Finish)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initTab loads the first page and maps rows`() = test {
        givenPage(listOf(rsItem(id = 1), rsItem(id = 2)), nextPageParams = NEXT_PAGE)
        val viewModel = createViewModel()

        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()

        val state = viewModel.tabStates.value.getValue(CommentsRsListTab.ALL)
        assertThat(state.isLoading).isFalse()
        assertThat(state.comments).hasSize(2)
        assertThat(state.comments.first().remoteCommentId).isEqualTo(1)
        assertThat(state.comments.first().authorName).isEqualTo("Jane")
        assertThat(state.comments.first().snippet).isEqualTo("hello")
        assertThat(state.canLoadMore).isTrue()
    }

    @Test
    fun `initTab passes the tab's query status to the data source`() = test {
        givenPage(emptyList())
        val viewModel = createViewModel()

        viewModel.initTab(CommentsRsListTab.APPROVED)
        advanceUntilIdle()

        verify(commentsRsDataSource).firstPageParams(eq(CommentsRsListTab.APPROVED.queryStatus), anyOrNull())
    }

    @Test
    fun `initTab is a no-op when the tab is already initialized`() = test {
        givenPage(emptyList())
        val viewModel = createViewModel()

        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()
        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()

        verify(commentsRsDataSource, times(1)).fetchCommentsPage(eq(site), any())
    }

    @Test
    fun `initTab failure with no content shows the error state`() = test {
        whenever(commentsRsDataSource.fetchCommentsPage(eq(site), any()))
            .thenReturn(RsCommentsPageResult.Error("server said no"))
        val viewModel = createViewModel()

        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()

        val state = viewModel.tabStates.value.getValue(CommentsRsListTab.ALL)
        assertThat(state.error).isEqualTo("server said no")
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `loadMore appends the next page and dedupes by comment id`() = test {
        givenPage(listOf(rsItem(id = 1), rsItem(id = 2)), nextPageParams = NEXT_PAGE)
        val viewModel = createViewModel()
        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()
        givenPage(listOf(rsItem(id = 2), rsItem(id = 3)), nextPageParams = null)

        viewModel.loadMore(CommentsRsListTab.ALL)
        advanceUntilIdle()

        val state = viewModel.tabStates.value.getValue(CommentsRsListTab.ALL)
        assertThat(state.comments.map { it.remoteCommentId }).containsExactly(1L, 2L, 3L)
        assertThat(state.canLoadMore).isFalse()
    }

    @Test
    fun `loadMore is a no-op when there is no next page`() = test {
        givenPage(listOf(rsItem(id = 1)), nextPageParams = null)
        val viewModel = createViewModel()
        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()

        viewModel.loadMore(CommentsRsListTab.ALL)
        advanceUntilIdle()

        verify(commentsRsDataSource, times(1)).fetchCommentsPage(eq(site), any())
    }

    @Test
    fun `refresh failure keeps the current comments and offers retry`() = test {
        givenPage(listOf(rsItem(id = 1)))
        val viewModel = createViewModel()
        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()
        whenever(commentsRsDataSource.fetchCommentsPage(eq(site), any()))
            .thenReturn(RsCommentsPageResult.Error("boom"))

        viewModel.snackbarMessages.test {
            viewModel.refreshTab(CommentsRsListTab.ALL, isUserRefresh = true)
            advanceUntilIdle()

            val snackbar = awaitItem()
            assertThat(snackbar.message).isEqualTo("boom")
            assertThat(snackbar.onAction != null).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
        val state = viewModel.tabStates.value.getValue(CommentsRsListTab.ALL)
        assertThat(state.comments).hasSize(1)
        assertThat(state.error).isNull()
    }

    @Test
    fun `refreshAllTabs refreshes only initialized tabs`() = test {
        givenPage(listOf(rsItem(id = 1)))
        val viewModel = createViewModel()
        viewModel.initTab(CommentsRsListTab.ALL)
        viewModel.initTab(CommentsRsListTab.SPAM)
        advanceUntilIdle()

        viewModel.refreshAllTabs()
        advanceUntilIdle()

        // 2 init fetches + 2 refresh fetches, nothing for the 3 uninitialized tabs
        verify(commentsRsDataSource, times(4)).fetchCommentsPage(eq(site), any())
    }

    @Test
    fun `onCommentClick emits OpenCommentDetail`() = test {
        givenPage(listOf(rsItem(id = 42)))
        val viewModel = createViewModel()
        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onCommentClick(42L)
            val event = awaitItem()
            assertThat(event).isInstanceOf(CommentsRsListEvent.OpenCommentDetail::class.java)
            assertThat((event as CommentsRsListEvent.OpenCommentDetail).remoteCommentId).isEqualTo(42L)
            assertThat(event.site).isEqualTo(site)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `post titles are resolved in a batch and applied to rows`() = test {
        givenPage(listOf(rsItem(id = 1, postId = 10), rsItem(id = 2, postId = 20)))
        whenever(commentsRsDataSource.fetchPostTitles(site, listOf(10L, 20L)))
            .thenReturn(mapOf(10L to "First post", 20L to "Second post"))
        val viewModel = createViewModel()

        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()

        val state = viewModel.tabStates.value.getValue(CommentsRsListTab.ALL)
        assertThat(state.comments.map { it.postTitle }).containsExactly("First post", "Second post")
    }

    private fun rsItem(id: Long, postId: Long = 99L) = RsCommentListItem(
        remoteCommentId = id,
        authorName = "Jane",
        authorAvatarUrl = "https://example.com/avatar.png",
        dateGmt = Date(0),
        contentHtml = "<p>hello</p>",
        postId = postId,
        status = APPROVED
    )

    companion object {
        private val FIRST_PAGE = CommentListParams()
        private val NEXT_PAGE = CommentListParams(page = 2u)
    }
}
