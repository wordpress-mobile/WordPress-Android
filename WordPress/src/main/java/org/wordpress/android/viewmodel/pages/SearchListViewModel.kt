package org.wordpress.android.viewmodel.pages

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus
import org.wordpress.android.modules.UI_SCOPE
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.DraftPage
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.PageItem.PublishedPage
import org.wordpress.android.ui.pages.PageItem.ScheduledPage
import org.wordpress.android.ui.pages.PageItem.TrashedPage
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType
import java.util.SortedMap
import javax.inject.Inject
import javax.inject.Named

class SearchListViewModel
@Inject constructor(
    private val resourceProvider: ResourceProvider,
    @Named(UI_SCOPE) private val uiScope: CoroutineScope
) : ViewModel() {
    private val _searchResult: MutableLiveData<List<PageItem>> = MutableLiveData()
    val searchResult: LiveData<List<PageItem>> = _searchResult

    private var isStarted: Boolean = false
    private lateinit var pagesViewModel: PagesViewModel

    fun start(pagesViewModel: PagesViewModel) {
        this.pagesViewModel = pagesViewModel

        if (!isStarted) {
            isStarted = true

            pagesViewModel.searchPages.observeForever(searchObserver)
        }
    }

    override fun onCleared() {
        pagesViewModel.searchPages.removeObserver(searchObserver)
    }

    private val searchObserver = Observer<SortedMap<PageListType, List<PageModel>>> { pages ->
        if (pages != null) {
            loadFoundPages(pages)

            pagesViewModel.checkIfNewPageButtonShouldBeVisible()
        } else {
            _searchResult.value = listOf(Empty(R.string.pages_search_suggestion, true))
        }
    }

    fun onMenuAction(action: Action, pageItem: Page): Boolean {
        return pagesViewModel.onMenuAction(action, pageItem)
    }

    fun onItemTapped(pageItem: Page) {
        pagesViewModel.onItemTapped(pageItem)
    }

    private fun loadFoundPages(pages: SortedMap<PageListType, List<PageModel>>) = uiScope.launch {
        if (pages.isNotEmpty()) {
            val pageItems = pages
                    .map { (listType, results) ->
                        listOf(Divider(resourceProvider.getString(listType.title))) +
                                results.map { it.toPageItem(pagesViewModel.arePageActionsEnabled) }
                    }
                    .fold(mutableListOf()) { acc: MutableList<PageItem>, list: List<PageItem> ->
                        acc.addAll(list)
                        return@fold acc
                    }
            _searchResult.value = pageItems
        } else {
            _searchResult.value = listOf(Empty(R.string.pages_empty_search_result, true))
        }
    }

    private fun PageModel.toPageItem(areActionsEnabled: Boolean): PageItem {
        return when (status) {
            PageStatus.PUBLISHED, PageStatus.PRIVATE ->
                PublishedPage(remoteId, title, date, actionsEnabled = areActionsEnabled)
            PageStatus.DRAFT, PageStatus.PENDING -> DraftPage(remoteId, title, date, actionsEnabled = areActionsEnabled)
            PageStatus.TRASHED -> TrashedPage(remoteId, title, date, actionsEnabled = areActionsEnabled)
            PageStatus.SCHEDULED -> ScheduledPage(remoteId, title, date, actionsEnabled = areActionsEnabled)
        }
    }
}
