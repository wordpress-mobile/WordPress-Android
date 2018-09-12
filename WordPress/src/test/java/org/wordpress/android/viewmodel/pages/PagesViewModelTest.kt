package org.wordpress.android.viewmodel.pages

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus
import org.wordpress.android.fluxc.model.page.PageStatus.DRAFT
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.DONE
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.FETCHING
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.REFRESHING
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
    private lateinit var viewModel: PagesViewModel
    private lateinit var listStates: MutableList<PageListState>
    private lateinit var pages: MutableList<List<PageModel>>
    private lateinit var searchPages: MutableList<SortedMap<PageStatus, List<PageModel>>>

    @Before
    fun setUp() {
        viewModel = PagesViewModel(pageStore, dispatcher, actionPerformer, Unconfined, Unconfined)
        listStates = mutableListOf()
        pages = mutableListOf()
        searchPages = mutableListOf()
        viewModel.listState.observeForever { if (it != null) listStates.add(it) }
        viewModel.pages.observeForever { if (it != null) pages.add(it) }
        viewModel.searchPages.observeForever { if (it != null) searchPages.add(it) }
    }

    @Test
    fun clearsResultAndLoadsDataOnStart() = runBlocking<Unit> {
        val pageModel = initPageRepo()
        whenever(pageStore.requestPagesFromServer(any())).thenReturn(OnPostChanged(1, false))

        viewModel.start(site)

        assertThat(listStates).containsExactly(REFRESHING, DONE)
        assertThat(pages).hasSize(2)
        assertThat(pages.last()).containsOnly(pageModel)
    }

    private suspend fun initPageRepo(): PageModel {
        val pageModel = PageModel(site, 1, "title", DRAFT, Date(), false, 1, null)
        val expectedPages = listOf(
                pageModel
        )
        whenever(pageStore.getPagesFromDb(site)).thenReturn(
                expectedPages
        )
        return pageModel
    }

    @Test
    fun onSiteWithoutPages() = runBlocking<Unit> {
        whenever(pageStore.getPagesFromDb(site)).thenReturn(emptyList())
        whenever(pageStore.requestPagesFromServer(any())).thenReturn(OnPostChanged(0, false))

        viewModel.start(site)

        assertThat(listStates).containsExactly(FETCHING, DONE)
        assertThat(pages).hasSize(2)
    }

    @Test
    fun onPageEditFinishedReloadSite() = runBlocking<Unit> {
        whenever(pageStore.requestPagesFromServer(site)).thenReturn(OnPostChanged(0, false))
        whenever(pageStore.getPagesFromDb(site)).thenReturn(listOf())

        val pageModel = initPageRepo()
        viewModel.start(site)

        reset(pageStore)

        val postModel = PostModel()
        val postId: Long = 5
        postModel.remotePostId = postId
        val job = launch { viewModel.onPageEditFinished(postId) }

        delay(10)

        verifyZeroInteractions(pageStore)

        job.join()

        viewModel.onPostUploaded(OnPostUploaded(postModel))

        verify(pageStore).requestPagesFromServer(site)
        assertThat(pages.last()).containsOnly(pageModel)
    }

    @Test
    fun onSearchReturnsResultsFromStore() = runBlocking<Unit> {
        initSearch()
        val query = "query"
        val drafts = listOf(PageModel(site, 1, "title", DRAFT, Date(), false, 1, null))
        val expectedResult = sortedMapOf(DRAFT to drafts)
        whenever(pageStore.groupedSearch(site, query)).thenReturn(expectedResult)

        viewModel.onSearch(query, 0)

        val result = viewModel.searchPages.value

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun onEmptySearchResultEmitsEmptyItem() = runBlocking {
        initSearch()
        val query = "query"
        whenever(pageStore.groupedSearch(site, query)).thenReturn(sortedMapOf())

        viewModel.onSearch(query, 0)

        val result = viewModel.searchPages.value

        assertThat(result).isEmpty()
    }

    @Test
    fun onEmptyQueryClearsSearch() = runBlocking {
        initSearch()
        val query = ""

        viewModel.onSearch(query, 0)

        val result = viewModel.searchPages.value

        assertThat(result).isNull()
    }

    private suspend fun initSearch() {
        whenever(pageStore.getPagesFromDb(site)).thenReturn(listOf())
        whenever(pageStore.requestPagesFromServer(any())).thenReturn(OnPostChanged(0, false))
        viewModel.start(site)
    }
}
