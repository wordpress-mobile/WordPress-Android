package org.wordpress.android.ui.pagesrs

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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.postsrs.data.PostRsRestClient
import org.wordpress.android.ui.postsrs.data.WpServiceProvider
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
internal class PagesRsListViewModelTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var serviceProvider: WpServiceProvider
    @Mock lateinit var restClient: PostRsRestClient
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var fluxCBridge: PageRsFluxCBridge
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private lateinit var site: SiteModel
    private var activeViewModel: PagesRsListViewModel? = null

    @Before
    fun setUp() {
        site = SiteModel().apply {
            id = 1
            siteId = 123L
        }
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    @After
    fun tearDown() {
        activeViewModel?.viewModelScope?.cancel()
        activeViewModel = null
    }

    private fun createViewModel() = PagesRsListViewModel(
        selectedSiteRepository = selectedSiteRepository,
        serviceProvider = serviceProvider,
        restClient = restClient,
        resourceProvider = resourceProvider,
        fluxCBridge = fluxCBridge,
        networkUtilsWrapper = networkUtilsWrapper,
        accountStore = accountStore,
        appPrefsWrapper = appPrefsWrapper,
        analyticsTracker = analyticsTracker,
    ).also { activeViewModel = it }

    @Test
    fun `when no site selected, emits ShowToast and Finish`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        val viewModel = createViewModel()

        viewModel.events.test {
            val first = awaitItem()
            assertThat(first).isInstanceOf(PageRsListEvent.ShowToast::class.java)
            assertThat((first as PageRsListEvent.ShowToast).messageResId)
                .isEqualTo(R.string.blog_not_found)

            val second = awaitItem()
            assertThat(second).isEqualTo(PageRsListEvent.Finish)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tab states are empty on init`() {
        val viewModel = createViewModel()
        assertThat(viewModel.tabStates.value).isEmpty()
    }

    @Test
    fun `isOpeningPage is false on init`() {
        val viewModel = createViewModel()
        assertThat(viewModel.isOpeningPage.value).isFalse()
    }

    @Test
    fun `search is inactive on init`() {
        val viewModel = createViewModel()
        assertThat(viewModel.isSearchActive.value).isFalse()
    }

    @Test
    fun `search query is empty on init`() {
        val viewModel = createViewModel()
        assertThat(viewModel.searchQuery.value).isEmpty()
    }

    @Test
    fun `onSearchOpen sets isSearchActive to true`() {
        val viewModel = createViewModel()

        viewModel.onSearchOpen()

        assertThat(viewModel.isSearchActive.value).isTrue()
    }

    @Test
    fun `onSearchOpen tracks PAGES_LIST_SEARCH_ACCESSED`() {
        val viewModel = createViewModel()

        viewModel.onSearchOpen()

        verify(analyticsTracker).track(
            eq(Stat.PAGES_LIST_SEARCH_ACCESSED),
            eq(site),
            anyOrNull<Map<String, *>>()
        )
    }

    @Test
    fun `onSearchClose sets isSearchActive to false`() {
        val viewModel = createViewModel()
        viewModel.onSearchOpen()

        viewModel.onSearchClose(PageRsListTab.PUBLISHED)

        assertThat(viewModel.isSearchActive.value).isFalse()
    }

    @Test
    fun `onSearchClose clears search query`() {
        val viewModel = createViewModel()
        viewModel.onSearchOpen()
        viewModel.onSearchQueryChanged("hello", PageRsListTab.PUBLISHED)

        viewModel.onSearchClose(PageRsListTab.PUBLISHED)

        assertThat(viewModel.searchQuery.value).isEmpty()
    }

    @Test
    fun `onSearchQueryChanged updates search query`() {
        val viewModel = createViewModel()

        viewModel.onSearchQueryChanged("hello", PageRsListTab.PUBLISHED)

        assertThat(viewModel.searchQuery.value).isEqualTo("hello")
    }

    @Test
    fun `onSearchQueryChanged with blank clears tab states`() {
        val viewModel = createViewModel()

        viewModel.onSearchQueryChanged("", PageRsListTab.PUBLISHED)

        assertThat(viewModel.tabStates.value).isEmpty()
    }

    @Test
    fun `onAuthorFilterChanged updates author filter`() {
        val viewModel = createViewModel()

        viewModel.onAuthorFilterChanged(AuthorFilterSelection.ME, PageRsListTab.PUBLISHED)

        assertThat(viewModel.authorFilter.value).isEqualTo(AuthorFilterSelection.ME)
    }

    @Test
    fun `onAuthorFilterChanged persists preference`() {
        val viewModel = createViewModel()

        viewModel.onAuthorFilterChanged(AuthorFilterSelection.ME, PageRsListTab.PUBLISHED)

        verify(appPrefsWrapper).pagesListAuthorSelection = AuthorFilterSelection.ME
    }

    @Test
    fun `onAuthorFilterChanged tracks PAGES_LIST_AUTHOR_FILTER_CHANGED`() {
        val viewModel = createViewModel()

        viewModel.onAuthorFilterChanged(AuthorFilterSelection.ME, PageRsListTab.PUBLISHED)

        verify(analyticsTracker).track(
            eq(Stat.PAGES_LIST_AUTHOR_FILTER_CHANGED),
            eq(site),
            any<Map<String, *>>()
        )
    }

    @Test
    fun `onAuthorFilterChanged ignores same selection`() {
        val viewModel = createViewModel()

        viewModel.onAuthorFilterChanged(AuthorFilterSelection.EVERYONE, PageRsListTab.PUBLISHED)

        verify(appPrefsWrapper, never()).pagesListAuthorSelection = any()
        verify(analyticsTracker, never()).track(
            eq(Stat.PAGES_LIST_AUTHOR_FILTER_CHANGED),
            any<SiteModel>(),
            any<Map<String, *>>()
        )
    }

    @Test
    fun `onAddNewPage emits CreateNewPage`() = test {
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onAddNewPage()

            assertThat(awaitItem()).isEqualTo(PageRsListEvent.CreateNewPage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onAddNewPage tracks PAGES_ADD_PAGE`() {
        val viewModel = createViewModel()

        viewModel.onAddNewPage()

        verify(analyticsTracker).track(
            eq(Stat.PAGES_ADD_PAGE),
            eq(site),
            anyOrNull<Map<String, *>>()
        )
    }

    @Test
    fun `openPage on non-trashed tab tracks PAGES_LIST_ITEM_SELECTED`() {
        val viewModel = createViewModel()

        viewModel.openPage(42L, PageRsListTab.PUBLISHED)

        verify(analyticsTracker).track(
            eq(Stat.PAGES_LIST_ITEM_SELECTED),
            eq(site),
            any<Map<String, *>>()
        )
    }

    @Test
    fun `openPage on trashed tab emits trashed toast and does not track edit`() = test {
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.openPage(remotePageId = 42L, tab = PageRsListTab.TRASHED)

            assertThat(awaitItem()).isEqualTo(
                PageRsListEvent.ShowToast(R.string.pages_list_item_trashed)
            )
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(viewModel.isOpeningPage.value).isFalse()
        verify(analyticsTracker, never()).track(
            eq(Stat.PAGES_LIST_ITEM_SELECTED),
            any<SiteModel>(),
            any<Map<String, *>>()
        )
    }

    @Test
    fun `loadMorePages no-ops when collection not initialized`() {
        val viewModel = createViewModel()

        viewModel.loadMorePages(PageRsListTab.PUBLISHED)

        assertThat(viewModel.tabStates.value).isEmpty()
    }

    @Test
    fun `refreshTab triggers init when collection not initialized`() {
        val viewModel = createViewModel()

        viewModel.refreshTab(PageRsListTab.PUBLISHED, isUserRefresh = true)

        // No collection exists, so refreshTab falls back to initTab. The synchronous portion
        // leaves the tab in an effective loading state (no error has been recorded yet).
        val state = viewModel.tabStates.value[PageRsListTab.PUBLISHED]
        assertThat(state?.error).isNull()
        assertThat(state?.isRefreshing ?: false).isFalse
    }
}
