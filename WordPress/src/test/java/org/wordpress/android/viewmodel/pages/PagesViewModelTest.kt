package org.wordpress.android.viewmodel.pages

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
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
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus.DRAFT
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.test
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
    private lateinit var pageModel: PageModel

    @Before
    fun setUp() {
        viewModel = PagesViewModel(
                pageStore,
                dispatcher,
                actionPerformer,
                networkUtils,
                Dispatchers.Unconfined,
                Dispatchers.Unconfined
        )
        listStates = mutableListOf()
        pages = mutableListOf()
        searchPages = mutableListOf()
        viewModel.listState.observeForever { if (it != null) listStates.add(it) }
        viewModel.pages.observeForever { if (it != null) pages.add(it) }
        viewModel.searchPages.observeForever { if (it != null) searchPages.add(it) }
        pageModel = PageModel(site, 1, "title", DRAFT, Date(), false, 1, null, 0)
        whenever(networkUtils.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun clearsResultAndLoadsDataOnStart() = test {
        val pageModel = initPageRepo()
        whenever(pageStore.requestPagesFromServer(any())).thenReturn(
                OnPostChanged(CauseOfOnPostChanged.FetchPages, 1, false)
        )

        viewModel.start(site)

        assertThat(listStates).containsExactly(REFRESHING, DONE)
        assertThat(pages).hasSize(2)
        assertThat(pages.last()).containsOnly(pageModel)
    }

    private suspend fun initPageRepo(): PageModel {
        val expectedPages = listOf(
                pageModel
        )
        whenever(pageStore.getPagesFromDb(site)).thenReturn(
                expectedPages
        )
        return pageModel
    }

    @Test
    fun onSiteWithoutPages() = test {
        whenever(pageStore.getPagesFromDb(site)).thenReturn(emptyList())
        whenever(pageStore.requestPagesFromServer(any())).thenReturn(
                OnPostChanged(CauseOfOnPostChanged.FetchPages, 0, false)
        )

        viewModel.start(site)

        assertThat(listStates).containsExactly(FETCHING, DONE)
        assertThat(pages).hasSize(2)
    }

    @Test
    fun onSearchReturnsResultsFromStore() = test {
        initSearch()
        val query = "query"
        val drafts = listOf(PageModel(site, 1, "title", DRAFT, Date(), false, 1, null, 0))
        val expectedResult = sortedMapOf(DRAFTS to drafts)
        whenever(pageStore.search(site, query)).thenReturn(drafts)

        viewModel.onSearch(query, 0)

        val result = viewModel.searchPages.value

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun onEmptySearchResultEmitsEmptyItem() = runBlocking {
        initSearch()
        val query = "query"
        whenever(pageStore.search(site, query)).thenReturn(listOf())

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
        whenever(pageStore.requestPagesFromServer(any())).thenReturn(
                OnPostChanged(CauseOfOnPostChanged.FetchPages, 0, false)
        )
        viewModel.start(site)
    }
}
