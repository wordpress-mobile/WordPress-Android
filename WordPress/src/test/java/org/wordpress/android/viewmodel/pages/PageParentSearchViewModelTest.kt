package org.wordpress.android.viewmodel.pages

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R.string
import org.wordpress.android.TEST_SCOPE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus.PUBLISHED
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class PageParentSearchViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var site: SiteModel
    @Mock lateinit var pageParentViewModel: PageParentViewModel

    private lateinit var searchPages: MutableLiveData<List<PageItem>>
    private lateinit var viewModel: PageParentSearchViewModel

    private lateinit var page: PageModel

    @Before
    fun setUp() {
        page = PageModel(site, 1, "title", PUBLISHED, Date(), false, 11L, null, 0)
        viewModel = PageParentSearchViewModel(TEST_SCOPE)
        searchPages = MutableLiveData()
        whenever(pageParentViewModel.searchPages).thenReturn(searchPages)
        viewModel.start(pageParentViewModel)
    }

    @Test
    fun `show empty item on start`() {
        searchPages.value = null

        assertThat(viewModel.searchResult.value).containsOnly(Empty(string.pages_search_suggestion, true))
    }

    @Test
    fun `show no matches on empty search results`() {
        searchPages.value = mutableListOf(Empty(string.pages_empty_search_result, false))

        assertThat(viewModel.searchResult.value).containsOnly(Empty(string.pages_empty_search_result, false))
    }

    @Test
    fun `passes page to page view model on parent radio button selected`() {
        val clickedPage = PageItem.ParentPage(id = 1, title = "title", isSelected = true, type = PageItem.Type.PARENT)

        viewModel.onParentSelected(clickedPage)

        verify(pageParentViewModel).onParentSelected(clickedPage)
    }
}
