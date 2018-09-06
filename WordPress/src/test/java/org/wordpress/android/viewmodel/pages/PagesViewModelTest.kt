package org.wordpress.android.viewmodel.pages

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import junit.framework.Assert.assertEquals
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
import org.wordpress.android.networking.PageUploadUtil
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.DraftPage
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.DONE
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.FETCHING
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.REFRESHING
import org.wordpress.android.viewmodel.test
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class PagesViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var pageStore: PageStore
    @Mock lateinit var site: SiteModel
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var uploadUtil: PageUploadUtil
    @Mock lateinit var dispatcher: Dispatcher
    private lateinit var viewModel: PagesViewModel

    @Before
    fun setUp() {
        viewModel = PagesViewModel(pageStore, dispatcher, resourceProvider, uploadUtil, Unconfined)
    }

    @Test
    fun clearsResultAndLoadsDataOnStart() = runBlocking<Unit> {
        whenever(pageStore.getPagesFromDb(site)).thenReturn(listOf(
                PageModel(site, 1, "title", DRAFT, Date(), false, 1, null))
        )
        whenever(pageStore.requestPagesFromServer(any())).thenReturn(OnPostChanged(1, false))
        val listStateObserver = viewModel.listState.test()
        val refreshPagesObserver = viewModel.refreshPages.test()
        val searchResultObserver = viewModel.searchResult.test()

        viewModel.start(site)

        assertEquals(searchResultObserver.await(), listOf(Empty(string.pages_search_suggestion, true)))

        val listStates = listStateObserver.awaitValues(2)

        assertThat(listStates).containsExactly(REFRESHING, DONE)
        refreshPagesObserver.awaitNullableValues(2)
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

        viewModel.onSearch(query)

        val result = observer.await()

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun onEmptySearchResultEmitsEmptyItem() = runBlocking<Unit> {
        initSearch()
        val query = "query"
        val pageItems = listOf(Empty(string.pages_empty_search_result, true))
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
        val pageItems = listOf(Empty(string.pages_search_suggestion, true))

        val data = viewModel.searchResult.test()

        viewModel.onSearch(query)

        val result = data.await()

        assertThat(result).isEqualTo(pageItems)
    }

    @Test
    fun onSiteWithoutPages() = runBlocking<Unit> {
        whenever(pageStore.getPagesFromDb(site)).thenReturn(emptyList())
        whenever(pageStore.requestPagesFromServer(any())).thenReturn(OnPostChanged(0, false))
        val listStateObserver = viewModel.listState.test()
        val refreshPagesObserver = viewModel.refreshPages.test()

        viewModel.start(site)

        val listStates = listStateObserver.awaitValues(2)

        assertThat(listStates).containsExactly(FETCHING, DONE)
        refreshPagesObserver.awaitNullableValues(2)
    }

    private suspend fun initSearch() {
        whenever(pageStore.getPagesFromDb(site)).thenReturn(listOf())
        whenever(pageStore.requestPagesFromServer(any())).thenReturn(OnPostChanged(0, false))
        viewModel.start(site)
    }
}
