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
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.EditorThemeStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.postsrs.data.PostRsRestClient
import org.wordpress.android.ui.postsrs.data.WpServiceProvider
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.SiteEditorMVPFeatureConfig
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
internal class PagesRsListViewModelTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var serviceProvider: WpServiceProvider
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var restClient: PostRsRestClient
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var postStore: PostStore
    @Mock lateinit var homepageSettings: PageRsHomepageSettings
    @Mock lateinit var blazeFeatureUtils: BlazeFeatureUtils
    @Mock lateinit var fluxCBridge: PageRsFluxCBridge
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper
    @Mock lateinit var editorThemeStore: EditorThemeStore
    @Mock lateinit var siteEditorMVPFeatureConfig: SiteEditorMVPFeatureConfig

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
        dispatcher = dispatcher,
        restClient = restClient,
        resourceProvider = resourceProvider,
        postStore = postStore,
        homepageSettings = homepageSettings,
        blazeFeatureUtils = blazeFeatureUtils,
        fluxCBridge = fluxCBridge,
        networkUtilsWrapper = networkUtilsWrapper,
        accountStore = accountStore,
        appPrefsWrapper = appPrefsWrapper,
        analyticsTracker = analyticsTracker,
        editorThemeStore = editorThemeStore,
        siteEditorMVPFeatureConfig = siteEditorMVPFeatureConfig,
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
    fun `openPage on trashed tab asks to move the page to draft`() {
        val viewModel = createViewModel()

        viewModel.openPage(remotePageId = 42L, tab = PageRsListTab.TRASHED)

        assertThat(viewModel.pendingConfirmation.value)
            .isEqualTo(PageRsListConfirmation.MoveToDraft(42L))
        assertThat(viewModel.isOpeningPage.value).isFalse()
    }

    @Test
    fun `dismissing the move to draft confirmation does not track item selected`() {
        val viewModel = createViewModel()
        viewModel.openPage(remotePageId = 42L, tab = PageRsListTab.TRASHED)

        viewModel.onDismissPendingAction()

        verify(analyticsTracker, never()).track(
            eq(Stat.PAGES_LIST_ITEM_SELECTED),
            any<SiteModel>(),
            any<Map<String, *>>()
        )
    }

    @Test
    fun `openPage on the site editor row emits OpenSiteEditor`() = test {
        site.setIsWPCom(true)
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.openPage(SITE_EDITOR_PAGE_ID, PageRsListTab.PUBLISHED)

            val event = awaitItem()
            assertThat(event).isInstanceOf(PageRsListEvent.OpenSiteEditor::class.java)
            event as PageRsListEvent.OpenSiteEditor
            assertThat(event.url).endsWith("site-editor.php?canvas=edit")
            assertThat(event.useWpComCredentials).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `openPage on the site editor row tracks PAGES_EDIT_HOMEPAGE_ITEM_PRESSED`() {
        val viewModel = createViewModel()

        viewModel.openPage(SITE_EDITOR_PAGE_ID, PageRsListTab.PUBLISHED)

        verify(analyticsTracker).track(
            eq(Stat.PAGES_EDIT_HOMEPAGE_ITEM_PRESSED),
            eq(site),
            anyOrNull<Map<String, *>>()
        )
    }

    @Test
    fun `openPage on the site editor row does not track PAGES_LIST_ITEM_SELECTED`() {
        val viewModel = createViewModel()

        viewModel.openPage(SITE_EDITOR_PAGE_ID, PageRsListTab.PUBLISHED)

        verify(analyticsTracker, never()).track(
            eq(Stat.PAGES_LIST_ITEM_SELECTED),
            any<SiteModel>(),
            any<Map<String, *>>()
        )
    }

    @Test
    fun `registers with the dispatcher on init and unregisters on clear`() {
        val viewModel = createViewModel()
        verify(dispatcher).register(viewModel)

        viewModel.onCleared()

        verify(dispatcher).unregister(viewModel)
    }

    @Test
    fun `onPostUploaded for a page of the selected site refreshes the tabs`() {
        val viewModel = createViewModel()

        viewModel.onPostUploaded(OnPostUploaded(pageUpload(), false))

        verify(restClient).clearCaches()
    }

    @Test
    fun `onPostUploaded ignores posts`() {
        val viewModel = createViewModel()

        viewModel.onPostUploaded(OnPostUploaded(pageUpload().apply { setIsPage(false) }, false))

        verify(restClient, never()).clearCaches()
    }

    @Test
    fun `onPostUploaded ignores pages from other sites`() {
        val viewModel = createViewModel()

        viewModel.onPostUploaded(OnPostUploaded(pageUpload().apply { setLocalSiteId(99) }, false))

        verify(restClient, never()).clearCaches()
    }

    @Test
    fun `onPostUploaded ignores failed uploads`() {
        val viewModel = createViewModel()
        val event = OnPostUploaded(pageUpload(), false).apply {
            error = PostStore.PostError(PostStore.PostErrorType.GENERIC_ERROR)
        }

        viewModel.onPostUploaded(event)

        verify(restClient, never()).clearCaches()
    }

    private fun pageUpload() = PostModel().apply {
        setIsPage(true)
        setLocalSiteId(site.id)
    }

    @Test
    fun `onPageMenuAction TRASH sets Trash confirmation`() {
        val viewModel = createViewModel()

        viewModel.onPageMenuAction(42L, PageRsMenuAction.TRASH)

        assertThat(viewModel.pendingConfirmation.value)
            .isEqualTo(PageRsListConfirmation.Trash(42L))
    }

    @Test
    fun `onPageMenuAction DELETE_PERMANENTLY sets Delete confirmation`() {
        val viewModel = createViewModel()

        viewModel.onPageMenuAction(42L, PageRsMenuAction.DELETE_PERMANENTLY)

        assertThat(viewModel.pendingConfirmation.value)
            .isEqualTo(PageRsListConfirmation.Delete(42L, ""))
    }

    @Test
    fun `onPageMenuAction tracks PAGES_OPTIONS_PRESSED`() {
        val viewModel = createViewModel()

        viewModel.onPageMenuAction(42L, PageRsMenuAction.TRASH)

        verify(analyticsTracker).track(
            eq(Stat.PAGES_OPTIONS_PRESSED),
            eq(site),
            eq(mapOf("option_name" to "move_to_bin"))
        )
    }

    @Test
    fun `onDismissPendingAction clears pending confirmation`() {
        val viewModel = createViewModel()
        viewModel.onPageMenuAction(42L, PageRsMenuAction.TRASH)

        viewModel.onDismissPendingAction()

        assertThat(viewModel.pendingConfirmation.value).isNull()
    }

    @Test
    fun `onConfirmPendingAction clears pending confirmation`() {
        val viewModel = createViewModel()
        viewModel.onPageMenuAction(42L, PageRsMenuAction.TRASH)

        viewModel.onConfirmPendingAction()

        assertThat(viewModel.pendingConfirmation.value).isNull()
    }

    @Test
    fun `onConfirmPendingAction shows snackbar when offline`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        whenever(resourceProvider.getString(R.string.no_network_message))
            .thenReturn("No network")
        val viewModel = createViewModel()
        viewModel.onPageMenuAction(42L, PageRsMenuAction.TRASH)

        viewModel.snackbarMessages.test {
            viewModel.onConfirmPendingAction()

            assertThat(awaitItem().message).isEqualTo("No network")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPageMenuAction VIEW with no loaded page emits nothing`() = test {
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onPageMenuAction(42L, PageRsMenuAction.VIEW)

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPageMenuAction SET_AS_HOMEPAGE without static homepage shows snackbar`() = test {
        whenever(homepageSettings.setHomepage(site, 42L))
            .thenReturn(PageRsHomepageSettings.Result.StaticHomepageDisabled)
        whenever(resourceProvider.getString(R.string.page_cannot_set_homepage))
            .thenReturn("Cannot set homepage")
        val viewModel = createViewModel()

        viewModel.snackbarMessages.test {
            viewModel.onPageMenuAction(42L, PageRsMenuAction.SET_AS_HOMEPAGE)

            assertThat(awaitItem().message).isEqualTo("Cannot set homepage")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPageMenuAction SET_AS_POSTS_PAGE without static homepage shows snackbar`() = test {
        whenever(homepageSettings.setPostsPage(site, 42L))
            .thenReturn(PageRsHomepageSettings.Result.StaticHomepageDisabled)
        whenever(resourceProvider.getString(R.string.page_cannot_set_posts_page))
            .thenReturn("Cannot set posts page")
        val viewModel = createViewModel()

        viewModel.snackbarMessages.test {
            viewModel.onPageMenuAction(42L, PageRsMenuAction.SET_AS_POSTS_PAGE)

            assertThat(awaitItem().message).isEqualTo("Cannot set posts page")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPageMenuAction SET_AS_HOMEPAGE success shows confirmation snackbar`() = test {
        whenever(homepageSettings.setHomepage(site, 42L))
            .thenReturn(PageRsHomepageSettings.Result.Success)
        whenever(resourceProvider.getString(R.string.page_homepage_successfully_updated))
            .thenReturn("Homepage updated")
        val viewModel = createViewModel()

        viewModel.snackbarMessages.test {
            viewModel.onPageMenuAction(42L, PageRsMenuAction.SET_AS_HOMEPAGE)

            assertThat(awaitItem().message).isEqualTo("Homepage updated")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPageMenuAction SET_AS_HOMEPAGE failure shows error snackbar`() = test {
        whenever(homepageSettings.setHomepage(site, 42L))
            .thenReturn(PageRsHomepageSettings.Result.Error("403"))
        whenever(resourceProvider.getString(R.string.page_homepage_update_failed))
            .thenReturn("Homepage update failed")
        val viewModel = createViewModel()

        viewModel.snackbarMessages.test {
            viewModel.onPageMenuAction(42L, PageRsMenuAction.SET_AS_HOMEPAGE)

            assertThat(awaitItem().message).isEqualTo("Homepage update failed")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `openParentPicker without loaded page does nothing`() {
        val viewModel = createViewModel()

        viewModel.openParentPicker(42L)

        assertThat(viewModel.parentPicker.value).isNull()
    }

    @Test
    fun `onParentPickerDismissed clears picker state`() {
        val viewModel = createViewModel()

        viewModel.onParentPickerDismissed()

        assertThat(viewModel.parentPicker.value).isNull()
    }

    @Test
    fun `onParentSearchChanged no-ops when picker not open`() {
        val viewModel = createViewModel()

        viewModel.onParentSearchChanged("home")

        assertThat(viewModel.parentPicker.value).isNull()
    }

    @Test
    fun `onLoadMoreParents no-ops when picker not open`() {
        val viewModel = createViewModel()

        viewModel.onLoadMoreParents()

        assertThat(viewModel.parentPicker.value).isNull()
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
