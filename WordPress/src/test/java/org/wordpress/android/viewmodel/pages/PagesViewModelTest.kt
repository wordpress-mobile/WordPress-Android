package org.wordpress.android.viewmodel.pages

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.experimental.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus.DRAFT
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Divider
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
    fun clearsResultAndLoadsDataOnStart() = runBlocking<Unit> {
        whenever(pageStore.loadPagesFromDb(site)).thenReturn(listOf(PageModel(1, "title", DRAFT, -1)))
        whenever(pageStore.requestPagesFromServer(any())).thenReturn(OnPostChanged(1, false))
        val listStateObserver = viewModel.listState.test()
        val refreshPagesObserver = viewModel.refreshPages.test()
        val searchResultObserver = viewModel.searchResult.test()

        viewModel.start(site)

        assertEquals(searchResultObserver.await(), listOf(Empty(string.empty_list_default)))

        val listStates = listStateObserver.awaitValues(2)

        assertThat(listStates).containsExactly(FETCHING, DONE)
        refreshPagesObserver.awaitNullableValues(2)
    }

    @Test
    fun onSearchReturnsResultsFromStore() = runBlocking<Unit> {
        initSearch()
        val query = "query"
        val title = "title"
        val drafts = listOf(PageModel(1, title, DRAFT, -1))
        val expectedResult = listOf(Divider(string.pages_drafts), PageItem.Page(1, title, null))
        whenever(pageStore.groupedSearch(site, query)).thenReturn(sortedMapOf(DRAFT to drafts))

        val observer = viewModel.searchResult.test()

        viewModel.onSearch(query)

        val result = observer.await()

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun onEmptySearchResultEmitsEmptyItem() = runBlocking<Unit> {
        initSearch()
        val query = "query"
        val pageItems = listOf(Empty(string.pages_empty_search_result))
        whenever(pageStore.groupedSearch(site, query)).thenReturn(sortedMapOf())

        val data = viewModel.searchResult.test()

        viewModel.onSearch(query)

        val result = data.await()

        assertThat(result).isEqualTo(pageItems)
    }

    @Test
    fun onEmptyQueryClearsSearch() = runBlocking<Unit> {
        initSearch()
        val query = ""
        val pageItems = listOf(Empty(string.empty_list_default))

        val data = viewModel.searchResult.test()

        viewModel.onSearch(query)

        val result = data.await()

        assertThat(result).isEqualTo(pageItems)
    }

    private suspend fun initSearch() {
        whenever(pageStore.loadPagesFromDb(site)).thenReturn(listOf())
        whenever(pageStore.requestPagesFromServer(any())).thenReturn(OnPostChanged(0, false))
        viewModel.start(site)
    }
}
