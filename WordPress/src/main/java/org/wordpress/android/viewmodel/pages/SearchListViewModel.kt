package org.wordpress.android.viewmodel.pages

import android.arch.lifecycle.Observer
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.pages.PageListViewModel.ListType.SEARCH
import java.util.SortedMap
import javax.inject.Inject

class SearchListViewModel
@Inject constructor(private val resourceProvider: ResourceProvider) : PageListViewModel() {

    fun start(pagesViewModel: PagesViewModel) {
        this.listType = SEARCH
        this.pagesViewModel = pagesViewModel

        if (!isStarted) {
            isStarted = true

            pagesViewModel.searchPages.observeForever(searchObserver)
        }
    }

    override fun onCleared() {
        pagesViewModel.searchPages.removeObserver(searchObserver)
    }

    private val searchObserver = Observer<SortedMap<PageStatus, List<PageModel>>> { pages ->
        if (pages != null) {
            loadFoundPages(pages)

            pagesViewModel.checkIfNewPageButtonShouldBeVisible()
        } else {
            _pages.postValue(listOf(Empty(string.pages_search_suggestion, true)))
        }
    }

    private fun loadFoundPages(pages: SortedMap<PageStatus, List<PageModel>>) = launch {
        if (pages.isNotEmpty()) {
            val pageItems = pages
                    .map { (status, results) ->
                        listOf(Divider(resourceProvider.getString(ListType.fromStatus(status).titleResource))) +
                                results.map { it.toPageItem(pagesViewModel.arePageActionsEnabled) }
                    }
                    .fold(mutableListOf()) { acc: MutableList<PageItem>, list: List<PageItem> ->
                        acc.addAll(list)
                        return@fold acc
                    }
            _pages.postValue(pageItems)
        } else {
            _pages.postValue(listOf(Empty(string.pages_empty_search_result, true)))
        }
    }
}
