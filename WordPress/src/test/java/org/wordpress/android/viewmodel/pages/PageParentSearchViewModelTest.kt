package org.wordpress.android.viewmodel.pages

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R.string
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Empty

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PageParentSearchViewModelTest : BaseUnitTest() {
    @Mock lateinit var pageParentViewModel: PageParentViewModel

    private lateinit var searchPages: MutableLiveData<List<PageItem>>
    private lateinit var viewModel: PageParentSearchViewModel

    @Before
    fun setUp() {
        viewModel = PageParentSearchViewModel(
                testDispatcher()
        )
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
