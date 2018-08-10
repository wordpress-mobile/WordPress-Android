package org.wordpress.android.viewmodel.pages

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageStatus
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus.DRAFT
import org.wordpress.android.fluxc.model.page.PageStatus.PUBLISHED
import org.wordpress.android.fluxc.model.page.PageStatus.SCHEDULED
import org.wordpress.android.fluxc.model.page.PageStatus.TRASHED
import org.wordpress.android.networking.PageUploadUtil
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Action.DELETE_PERMANENTLY
import org.wordpress.android.ui.pages.PageItem.Action.MOVE_TO_DRAFT
import org.wordpress.android.ui.pages.PageItem.Action.MOVE_TO_TRASH
import org.wordpress.android.ui.pages.PageItem.Action.PUBLISH_NOW
import org.wordpress.android.ui.pages.PageItem.Action.SET_PARENT
import org.wordpress.android.ui.pages.PageItem.Action.VIEW_PAGE
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.DraftPage
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.PageItem.PublishedPage
import org.wordpress.android.ui.pages.PageItem.ScheduledPage
import org.wordpress.android.ui.pages.PageItem.TrashedPage
import org.wordpress.android.util.toFormattedDateString
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

class PageListViewModel
@Inject constructor(
    val dispatcher: Dispatcher,
    private val uploadUtil: PageUploadUtil
) : ViewModel() {
    private val _pages: MutableLiveData<List<PageItem>> = MutableLiveData()
    val pages: LiveData<List<PageItem>> = _pages

    private var isStarted: Boolean = false
    private lateinit var pageType: PageStatus

    private lateinit var pagesViewModel: PagesViewModel

    fun start(pageType: PageStatus, pagesViewModel: PagesViewModel) {
        this.pageType = pageType
        this.pagesViewModel = pagesViewModel

        if (!isStarted) {
            isStarted = true
            loadPagesAsync()

            pagesViewModel.refreshPages.observeForever(refreshPagesObserver)
        }
    }

    override fun onCleared() {
        pagesViewModel.refreshPages.removeObserver(refreshPagesObserver)
    }

    fun onMenuAction(action: Action, pageItem: Page): Boolean {
        return pagesViewModel.onMenuAction(action, pageItem)
    }

    fun onItemTapped(pageItem: Page) {
        pagesViewModel.onItemTapped(pageItem)
    }

    private val refreshPagesObserver = Observer<Unit> {
        loadPagesAsync()
    }

    private fun loadPagesAsync() = launch {
        val newPages = pagesViewModel.pages
                .filter { it.status == pageType }
                .let {
                    when (pageType) {
                        PUBLISHED -> preparePublishedPages(it)
                        SCHEDULED -> prepareScheduledPages(it)
                        DRAFT -> prepareDraftPages(it)
                        TRASHED -> prepareTrashedPages(it)
                    }
                }

        if (newPages.isEmpty()) {
            _pages.postValue(listOf(Empty(string.empty_list_default)))
        } else {
            _pages.postValue(newPages)
        }
    }

    private fun preparePublishedPages(pages: List<PageModel>): List<PageItem> {
        return topologicalSort(pages.toMutableList())
                .map {
                    val label = if (it.hasLocalChanges) string.local_changes else null
                    PublishedPage(it.remoteId, it.title, label, getPageItemIndent(it))
                }
    }

    private fun prepareScheduledPages(pages: List<PageModel>): List<PageItem> {
        return pages.groupBy { it.date.toFormattedDateString() }
                .map { (date, results) -> listOf(Divider(date)) +
                        results.map { ScheduledPage(it.pageId.toLong(), it.title) }
                }
                .fold(mutableListOf()) { acc: MutableList<PageItem>, list: List<PageItem> ->
                    acc.addAll(list)
                    return@fold acc
                }
    }

    private fun prepareDraftPages(pages: List<PageModel>): List<PageItem> {
        return pages.map {
            val label = if (it.hasLocalChanges) string.local_draft else null
            DraftPage(it.remoteId, it.title, label)
        }
    }

    private fun prepareTrashedPages(pages: List<PageModel>): List<PageItem> {
        return pages.map {
            TrashedPage(it.remoteId, it.title)
        }
    }

    private fun topologicalSort(pages: MutableList<PageModel>, parent: PageModel? = null): List<PageModel> {
        val sortedList = mutableListOf<PageModel>()
        pages.filter { it.parent == parent }.forEach {
            sortedList += it
            pages -= it
            sortedList += topologicalSort(pages, it)
        }
        return sortedList
    }

    private fun getPageItemIndent(page: PageModel?): Int {
        return if (page == null) -1 else getPageItemIndent(page.parent) + 1
    }

    enum class PageListState {
        DONE,
        ERROR,
        REFRESHING,
        FETCHING
    }
}
