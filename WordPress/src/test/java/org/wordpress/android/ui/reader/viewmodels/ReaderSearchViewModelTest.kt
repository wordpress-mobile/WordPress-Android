package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.datasets.wrappers.ReaderSearchTableWrapper
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.ReaderSiteModel
import org.wordpress.android.fluxc.store.ReaderStore
import org.wordpress.android.fluxc.store.ReaderStore.OnReaderSitesSearched
import org.wordpress.android.fluxc.store.ReaderStore.ReaderError
import org.wordpress.android.fluxc.store.ReaderStore.ReaderErrorType
import org.wordpress.android.fluxc.store.ReaderStore.ReaderSearchSitesPayload
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener
import org.wordpress.android.ui.reader.actions.ReaderBlogActionsWrapper
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.util.NetworkUtilsWrapper

@ExperimentalCoroutinesApi
class ReaderSearchViewModelTest : BaseUnitTest() {
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var readerStore: ReaderStore
    @Mock lateinit var searchTable: ReaderSearchTableWrapper
    @Mock lateinit var blogTable: ReaderBlogTableWrapper
    @Mock lateinit var blogActions: ReaderBlogActionsWrapper
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var readerTracker: ReaderTracker

    private lateinit var viewModel: ReaderSearchViewModel

    @Before
    fun setUp() {
        whenever(searchTable.getQueries(any(), any())).thenReturn(emptyList())
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        viewModel = createViewModel()
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `shows recent queries when opened`() {
        whenever(searchTable.getQueries(null, 10)).thenReturn(listOf("cats", "dogs"))

        viewModel = createViewModel()

        assertThat(viewModel.uiState.value.suggestions).containsExactly("cats", "dogs")
        assertThat(viewModel.uiState.value.content).isEqualTo(ReaderSearchContent.SUGGESTIONS)
    }

    @Test
    fun `shows the explainer when there's no history`() {
        assertThat(viewModel.uiState.value.content).isEqualTo(ReaderSearchContent.EXPLAINER)
    }

    @Test
    fun `typing filters the suggestions by prefix`() {
        whenever(searchTable.getQueries("ca", 5)).thenReturn(listOf("cats"))

        viewModel.onQueryChanged("ca ")

        assertThat(viewModel.uiState.value.suggestions).containsExactly("cats")
    }

    @Test
    fun `submitting trims, saves and tracks the query, then searches posts and sites`() = test {
        viewModel.events.test {
            viewModel.onQueryChanged("  cats ")
            viewModel.onSearchSubmitted()

            assertThat(awaitItem()).isEqualTo(ReaderSearchEvent.SubmitPostSearch("cats"))
        }

        verify(searchTable).addOrUpdateQuery("cats")
        verify(readerTracker).trackQuery(AnalyticsTracker.Stat.READER_SEARCH_PERFORMED, "cats")
        assertThat(lastSiteSearch().searchTerm).isEqualTo("cats")
        assertThat(lastSiteSearch().offset).isEqualTo(0)
        with(viewModel.uiState.value) {
            assertThat(query).isEqualTo("cats")
            assertThat(submittedQuery).isEqualTo("cats")
            assertThat(isLoadingSites).isTrue
            assertThat(content).isEqualTo(ReaderSearchContent.RESULTS)
        }
    }

    @Test
    fun `a blank query isn't submitted`() {
        viewModel.onQueryChanged("  ")
        viewModel.onSearchSubmitted()

        verify(searchTable, never()).addOrUpdateQuery(any())
        assertThat(viewModel.uiState.value.submittedQuery).isNull()
    }

    @Test
    fun `tapping a suggestion searches for it`() {
        viewModel.onSuggestionClicked("dogs")

        assertThat(viewModel.uiState.value.submittedQuery).isEqualTo("dogs")
        verify(searchTable).addOrUpdateQuery("dogs")
    }

    @Test
    fun `emptying the field ends the search`() = test {
        submit("cats")
        viewModel.onTabSelected(ReaderSearchTab.SITES)

        viewModel.events.test {
            skipItems(1) // the submit
            viewModel.onQueryChanged("")
            assertThat(awaitItem()).isEqualTo(ReaderSearchEvent.ClearPostSearch)
        }

        with(viewModel.uiState.value) {
            assertThat(submittedQuery).isNull()
            assertThat(selectedTab).isEqualTo(ReaderSearchTab.POSTS)
            assertThat(sites).isEmpty()
        }
    }

    @Test
    fun `a blank field ends the search too`() {
        submit("cats")

        viewModel.onQueryChanged("  ")

        assertThat(viewModel.uiState.value.submittedQuery).isNull()
    }

    @Test
    fun `editing the query shows matching suggestions over the results`() {
        submit("cats")
        whenever(searchTable.getQueries("do", 5)).thenReturn(listOf("dogs"))

        viewModel.onQueryChanged("do")
        assertThat(viewModel.uiState.value.content).isEqualTo(ReaderSearchContent.SUGGESTIONS)

        viewModel.onQueryChanged("dox")
        assertThat(viewModel.uiState.value.content).isEqualTo(ReaderSearchContent.RESULTS)
    }

    @Test
    fun `site results for an older query are ignored`() {
        submit("cats")

        viewModel.onReaderSitesSearched(OnReaderSitesSearched(listOf(site(1)), "dogs", 0, true))

        assertThat(viewModel.uiState.value.sites).isEmpty()
        assertThat(viewModel.uiState.value.isLoadingSites).isTrue
    }

    @Test
    fun `site results replace on the first page and append after it`() {
        submit("cats")

        viewModel.onReaderSitesSearched(OnReaderSitesSearched(listOf(site(1), site(2)), "cats", 0, true))
        viewModel.onLoadMoreSites()
        assertThat(lastSiteSearch().offset).isEqualTo(2)
        assertThat(viewModel.uiState.value.isLoadingMoreSites).isTrue

        viewModel.onReaderSitesSearched(OnReaderSitesSearched(listOf(site(3)), "cats", 2, false))

        with(viewModel.uiState.value) {
            assertThat(sites.map { it.feedId }).containsExactly(1L, 2L, 3L)
            assertThat(canLoadMoreSites).isFalse
            assertThat(isLoadingMoreSites).isFalse
        }
    }

    @Test
    fun `a failed later page stops paging instead of retrying`() {
        submit("cats")
        viewModel.onReaderSitesSearched(OnReaderSitesSearched(listOf(site(1)), "cats", 0, true))
        viewModel.onLoadMoreSites()

        viewModel.onReaderSitesSearched(
            OnReaderSitesSearched(ReaderError(ReaderErrorType.GENERIC_ERROR, null), "cats", 1)
        )
        viewModel.onLoadMoreSites()

        with(viewModel.uiState.value) {
            assertThat(canLoadMoreSites).isFalse
            assertThat(sitesRequestFailed).isFalse
            assertThat(sites).hasSize(1)
        }
        assertThat(siteSearches()).hasSize(2)
    }

    @Test
    fun `a later page from before the search restarted is ignored`() {
        submit("cats")
        viewModel.onReaderSitesSearched(OnReaderSitesSearched(listOf(site(1), site(2)), "cats", 0, true))
        viewModel.onSearchSubmitted()

        viewModel.onReaderSitesSearched(OnReaderSitesSearched(listOf(site(3)), "cats", 2, true))

        assertThat(viewModel.uiState.value.sites).isEmpty()
        assertThat(viewModel.uiState.value.isLoadingSites).isTrue
    }

    @Test
    fun `suggestions for the previous query don't hide the results`() {
        val state = ReaderSearchUiState(
            query = "dox",
            submittedQuery = "cats",
            suggestions = listOf("dogs"),
            suggestionsQuery = "do"
        )

        assertThat(state.content).isEqualTo(ReaderSearchContent.RESULTS)
    }

    @Test
    fun `duplicate sites are dropped and paging continues from the server's offset`() {
        submit("cats")
        viewModel.onReaderSitesSearched(OnReaderSitesSearched(listOf(site(1), site(1), site(2)), "cats", 0, true))
        assertThat(viewModel.uiState.value.sites.map { it.feedId }).containsExactly(1L, 2L)

        viewModel.onLoadMoreSites()

        assertThat(lastSiteSearch().offset).isEqualTo(3)
    }

    @Test
    fun `a feed and a site with the same id get different keys`() {
        val feed = ReaderSearchSite(
            siteId = 0,
            feedId = 5,
            title = null,
            url = null,
            iconUrl = null,
            isFollowing = false
        )
        val site = feed.copy(siteId = 5, feedId = 0)

        assertThat(feed.key).isNotEqualTo(site.key)
    }

    @Test
    fun `doesn't load more sites when there are no more`() {
        submit("cats")
        viewModel.onReaderSitesSearched(OnReaderSitesSearched(listOf(site(1)), "cats", 0, false))

        viewModel.onLoadMoreSites()

        assertThat(siteSearches()).hasSize(1)
    }

    @Test
    fun `a failed site search shows the error`() {
        submit("cats")

        viewModel.onReaderSitesSearched(
            OnReaderSitesSearched(ReaderError(ReaderErrorType.GENERIC_ERROR, null), "cats", 0)
        )

        assertThat(viewModel.uiState.value.sitesRequestFailed).isTrue
        assertThat(viewModel.uiState.value.isLoadingSites).isFalse
    }

    @Test
    fun `following a site disables its button until the request returns`() {
        val site = submitWithSite(isFollowing = false)
        whenever(blogActions.followFeedById(eq(2L), eq(1L), eq(true), any(), any(), any())).thenReturn(true)

        viewModel.onSiteFollowClicked(site)
        assertThat(viewModel.uiState.value.sites.single().isFollowing).isTrue
        assertThat(viewModel.uiState.value.sites.single().isFollowEnabled).isFalse

        followListener(isAskingToFollow = true).onActionResult(true)
        assertThat(viewModel.uiState.value.sites.single().isFollowing).isTrue
        assertThat(viewModel.uiState.value.sites.single().isFollowEnabled).isTrue
    }

    @Test
    fun `a follow that fails before the request returns leaves the button enabled`() {
        val site = submitWithSite(isFollowing = false)
        whenever(blogActions.followFeedById(any(), any(), any(), any(), any(), any())).thenAnswer {
            it.getArgument<ActionListener>(3).onActionResult(false)
            true
        }

        viewModel.onSiteFollowClicked(site)

        with(viewModel.uiState.value.sites.single()) {
            assertThat(isFollowing).isFalse
            assertThat(isFollowEnabled).isTrue
        }
    }

    @Test
    fun `a failed unfollow reverts the button and shows an error`() = test {
        val site = submitWithSite(isFollowing = true)
        whenever(blogActions.followFeedById(any(), any(), eq(false), any(), any(), any())).thenReturn(true)
        viewModel.onSiteFollowClicked(site)

        viewModel.events.test {
            skipItems(1) // the submit
            followListener(isAskingToFollow = false).onActionResult(false)
            assertThat(awaitItem())
                .isEqualTo(ReaderSearchEvent.ShowToast(R.string.reader_toast_err_unable_to_unfollow_blog))
        }
        assertThat(viewModel.uiState.value.sites.single().isFollowing).isTrue
    }

    @Test
    fun `following while offline shows a message instead`() = test {
        val site = submitWithSite(isFollowing = false)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        viewModel.events.test {
            skipItems(1) // the submit
            viewModel.onSiteFollowClicked(site)
            assertThat(awaitItem()).isEqualTo(ReaderSearchEvent.ShowToast(R.string.no_network_message))
        }
        verify(blogActions, never()).followFeedById(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `returning from a site preview refreshes that site's follow status`() {
        val site = submitWithSite(isFollowing = false)
        whenever(blogTable.isSiteFollowed(2L, 1L)).thenReturn(true)

        viewModel.onSiteClicked(site)
        viewModel.onResume()

        assertThat(viewModel.uiState.value.sites.single().isFollowing).isTrue
    }

    @Test
    fun `deleting a suggestion reloads the suggestions`() {
        whenever(searchTable.getQueries(null, 10)).thenReturn(listOf("cats", "dogs"), listOf("dogs"))
        viewModel = createViewModel()

        viewModel.onSuggestionDeleteClicked("cats")

        verify(searchTable).deleteQuery("cats")
        assertThat(viewModel.uiState.value.suggestions).containsExactly("dogs")
    }

    @Test
    fun `clearing the history deletes and tracks it`() {
        viewModel.onClearHistoryConfirmed()

        verify(searchTable).deleteAllQueries()
        verify(readerTracker).track(AnalyticsTracker.Stat.READER_SEARCH_HISTORY_CLEARED)
    }

    @Test
    fun `reconnecting re-runs the current search`() {
        submit("cats")

        viewModel.onConnectionRestored()

        assertThat(siteSearches()).hasSize(2)
    }

    @Test
    fun `a restored search fetches its sites again without saving or tracking it`() {
        val savedState = SavedStateHandle(
            mapOf("query" to "cats", "submitted_query" to "cats", "selected_tab" to ReaderSearchTab.SITES)
        )

        viewModel = createViewModel(savedState)

        assertThat(viewModel.uiState.value.selectedTab).isEqualTo(ReaderSearchTab.SITES)
        assertThat(lastSiteSearch().searchTerm).isEqualTo("cats")
        verify(searchTable, never()).addOrUpdateQuery(any())
        verify(readerTracker, never()).trackQuery(any(), any())
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) = ReaderSearchViewModel(
        savedStateHandle,
        dispatcher,
        readerStore,
        searchTable,
        blogTable,
        blogActions,
        networkUtilsWrapper,
        readerTracker,
        testDispatcher(),
        testDispatcher()
    )

    private fun submit(query: String) {
        viewModel.onQueryChanged(query)
        viewModel.onSearchSubmitted()
    }

    private fun submitWithSite(isFollowing: Boolean): ReaderSearchSite {
        submit("cats")
        viewModel.onReaderSitesSearched(
            OnReaderSitesSearched(listOf(site(1, isFollowing = isFollowing)), "cats", 0, false)
        )
        return viewModel.uiState.value.sites.single()
    }

    private fun followListener(isAskingToFollow: Boolean): ActionListener {
        val captor = argumentCaptor<ActionListener>()
        verify(blogActions).followFeedById(
            eq(2L),
            eq(1L),
            eq(isAskingToFollow),
            captor.capture(),
            eq(ReaderTracker.SOURCE_SEARCH),
            eq(readerTracker)
        )
        return captor.lastValue
    }

    private fun siteSearches(): List<ReaderSearchSitesPayload> {
        val captor = argumentCaptor<Action<*>>()
        verify(dispatcher, atLeastOnce()).dispatch(captor.capture())
        return captor.allValues.mapNotNull { it.payload as? ReaderSearchSitesPayload }
    }

    private fun lastSiteSearch() = siteSearches().last()

    private fun site(feedId: Long, isFollowing: Boolean = false) = ReaderSiteModel().apply {
        this.feedId = feedId
        siteId = feedId + 1
        title = "Site $feedId"
        url = "https://site$feedId.example.com"
        this.isFollowing = isFollowing
    }
}
