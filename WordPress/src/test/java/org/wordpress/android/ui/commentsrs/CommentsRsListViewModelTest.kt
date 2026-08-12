package org.wordpress.android.ui.commentsrs

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.SPAM
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource.RsComment
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource.RsCommentsPageResult
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource.RsResult
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.items.listitem.SiteCapabilityChecker
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.WPAvatarUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import uniffi.wp_api.CommentListParams
import uniffi.wp_api.RequestExecutionErrorReason
import uniffi.wp_api.WpErrorCode
import java.util.Date

@ExperimentalCoroutinesApi
class CommentsRsListViewModelTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var commentsRsDataSource: CommentsRsDataSource
    @Mock lateinit var siteCapabilityChecker: SiteCapabilityChecker
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var avatarUtilsWrapper: WPAvatarUtilsWrapper
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private lateinit var site: SiteModel
    private lateinit var commentBrowsingSession: CommentBrowsingSession
    private var activeViewModel: CommentsRsListViewModel? = null

    @Before
    fun setUp() = test {
        site = SiteModel().apply {
            id = 1
            siteId = 123L
        }
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(siteCapabilityChecker.canModerateComments(site)).thenReturn(true)
        whenever(resourceProvider.getString(any())).thenReturn("string")
        // Fetches short-circuit when offline, so the default has to be a connected device.
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(avatarUtilsWrapper.rewriteAvatarUrlWithResource(any(), any())).thenAnswer { it.arguments[0] }
        whenever(commentsRsDataSource.firstPageParams(any(), anyOrNull())).thenReturn(FIRST_PAGE)
        whenever(commentsRsDataSource.fetchPostTitles(any(), any())).thenReturn(emptyMap())
        commentBrowsingSession = CommentBrowsingSession(commentsRsDataSource)
    }

    @After
    fun tearDown() {
        activeViewModel?.viewModelScope?.cancel()
        activeViewModel = null
    }

    private fun createViewModel() = CommentsRsListViewModel(
        selectedSiteRepository = selectedSiteRepository,
        commentsRsDataSource = commentsRsDataSource,
        siteCapabilityChecker = siteCapabilityChecker,
        resourceProvider = resourceProvider,
        networkUtilsWrapper = networkUtilsWrapper,
        avatarUtilsWrapper = avatarUtilsWrapper,
        analyticsTracker = analyticsTracker,
        commentBrowsingSession = commentBrowsingSession,
        bgDispatcher = testDispatcher()
    ).also { activeViewModel = it }

    private suspend fun givenPage(
        comments: List<RsComment>,
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
    fun `no-site initTab and refreshTab bail without registering a tab, fetching, or crashing`() = test {
        // The Compose pager fires its init effect while init() races its async Finish. initTab must
        // bail before registering the tab (no stranded spinner), and a pull-to-refresh landing in
        // the same window must not reach clearPostTitles(site) — the requireNotNull this guards.
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)
        val viewModel = createViewModel()

        viewModel.initTab(CommentsRsListTab.ALL)
        viewModel.refreshTab(CommentsRsListTab.ALL, isUserRefresh = true)
        advanceUntilIdle()

        assertThat(viewModel.tabStates.value).isEmpty()
        verify(commentsRsDataSource, never()).clearPostTitles(any())
        verify(commentsRsDataSource, never()).fetchCommentsPage(any(), any())
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
    fun `initTab offline with no content shows the network message`() = test {
        // The rs client reports offline as a result variant rather than a thrown exception, so
        // the reason has to survive the trip out of the data source for this to be classified.
        whenever(resourceProvider.getString(R.string.error_generic_network)).thenReturn("no network")
        whenever(commentsRsDataSource.fetchCommentsPage(eq(site), any())).thenReturn(
            RsCommentsPageResult.Error(
                message = null,
                reason = RequestExecutionErrorReason.DeviceIsOfflineError(errorMessage = "off")
            )
        )
        val viewModel = createViewModel()

        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()

        val state = viewModel.tabStates.value.getValue(CommentsRsListTab.ALL)
        assertThat(state.error).isEqualTo("no network")
        assertThat(state.isAuthError).isFalse()
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `initTab offline shows the error without hitting the network`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        whenever(resourceProvider.getString(R.string.error_generic_network)).thenReturn("no network")
        val viewModel = createViewModel()

        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()

        verify(commentsRsDataSource, never()).fetchCommentsPage(any(), any())
        val state = viewModel.tabStates.value.getValue(CommentsRsListTab.ALL)
        assertThat(state.error).isEqualTo("no network")
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `initTab auth failure carried as a WP error envelope is still an auth failure`() = test {
        // A rejected credential can come back either as a transport reason or, when the server
        // answers with a WP error body, as an error code. Only checking the reason let a 401 keep
        // its Retry button and show the server's raw wording.
        whenever(resourceProvider.getString(R.string.post_rs_error_auth)).thenReturn("check login")
        whenever(commentsRsDataSource.fetchCommentsPage(eq(site), any())).thenReturn(
            RsCommentsPageResult.Error(
                message = "Sorry, you are not allowed to do that.",
                errorCode = WpErrorCode.Unauthorized()
            )
        )
        val viewModel = createViewModel()

        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()

        val state = viewModel.tabStates.value.getValue(CommentsRsListTab.ALL)
        assertThat(state.isAuthError).isTrue()
        assertThat(state.error).isEqualTo("check login")
    }

    @Test
    fun `initTab auth failure flags the tab so the retry is withheld`() = test {
        whenever(commentsRsDataSource.fetchCommentsPage(eq(site), any())).thenReturn(
            RsCommentsPageResult.Error(
                message = null,
                reason = RequestExecutionErrorReason.HttpAuthenticationRejectedError(
                    hostname = "example.com",
                    method = null
                )
            )
        )
        val viewModel = createViewModel()

        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()

        // Retrying rejected credentials just fails again, so the screen drops the retry button.
        assertThat(viewModel.tabStates.value.getValue(CommentsRsListTab.ALL).isAuthError).isTrue()
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
    fun `stale loadMore result arriving after a silent refresh is discarded`() = test {
        givenPage(listOf(rsItem(id = 1), rsItem(id = 2)), nextPageParams = NEXT_PAGE)
        val viewModel = createViewModel()
        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()
        // Next two fetches: the silent refresh gets a fresh first page; the concurrently
        // launched loadMore gets the (now stale) old second page.
        whenever(commentsRsDataSource.fetchCommentsPage(eq(site), any())).thenReturn(
            RsCommentsPageResult.Success(listOf(rsItem(id = 10), rsItem(id = 11)), NEXT_PAGE),
            RsCommentsPageResult.Success(listOf(rsItem(id = 3), rsItem(id = 4)), null)
        )

        viewModel.refreshTab(CommentsRsListTab.ALL) // silent: sets no busy flag
        viewModel.loadMore(CommentsRsListTab.ALL)
        advanceUntilIdle()

        val state = viewModel.tabStates.value.getValue(CommentsRsListTab.ALL)
        assertThat(state.comments.map { it.remoteCommentId }).containsExactly(10L, 11L)
        assertThat(state.isLoadingMore).isFalse()
        assertThat(state.canLoadMore).isTrue()
    }

    @Test
    fun `loadMore failure offers a retry snackbar`() = test {
        givenPage(listOf(rsItem(id = 1)), nextPageParams = NEXT_PAGE)
        val viewModel = createViewModel()
        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()
        whenever(commentsRsDataSource.fetchCommentsPage(eq(site), any()))
            .thenReturn(RsCommentsPageResult.Error("boom"))

        viewModel.snackbarMessages.test {
            viewModel.loadMore(CommentsRsListTab.ALL)
            advanceUntilIdle()

            val snackbar = awaitItem()
            assertThat(snackbar.message).isEqualTo("boom")
            assertThat(snackbar.onAction != null).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(viewModel.tabStates.value.getValue(CommentsRsListTab.ALL).isLoadingMore).isFalse()
    }

    @Test
    fun `fully deduplicated page auto-advances to the next page`() = test {
        givenPage(listOf(rsItem(id = 1), rsItem(id = 2)), nextPageParams = NEXT_PAGE)
        val viewModel = createViewModel()
        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()
        // The next page duplicates the current rows entirely (server-side shift), then the
        // page after that has the genuinely new comment.
        whenever(commentsRsDataSource.fetchCommentsPage(eq(site), any())).thenReturn(
            RsCommentsPageResult.Success(listOf(rsItem(id = 1), rsItem(id = 2)), THIRD_PAGE),
            RsCommentsPageResult.Success(listOf(rsItem(id = 3)), null)
        )

        viewModel.loadMore(CommentsRsListTab.ALL)
        advanceUntilIdle()

        val state = viewModel.tabStates.value.getValue(CommentsRsListTab.ALL)
        assertThat(state.comments.map { it.remoteCommentId }).containsExactly(1L, 2L, 3L)
        assertThat(state.canLoadMore).isFalse()
    }

    @Test
    fun `an empty page with a remaining cursor auto-advances to the next page`() = test {
        givenPage(listOf(rsItem(id = 1), rsItem(id = 2)), nextPageParams = NEXT_PAGE)
        val viewModel = createViewModel()
        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()
        // A middle page can come back empty (comments deleted server-side) while the
        // headers still advertise more pages.
        whenever(commentsRsDataSource.fetchCommentsPage(eq(site), any())).thenReturn(
            RsCommentsPageResult.Success(emptyList(), THIRD_PAGE),
            RsCommentsPageResult.Success(listOf(rsItem(id = 3)), null)
        )

        viewModel.loadMore(CommentsRsListTab.ALL)
        advanceUntilIdle()

        val state = viewModel.tabStates.value.getValue(CommentsRsListTab.ALL)
        assertThat(state.comments.map { it.remoteCommentId }).containsExactly(1L, 2L, 3L)
        assertThat(state.canLoadMore).isFalse()
    }

    @Test
    fun `auto-advance is capped when pages keep adding nothing`() = test {
        givenPage(listOf(rsItem(id = 1)), nextPageParams = NEXT_PAGE)
        val viewModel = createViewModel()
        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()
        // Pathological cursor chain: every page dedupes away with a cursor still present.
        whenever(commentsRsDataSource.fetchCommentsPage(eq(site), any()))
            .thenReturn(RsCommentsPageResult.Success(listOf(rsItem(id = 1)), NEXT_PAGE))

        viewModel.loadMore(CommentsRsListTab.ALL)
        advanceUntilIdle()

        // 1 init + 1 user loadMore + at most 3 auto-advances, then the chain stops.
        verify(commentsRsDataSource, times(5)).fetchCommentsPage(eq(site), any())
        val state = viewModel.tabStates.value.getValue(CommentsRsListTab.ALL)
        assertThat(state.isLoadingMore).isFalse()
    }

    @Test
    fun `a user refresh retries cached post titles, a silent refresh does not`() = test {
        givenPage(listOf(rsItem(id = 1)))
        val viewModel = createViewModel()
        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()

        viewModel.refreshTab(CommentsRsListTab.ALL) // silent
        advanceUntilIdle()
        verify(commentsRsDataSource, never()).clearPostTitles(any())

        viewModel.refreshTab(CommentsRsListTab.ALL, isUserRefresh = true)
        advanceUntilIdle()
        verify(commentsRsDataSource).clearPostTitles(site)
    }

    @Test
    fun `a user refresh cancels an in-flight title resolve and fetches fresh titles`() = test {
        givenPage(listOf(rsItem(id = 1, postId = 10)))
        // The first resolve hangs mid-flight (holding pre-refresh results); the retry after
        // the user refresh returns the fresh title.
        whenever(commentsRsDataSource.fetchPostTitles(eq(site), any()))
            .doSuspendableAnswer { delay(60_000); emptyMap() }
            .thenReturn(mapOf(10L to "Fresh title"))
        val viewModel = createViewModel()
        viewModel.initTab(CommentsRsListTab.ALL)
        runCurrent() // page applied; resolve job now suspended mid-flight

        viewModel.refreshTab(CommentsRsListTab.ALL, isUserRefresh = true)
        advanceUntilIdle()

        // Without the cancel, the identical-ids guard defers to the hung pre-refresh job and
        // the fresh title is never fetched.
        val state = viewModel.tabStates.value.getValue(CommentsRsListTab.ALL)
        assertThat(state.comments.first().postTitle).isEqualTo("Fresh title")
        verify(commentsRsDataSource, times(2)).fetchPostTitles(eq(site), any())
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
    fun `refreshTab is a no-op while another first-page fetch is in flight`() = test {
        givenPage(listOf(rsItem(id = 1)))
        val viewModel = createViewModel()
        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()
        whenever(commentsRsDataSource.fetchCommentsPage(eq(site), any()))
            .doSuspendableAnswer { delay(60_000); RsCommentsPageResult.Success(emptyList(), null) }

        viewModel.refreshTab(CommentsRsListTab.ALL) // suspends mid-flight
        runCurrent()
        viewModel.refreshTab(CommentsRsListTab.ALL, isUserRefresh = true) // overlaps: skipped
        runCurrent()

        // 1 init + 1 first refresh; the overlapping refresh didn't fire a duplicate request
        verify(commentsRsDataSource, times(2)).fetchCommentsPage(eq(site), any())
    }

    @Test
    fun `a silent refresh failure on a populated tab shows no snackbar`() = test {
        givenPage(listOf(rsItem(id = 1)))
        val viewModel = createViewModel()
        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()
        whenever(commentsRsDataSource.fetchCommentsPage(eq(site), any()))
            .thenReturn(RsCommentsPageResult.Error("boom"))

        viewModel.snackbarMessages.test {
            viewModel.refreshTab(CommentsRsListTab.ALL) // silent
            advanceUntilIdle()

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        val state = viewModel.tabStates.value.getValue(CommentsRsListTab.ALL)
        assertThat(state.comments).hasSize(1)
        assertThat(state.error).isNull()
    }

    @Test
    fun `a refresh landing while a loadMore is in flight clears isLoadingMore`() = test {
        givenPage(listOf(rsItem(id = 1)), nextPageParams = NEXT_PAGE)
        val viewModel = createViewModel()
        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()
        // The loadMore hangs mid-flight; the silent refresh completes immediately.
        whenever(commentsRsDataSource.fetchCommentsPage(eq(site), any()))
            .doSuspendableAnswer { delay(60_000); RsCommentsPageResult.Success(emptyList(), null) }
            .thenReturn(RsCommentsPageResult.Success(listOf(rsItem(id = 10)), null))

        viewModel.loadMore(CommentsRsListTab.ALL)
        runCurrent() // loadMore suspended, isLoadingMore = true
        viewModel.refreshTab(CommentsRsListTab.ALL)
        runCurrent() // refresh applied while the loadMore is still in flight

        // The replacing page must clear the flag itself — the stale loadMore result is
        // discarded by the generation check, so nothing else resets it promptly.
        val state = viewModel.tabStates.value.getValue(CommentsRsListTab.ALL)
        assertThat(state.isLoadingMore).isFalse()
        assertThat(state.comments.map { it.remoteCommentId }).containsExactly(10L)
    }

    @Test
    fun `tab changes are tracked once per change with the filter property`() = test {
        whenever(resourceProvider.getString(CommentsRsListTab.ALL.trackingLabelResId)).thenReturn("All")
        whenever(resourceProvider.getString(CommentsRsListTab.SPAM.trackingLabelResId)).thenReturn("Spam")
        val viewModel = createViewModel()

        viewModel.onTabChanged(CommentsRsListTab.ALL)
        viewModel.onTabChanged(CommentsRsListTab.ALL) // repeat: not tracked again
        viewModel.onTabChanged(CommentsRsListTab.SPAM)

        verify(analyticsTracker, times(1))
            .track(Stat.COMMENT_FILTER_CHANGED, mapOf("selected_filter" to "All"))
        verify(analyticsTracker, times(1))
            .track(Stat.COMMENT_FILTER_CHANGED, mapOf("selected_filter" to "Spam"))
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

    @Test
    fun `canModerate reflects the capability checker`() = test {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.canModerate.value).isTrue()
    }

    @Test
    fun `batch moderation applies the new status to the selection when the user can moderate`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(commentsRsDataSource.updateStatus(eq(site), any(), any())).thenReturn(RsResult.Success)
        givenPage(listOf(rsItem(id = 1)), nextPageParams = null)
        val viewModel = createViewModel()
        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()

        viewModel.onCommentLongClick(1)
        viewModel.onBatchAction(CommentsRsBatchAction.SPAM, CommentsRsListTab.ALL)
        advanceUntilIdle()

        verify(commentsRsDataSource).updateStatus(site, 1, SPAM)
    }

    @Test
    fun `batch moderation is a no-op without the moderate capability`() = test {
        // No network stub needed: the capability guard short-circuits before the network check.
        whenever(siteCapabilityChecker.canModerateComments(site)).thenReturn(false)
        givenPage(listOf(rsItem(id = 1)), nextPageParams = null)
        val viewModel = createViewModel()
        viewModel.initTab(CommentsRsListTab.ALL)
        advanceUntilIdle()

        viewModel.onCommentLongClick(1)
        viewModel.onBatchAction(CommentsRsBatchAction.SPAM, CommentsRsListTab.ALL)
        advanceUntilIdle()

        assertThat(viewModel.canModerate.value).isFalse()
        verify(commentsRsDataSource, never()).updateStatus(eq(site), any(), any())
    }

    @Test
    fun `unreplied tab shows only top-level comments the current user has not replied to`() = test {
        whenever(siteCapabilityChecker.currentUserId(site)).thenReturn(ME)
        // id=1 top-level by someone else, answered by me (id=2) -> dropped.
        // id=2 is my reply -> never a candidate. id=3 top-level by someone else, unanswered -> kept.
        givenPage(
            listOf(
                rsItem(id = 1, authorId = OTHER),
                rsItem(id = 2, authorId = ME, parentId = 1),
                rsItem(id = 3, authorId = OTHER)
            )
        )
        val viewModel = createViewModel()

        viewModel.initTab(CommentsRsListTab.UNREPLIED)
        advanceUntilIdle()

        val state = viewModel.tabStates.value.getValue(CommentsRsListTab.UNREPLIED)
        assertThat(state.comments.map { it.remoteCommentId }).containsExactly(3L)
    }

    @Test
    fun `unreplied tab over-fetches a larger page`() = test {
        whenever(siteCapabilityChecker.currentUserId(site)).thenReturn(ME)
        givenPage(emptyList())
        val viewModel = createViewModel()

        viewModel.initTab(CommentsRsListTab.UNREPLIED)
        advanceUntilIdle()

        val params = argumentCaptor<CommentListParams>()
        verify(commentsRsDataSource).fetchCommentsPage(eq(site), params.capture())
        assertThat(params.firstValue.perPage).isEqualTo(100u)
    }

    @Test
    fun `unreplied tab pages past a first page that filters to nothing`() = test {
        whenever(siteCapabilityChecker.currentUserId(site)).thenReturn(ME)
        // Page 1 is 100 raw comments that all thread away (here: authored by me), but a next page
        // exists; page 2 carries the actual unreplied comment. Without auto-advancing, the tab
        // would sit on a false empty state and never reach it.
        whenever(commentsRsDataSource.fetchCommentsPage(eq(site), any())).thenReturn(
            RsCommentsPageResult.Success(listOf(rsItem(id = 1, authorId = ME)), NEXT_PAGE),
            RsCommentsPageResult.Success(listOf(rsItem(id = 2, authorId = OTHER)), null)
        )
        val viewModel = createViewModel()

        viewModel.initTab(CommentsRsListTab.UNREPLIED)
        advanceUntilIdle()

        val state = viewModel.tabStates.value.getValue(CommentsRsListTab.UNREPLIED)
        assertThat(state.comments.map { it.remoteCommentId }).containsExactly(2L)
    }

    @Test
    fun `unreplied load-more keeps paging when a page threads the visible list down to empty`() = test {
        whenever(siteCapabilityChecker.currentUserId(site)).thenReturn(ME)
        // Page 1: one visible unreplied comment (A). Page 2: the user's reply to A and nothing new,
        // so filtering drops A and the visible count shrinks 1 -> 0. Page 3 carries a real unreplied
        // comment. Without advancing on the shrink, the tab would sit on a false empty state.
        whenever(commentsRsDataSource.fetchCommentsPage(eq(site), any())).thenReturn(
            RsCommentsPageResult.Success(listOf(rsItem(id = 1, authorId = OTHER)), NEXT_PAGE),
            RsCommentsPageResult.Success(listOf(rsItem(id = 2, authorId = ME, parentId = 1)), THIRD_PAGE),
            RsCommentsPageResult.Success(listOf(rsItem(id = 3, authorId = OTHER)), null)
        )
        val viewModel = createViewModel()
        viewModel.initTab(CommentsRsListTab.UNREPLIED)
        advanceUntilIdle()

        viewModel.loadMore(CommentsRsListTab.UNREPLIED)
        advanceUntilIdle()

        val state = viewModel.tabStates.value.getValue(CommentsRsListTab.UNREPLIED)
        assertThat(state.comments.map { it.remoteCommentId }).containsExactly(3L)
    }

    @Test
    fun `tapping an unreplied comment seeds the swipe session without a paging cursor`() = test {
        whenever(siteCapabilityChecker.currentUserId(site)).thenReturn(ME)
        givenPage(listOf(rsItem(id = 1, authorId = OTHER)), nextPageParams = NEXT_PAGE)
        val viewModel = createViewModel()
        viewModel.onTabChanged(CommentsRsListTab.UNREPLIED)
        viewModel.initTab(CommentsRsListTab.UNREPLIED)
        advanceUntilIdle()

        viewModel.onCommentClick(1L)

        // A cursor would let the detail pager page the raw stream into the replied-to and own
        // comments the tab filters out; Unreplied must seed only the visible ids.
        assertThat(commentBrowsingSession.canLoadMore).isFalse()
    }

    private fun rsItem(id: Long, postId: Long = 99L, authorId: Long = 0L, parentId: Long = 0L) = RsComment(
        remoteCommentId = id,
        authorId = authorId,
        parentId = parentId,
        authorName = "Jane",
        authorAvatarUrl = "https://example.com/avatar.png",
        dateGmt = Date(0),
        contentHtml = "<p>hello</p>",
        url = "https://example.com/post/#comment-$id",
        postId = postId,
        status = APPROVED
    )

    companion object {
        private const val ME = 7L
        private const val OTHER = 42L
        private val FIRST_PAGE = CommentListParams()
        private val NEXT_PAGE = CommentListParams(page = 2u)
        private val THIRD_PAGE = CommentListParams(page = 3u)
    }
}
