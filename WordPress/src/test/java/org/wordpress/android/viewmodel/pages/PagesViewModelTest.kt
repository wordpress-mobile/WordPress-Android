package org.wordpress.android.viewmodel.pages

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus.DRAFT
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.test
import org.wordpress.android.ui.uploads.PostEvents
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.DONE
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.FETCHING
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.REFRESHING
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.DRAFTS
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
    private lateinit var viewModel: PagesViewModel
    private lateinit var listStates: MutableList<PageListState>
    private lateinit var pages: MutableList<List<PageModel>>
    private lateinit var searchPages: MutableList<SortedMap<PageListType, List<PageModel>>>

    @UseExperimental(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        viewModel = PagesViewModel(
                pageStore = pageStore,
                postStore = mock(),
                dispatcher = dispatcher,
                actionPerfomer = actionPerformer,
                networkUtils = networkUtils,
                previewStateHelper = mock(),
                uiDispatcher = Dispatchers.Unconfined,
                defaultDispatcher = Dispatchers.Unconfined,
                eventBusWrapper = mock()
        )
        listStates = mutableListOf()
        pages = mutableListOf()
        searchPages = mutableListOf()
        viewModel.listState.observeForever { if (it != null) listStates.add(it) }
        viewModel.pages.observeForever { if (it != null) pages.add(it) }
        viewModel.searchPages.observeForever { if (it != null) searchPages.add(it) }
        whenever(networkUtils.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `when started with a non-empty PageStore, it clears the results and loads the data`() = test {
        // Arrange
        val pageModel = setUpPageStoreWithASinglePage()

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
        val drafts = listOf(PageModel(site, 1, "title", DRAFT, Date(), false, 1, null, 0))
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
    fun `when a page is being uploaded, page actions are disabled`() = test {
        // Arrange
        setUpPageStoreWithEmptyPages()
        viewModel.start(site)

        assertThat(viewModel.arePageActionsEnabled).isTrue()

        // Act
        viewModel.onEventBackgroundThread(PostEvents.PostUploadStarted(createPostModel()))

        // Assert
        assertThat(viewModel.arePageActionsEnabled).isFalse()
    }

    @Test
    fun `when a page upload is completed, page actions are re-enabled`() = test {
        // Given
        val page = createPostModel()

        setUpPageStoreWithEmptyPages()
        viewModel.start(site)

        viewModel.onEventBackgroundThread(PostEvents.PostUploadStarted(page))
        assertThat(viewModel.arePageActionsEnabled).isFalse()

        // When
        viewModel.onPostUploaded(OnPostUploaded(page))

        // Then
        assertThat(viewModel.arePageActionsEnabled).isTrue()
    }

    private suspend fun setUpPageStoreWithEmptyPages() {
        whenever(pageStore.getPagesFromDb(site)).thenReturn(listOf())
        whenever(pageStore.requestPagesFromServer(any())).thenReturn(
                OnPostChanged(CauseOfOnPostChanged.FetchPages, 0, false)
        )
    }

    private suspend fun setUpPageStoreWithASinglePage(): PageModel {
        val pageModel = PageModel(site, 1, "title", DRAFT, Date(), false, 1, null, 0)

        whenever(pageStore.getPagesFromDb(site)).thenReturn(listOf(pageModel))
        whenever(pageStore.requestPagesFromServer(any())).thenReturn(
                OnPostChanged(CauseOfOnPostChanged.FetchPages, 1, false)
        )

        return pageModel
    }

    private companion object Fixtures {
        fun createPostModel() = PostModel().apply {
            setId(1_001)
            setRemotePostId(2_034)
            setIsPage(true)
        }
    }
}
