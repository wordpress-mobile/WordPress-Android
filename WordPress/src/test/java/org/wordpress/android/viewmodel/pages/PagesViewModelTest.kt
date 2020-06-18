package org.wordpress.android.viewmodel.pages

import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront.PAGE
import org.wordpress.android.fluxc.model.SiteHomepageSettings.StaticPage
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus.DRAFT
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.SiteOptionsStore
import org.wordpress.android.fluxc.store.SiteOptionsStore.HomepageUpdatedPayload
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsError
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsErrorType.INVALID_PARAMETERS
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.test
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Action.PUBLISH_NOW
import org.wordpress.android.ui.pages.PageItem.Action.SET_AS_HOMEPAGE
import org.wordpress.android.ui.pages.PageItem.Action.SET_AS_POSTS_PAGE
import org.wordpress.android.ui.pages.PageItem.PublishedPage
import org.wordpress.android.ui.pages.PagesAuthorFilterUIState
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.posts.AuthorFilterSelection.EVERYONE
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.uploads.UploadStarter
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.DONE
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.FETCHING
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.REFRESHING
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.DRAFTS
import org.wordpress.android.viewmodel.uistate.ProgressBarUiState
import java.util.Date
import java.util.SortedMap

@RunWith(MockitoJUnitRunner::class)
class PagesViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var pageStore: PageStore
    @Mock lateinit var site: SiteModel
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var actionPerformer: ActionPerformer
    @Mock lateinit var networkUtils: NetworkUtilsWrapper
    @Mock lateinit var uploadStarter: UploadStarter
    @Mock lateinit var siteOptionsStore: SiteOptionsStore
    @Mock lateinit var appLogWrapper: AppLogWrapper
    @Mock lateinit var siteStore: SiteStore
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper
    private lateinit var viewModel: PagesViewModel
    private lateinit var listStates: MutableList<PageListState>
    private lateinit var pages: MutableList<List<PageModel>>
    private lateinit var searchPages: MutableList<SortedMap<PageListType, List<PageModel>>>
    private lateinit var authorSelectionUpdated: MutableLiveData<AuthorFilterSelection>
    private lateinit var authorUIState: MutableLiveData<PagesAuthorFilterUIState>

    @Before
    fun setUp() {
        viewModel = PagesViewModel(
                pageStore = pageStore,
                postStore = mock(),
                dispatcher = dispatcher,
                actionPerfomer = actionPerformer,
                networkUtils = networkUtils,
                previewStateHelper = mock(),
                analyticsTracker = mock(),
                uploadStatusTracker = mock(),
                autoSaveConflictResolver = mock(),
                uiDispatcher = Dispatchers.Unconfined,
                defaultDispatcher = Dispatchers.Unconfined,
                eventBusWrapper = mock(),
                uploadStarter = uploadStarter,
                pageListEventListenerFactory = mock(),
                siteOptionsStore = siteOptionsStore,
                appLogWrapper = appLogWrapper,
                siteStore = siteStore,
                accountStore = accountStore,
                prefs = appPrefsWrapper
        )
        listStates = mutableListOf()
        pages = mutableListOf()
        searchPages = mutableListOf()
        authorSelectionUpdated = MutableLiveData<AuthorFilterSelection>()
        authorUIState = MutableLiveData<PagesAuthorFilterUIState>()
        viewModel.listState.observeForever { if (it != null) listStates.add(it) }
        viewModel.pages.observeForever { if (it != null) pages.add(it) }
        viewModel.searchPages.observeForever { if (it != null) searchPages.add(it) }
        viewModel.authorSelectionUpdated.observeForever { if (it != null) authorSelectionUpdated.value = it }
        viewModel.authorUIState.observeForever { if (it != null) authorUIState.value = it }
        whenever(networkUtils.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `when started with a non-empty PageStore, it clears the results and loads the data`() = test {
        // Arrange
        val pageModel = setUpPageStoreWithASinglePage(site)

        // Act
        viewModel.start(site)

        // Assert
        assertThat(listStates).containsExactly(REFRESHING, DONE)
        assertThat(pages).hasSize(2)
        assertThat(pages.last()).containsOnly(pageModel)
    }

    @Test
    fun `when started with an empty PageStore, it shows an initial fetch UI`() = test {
        // Arrange
        setUpPageStoreWithEmptyPages()

        // Act
        viewModel.start(site)

        // Assert
        assertThat(listStates).containsExactly(FETCHING, DONE)
        assertThat(pages).hasSize(2)
    }

    @Test
    fun `when searching, it returns the results from the Store`() = test {
        // Arrange
        setUpPageStoreWithEmptyPages()
        viewModel.start(site)

        val query = "query"
        val drafts = listOf(PageModel(PostModel(), site, 1, "title", DRAFT, Date(), false, 1, null, 0))
        val expectedResult = sortedMapOf(DRAFTS to drafts)
        whenever(pageStore.search(site, query)).thenReturn(drafts)

        // Act
        viewModel.onSearch(query, 0)

        // Assert
        val result = viewModel.searchPages.value
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `when searching and the Store is empty, it returns an empty list`() = runBlocking {
        // Arrange
        setUpPageStoreWithEmptyPages()
        viewModel.start(site)
        val query = "query"
        whenever(pageStore.search(site, query)).thenReturn(listOf())

        // Act
        viewModel.onSearch(query, 0)

        // Assert
        val result = viewModel.searchPages.value
        assertThat(result).isEmpty()
    }

    @Test
    fun `when searching with an empty query, it clears the search results`() = runBlocking {
        // Arrange
        setUpPageStoreWithEmptyPages()
        viewModel.start(site)
        val query = ""

        // Act
        viewModel.onSearch(query, 0)

        // Assert
        val result = viewModel.searchPages.value
        assertThat(result).isNull()
    }

    @Test
    fun `Auto-upload is initiated when the user enters the screen`() = test {
        // Given
        setUpPageStoreWithEmptyPages()
        // When
        viewModel.start(site)
        // Then
        verify(uploadStarter).queueUploadFromSite(site)
    }

    @Test
    fun `Auto-upload is initiated when the user pulls to refresh`() = test {
        // Given
        setUpPageStoreWithEmptyPages()
        viewModel.start(site)
        // When
        viewModel.onPullToRefresh()
        // Then
        // invoked twice - once in start() and once in onPullToRefresh()
        verify(uploadStarter, times(2)).queueUploadFromSite(site)
    }

    @Test
    fun `postUploadAction invoked on edit post activity result`() = test {
        // Given
        val intent = mock<Intent>()
        val pageModel = setUpPageStoreWithASinglePage(site)
        whenever(pageStore.getPageByLocalId(eq(pageModel.pageId), anyOrNull()))
                .thenReturn(pageModel)

        viewModel.start(site)
        // When
        viewModel.onPageEditFinished(pageModel.pageId, intent)
        // Then
        assertThat(viewModel.postUploadAction.value).isEqualTo(Triple(pageModel.post, pageModel.site, intent))
    }

    @Test
    fun `publish now menu action updates publishAction live data`() = test {
        // Given
        val pageModel = setUpPageStoreWithASinglePage(site)
        val page: PageItem.Page = mock()
        whenever(page.remoteId).thenReturn(pageModel.remoteId)
        viewModel.start(site)
        // When
        viewModel.onMenuAction(PUBLISH_NOW, page)

        // Then
        assertThat(viewModel.publishAction.value).isEqualTo(pageModel)
    }

    @Test
    fun `scrollToPage is invoked on edit post activity result`() = test {
        // Given
        val intent = mock<Intent>()
        val pageModel = setUpPageStoreWithASinglePage(site)
        whenever(pageStore.getPageByLocalId(eq(pageModel.pageId), anyOrNull()))
                .thenReturn(pageModel)

        viewModel.start(site)
        // When
        viewModel.onPageEditFinished(pageModel.pageId, intent)
        // Then
        assertThat(viewModel.scrollToPage.value).isEqualTo(pageModel)
    }

    @Test
    fun `SET_AS_HOMEPAGE sets page as Homepage`() = test {
        // Arrange
        val homepageId = 1L
        val snackbarMessages = mutableListOf<SnackbarMessageHolder>()
        setupPageOnFrontUpdate(
                snackbarMessages = snackbarMessages,
                showOnFront = PAGE,
                updatedPageOnFrontId = homepageId
        )

        // Act
        viewModel.onMenuAction(
                SET_AS_HOMEPAGE,
                getPublishedPage(homepageId)
        )

        // Assert
        assertThat(snackbarMessages[0].messageRes).isEqualTo(R.string.page_homepage_successfully_updated)
    }

    @Test
    fun `SET_AS_HOMEPAGE shows error store returns an error`() = test {
        // Arrange
        val homepageId = 1L
        val snackbarMessages = mutableListOf<SnackbarMessageHolder>()
        setupPageOnFrontUpdate(
                snackbarMessages = snackbarMessages,
                showOnFront = PAGE,
                updatedPageOnFrontId = homepageId,
                isError = true
        )

        // Act
        viewModel.onMenuAction(
                SET_AS_HOMEPAGE,
                getPublishedPage(homepageId)
        )

        // Assert
        assertThat(snackbarMessages[0].messageRes).isEqualTo(R.string.page_homepage_update_failed)
    }

    @Test
    fun `SET_AS_POSTS_PAGE sets page as Posts page`() = test {
        // Arrange
        val pageForPostsId = 1L
        val snackbarMessages = mutableListOf<SnackbarMessageHolder>()
        setupPageForPostsUpdate(
                snackbarMessages = snackbarMessages,
                showOnFront = PAGE,
                updatedPageForPostsId = pageForPostsId
        )

        // Act
        viewModel.onMenuAction(
                SET_AS_POSTS_PAGE,
                getPublishedPage(pageForPostsId)
        )

        // Assert
        assertThat(snackbarMessages[0].messageRes).isEqualTo(R.string.page_posts_page_successfully_updated)
    }

    @Test
    fun `SET_AS_POSTS_PAGE shows error store returns an error`() = test {
        // Arrange
        val pageForPostsId = 1L
        val snackbarMessages = mutableListOf<SnackbarMessageHolder>()
        setupPageForPostsUpdate(
                snackbarMessages = snackbarMessages,
                showOnFront = PAGE,
                updatedPageForPostsId = pageForPostsId,
                isError = true
        )

        // Act
        viewModel.onMenuAction(
                SET_AS_POSTS_PAGE,
                getPublishedPage(pageForPostsId)
        )

        // Assert
        assertThat(snackbarMessages[0].messageRes).isEqualTo(R.string.page_posts_page_update_failed)
    }

    private fun getPublishedPage(remoteId: Long): PublishedPage = PublishedPage(
            remoteId = remoteId,
            localId = 2,
            title = "Published page",
            date = Date(),
            actions = emptySet(),
            progressBarUiState = ProgressBarUiState.Hidden,
            showOverlay = false
    )

    private suspend fun setupPageForPostsUpdate(
        snackbarMessages: MutableList<SnackbarMessageHolder>,
        showOnFront: ShowOnFront = PAGE,
        updatedPageForPostsId: Long,
        isError: Boolean = false
    ) {
        val site = SiteModel()
        site.showOnFront = showOnFront.value
        setUpPageStoreWithASinglePage(site)
        viewModel.start(site)
        val settings = StaticPage(updatedPageForPostsId, -1)
        val homepageUpdatedPayload = if (!isError) HomepageUpdatedPayload(settings) else {
            HomepageUpdatedPayload(
                    SiteOptionsError(INVALID_PARAMETERS, "Message")
            )
        }
        whenever(siteOptionsStore.updatePageForPosts(eq(site), eq(updatedPageForPostsId))).thenReturn(
                homepageUpdatedPayload
        )
        viewModel.showSnackbarMessage.observeForever {
            snackbarMessages.add(it)
        }
    }

    private suspend fun setupPageOnFrontUpdate(
        snackbarMessages: MutableList<SnackbarMessageHolder>,
        showOnFront: ShowOnFront = PAGE,
        updatedPageOnFrontId: Long,
        isError: Boolean = false
    ) {
        val site = SiteModel()
        site.showOnFront = showOnFront.value
        setUpPageStoreWithASinglePage(site)
        viewModel.start(site)
        val settings = StaticPage(-1, updatedPageOnFrontId)
        val homepageUpdatedPayload = if (!isError) HomepageUpdatedPayload(settings) else {
            HomepageUpdatedPayload(
                    SiteOptionsError(INVALID_PARAMETERS, "Message")
            )
        }
        whenever(siteOptionsStore.updatePageOnFront(eq(site), eq(updatedPageOnFrontId))).thenReturn(
                homepageUpdatedPayload
        )
        viewModel.showSnackbarMessage.observeForever {
            snackbarMessages.add(it)
        }
    }

    private suspend fun setUpPageStoreWithEmptyPages() {
        whenever(pageStore.getPagesFromDb(site)).thenReturn(listOf())
        whenever(pageStore.requestPagesFromServer(any())).thenReturn(
                OnPostChanged(CauseOfOnPostChanged.FetchPages, 0, false)
        )
    }

    private suspend fun setUpPageStoreWithASinglePage(site: SiteModel): PageModel {
        val pageModel = PageModel(PostModel(), site, 1, "title", DRAFT, Date(), false, 1, null, 0)

        whenever(pageStore.getPagesFromDb(site)).thenReturn(listOf(pageModel))
        whenever(pageStore.requestPagesFromServer(any())).thenReturn(
                OnPostChanged(CauseOfOnPostChanged.FetchPages, 1, false)
        )

        return pageModel
    }

    @Test
    fun `when logged into a wpcom site with others edit, the author filter shows`() = test {
        // Arrange
        val wpcomSite = SiteModel()
        wpcomSite.setIsWPCom(true)
        wpcomSite.hasCapabilityEditOthersPages = true
        setUpPageStoreWithASinglePage(wpcomSite)

        whenever(appPrefsWrapper.postListAuthorSelection).thenReturn(EVERYONE)
        // Act
        viewModel.start(wpcomSite)

        // Assert
        assertThat(viewModel.authorUIState.value?.isAuthorFilterVisible).isEqualTo(true)
    }

    @Test
    fun `when logged into a non wpcom site, the author filter does not show`() = test {
        // Arrange
        val wpcomSite = SiteModel()
        wpcomSite.setIsWPCom(false)

        // Act
        viewModel.start(wpcomSite)

        // Assert
        assertThat(authorUIState.value?.isAuthorFilterVisible).isEqualTo(false)
        assertThat(viewModel.authorUIState.value?.isAuthorFilterVisible).isEqualTo(false)
    }

    @Test
    fun `when logged into a wpcom site with no others edit, the author filter does not show`() = test {
        // Arrange
        val wpcomSite = SiteModel()
        wpcomSite.setIsWPCom(true)
        wpcomSite.hasCapabilityEditOthersPages = false
        setUpPageStoreWithASinglePage(wpcomSite)

        // Act
        viewModel.start(wpcomSite)

        // Assert
        assertThat(viewModel.authorUIState.value?.isAuthorFilterVisible).isEqualTo(false)
    }

    @Test
    fun `when prefs are saved, they match state`() = test {
        // Arrange
        val wpcomSite = SiteModel()
        wpcomSite.setIsWPCom(true)
        wpcomSite.hasCapabilityEditOthersPages = true
        setUpPageStoreWithASinglePage(wpcomSite)

        whenever(appPrefsWrapper.postListAuthorSelection).thenReturn(EVERYONE)
        // Act
        viewModel.start(wpcomSite)

        // Assert
        assertThat(viewModel.authorUIState.value?.authorFilterSelection).isEqualTo(EVERYONE)
    }
}
