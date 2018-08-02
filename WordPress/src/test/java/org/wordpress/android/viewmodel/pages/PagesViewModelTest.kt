package org.wordpress.android.viewmodel.pages

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.models.pages.PageModel
import org.wordpress.android.models.pages.PageStatus.DRAFT
import org.wordpress.android.networking.PageStore
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.DONE
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.FETCHING
import org.wordpress.android.viewmodel.test

@RunWith(MockitoJUnitRunner::class)
class PagesViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var pageStore: PageStore
    @Mock lateinit var site: SiteModel
    private lateinit var viewModel: PagesViewModel
    @Before
    fun setUp() {
        viewModel = PagesViewModel(pageStore)
    }

    @Test
    fun loadsDataOnStart() = runBlocking<Unit> {
        whenever(pageStore.loadPagesFromDb(site)).thenReturn(listOf(PageModel(1, "title", DRAFT, -1)))
        whenever(pageStore.requestPagesFromServer(any(), any())).thenReturn(OnPostChanged(1, false))
        val listStateObserver = viewModel.listState.test()
        val refreshPagesObserver = viewModel.refreshPages.test()

        viewModel.start(site)

        val listStates = listStateObserver.awaitValues(2)

        assertEquals(FETCHING, listStates[0])
        assertEquals(DONE, listStates[1])
        refreshPagesObserver.awaitNullableValues(2)
    }

    @Test
    fun onSearchReturnsResultsFromStore() = runBlocking {
        initSearch()
        val query = "query"
        val expectedResult = listOf(PageModel(1, "title", DRAFT, -1))
        val pageItems = expectedResult.map { PageItem.Page(it.pageId.toLong(),  it.title, null) }
        whenever(pageStore.search(site, query)).thenReturn(expectedResult)

        val observer = viewModel.searchResult.test()

        viewModel.onSearch(query)

        val searchResult = observer.await()

        assertEquals(pageItems, searchResult)
    }

    @Test
    fun onEmptySearchResultEmitsEmptyItem() = runBlocking {
        initSearch()
        val query = "query"
        val pageItems = listOf(Empty(string.pages_empty_search_result))
        whenever(pageStore.search(site, query)).thenReturn(listOf())

        val data = viewModel.searchResult.test()

        viewModel.onSearch(query)

        val result = data.await()

        assertEquals(pageItems, result)
    }

    @Test
    fun onEmptyQueryClearsSearch() = runBlocking {
        initSearch()
        val query = ""
        val pageItems = listOf(Empty(string.empty_list_default))

        val data = viewModel.searchResult.test()

        viewModel.onSearch(query)

        val result = data.await()

        assertEquals(pageItems, result)
    }

    private suspend fun initSearch() {
        whenever(pageStore.loadPagesFromDb(site)).thenReturn(listOf())
        whenever(pageStore.requestPagesFromServer(any(), any())).thenReturn(OnPostChanged(0, false))
        viewModel.start(site)
    }
}
