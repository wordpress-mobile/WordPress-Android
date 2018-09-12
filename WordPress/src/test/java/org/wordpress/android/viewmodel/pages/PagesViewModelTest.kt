package org.wordpress.android.viewmodel.pages

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus
import org.wordpress.android.fluxc.model.page.PageStatus.DRAFT
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
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
    private lateinit var refreshPages: MutableList<List<PageModel>>
    private lateinit var searchPages: MutableList<SortedMap<PageStatus, List<PageModel>>>

    @Before
    fun setUp() {
        viewModel = PagesViewModel(pageStore, dispatcher, actionPerformer, Unconfined, Unconfined)
        listStates = mutableListOf()
        refreshPages = mutableListOf()
        viewModel.listState.observeForever { if (it != null) listStates.add(it) }
        viewModel.pages.observeForever { if (it != null) refreshPages.add(it) }
        viewModel.searchPages.observeForever { if (it != null) searchPages.add(it) }
    }

    @Test
    fun clearsResultAndLoadsDataOnStart() = runBlocking<Unit> {
        whenever(pageStore.getPagesFromDb(site)).thenReturn(
                listOf(
                        PageModel(site, 1, "title", DRAFT, Date(), false, 1, null)
                )
        )
        whenever(pageStore.requestPagesFromServer(any())).thenReturn(OnPostChanged(1, false))

        viewModel.start(site)

        assertThat(listStates).containsExactly(REFRESHING, DONE)
        assertThat(refreshPages).hasSize(3)
    }

    @Test
    fun onSiteWithoutPages() = runBlocking<Unit> {
        whenever(pageStore.getPagesFromDb(site)).thenReturn(emptyList())
        whenever(pageStore.requestPagesFromServer(any())).thenReturn(OnPostChanged(0, false))

        viewModel.start(site)

        assertThat(listStates).containsExactly(FETCHING, DONE)
        assertThat(refreshPages).hasSize(3)
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
