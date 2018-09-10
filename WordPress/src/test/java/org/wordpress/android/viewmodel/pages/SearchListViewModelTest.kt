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
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus.DRAFT
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.DraftPage
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.test
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class SearchListViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var pageStore: PageStore
    @Mock lateinit var site: SiteModel
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var actionPerformer: ActionPerformer
    private lateinit var viewModel: SearchListViewModel
    private lateinit var pagesViewModel: PagesViewModel

    @Before
    fun setUp() {
        viewModel = SearchListViewModel(resourceProvider)
        pagesViewModel = PagesViewModel(pageStore, dispatcher, actionPerformer, Unconfined)
    }

    @Test
    fun onSearchReturnsResultsFromStore() = runBlocking<Unit> {
        initSearch()
        whenever(resourceProvider.getString(string.pages_drafts)).thenReturn("Drafts")
        val query = "query"
        val title = "title"
        val drafts = listOf( PageModel(site, 1, "title", DRAFT, Date(), false, 1, null))
        val expectedResult = listOf(Divider("Drafts"), DraftPage(1, title))
        whenever(pageStore.groupedSearch(site, query)).thenReturn(sortedMapOf(DRAFT to drafts))

        val observer = viewModel.searchResult.test()

        pagesViewModel.onSearch(query)

        val result = observer.await()

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun onEmptySearchResultEmitsEmptyItem() = runBlocking<Unit> {
        initSearch()
        val query = "query"
        val pageItems = listOf(Empty(string.pages_empty_search_result, true))
        whenever(pageStore.groupedSearch(site, query)).thenReturn(sortedMapOf())

        val observer = viewModel.searchResult.test()

        pagesViewModel.onSearch(query)

        val result = observer.await()

        assertThat(result).isEqualTo(pageItems)
    }

    @Test
    fun onEmptyQueryClearsSearch() = runBlocking<Unit> {
        initSearch()
        val query = ""
        val pageItems = listOf(Empty(string.pages_search_suggestion, true))

        val observer = viewModel.searchResult.test()

        pagesViewModel.onSearch(query)

        val result = observer.await()

        assertThat(result).isEqualTo(pageItems)
    }

    private suspend fun initSearch() {
        whenever(pageStore.getPagesFromDb(site)).thenReturn(listOf())
        whenever(pageStore.requestPagesFromServer(any())).thenReturn(OnPostChanged(0, false))
        pagesViewModel.start(site)
        viewModel.start(pagesViewModel)
    }
}
