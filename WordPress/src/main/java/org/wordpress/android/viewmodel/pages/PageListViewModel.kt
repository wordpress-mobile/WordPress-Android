package org.wordpress.android.viewmodel.pages

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus
import org.wordpress.android.fluxc.model.page.PageStatus.DRAFT
import org.wordpress.android.fluxc.model.page.PageStatus.PUBLISHED
import org.wordpress.android.fluxc.model.page.PageStatus.SCHEDULED
import org.wordpress.android.fluxc.model.page.PageStatus.TRASHED
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.DraftPage
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.PageItem.PublishedPage
import org.wordpress.android.ui.pages.PageItem.ScheduledPage
import org.wordpress.android.ui.pages.PageItem.TrashedPage
import org.wordpress.android.util.toFormattedDateString
import javax.inject.Inject

class PageListViewModel
@Inject constructor(val dispatcher: Dispatcher) : ViewModel() {
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
            loadPagesAsync(true)

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

    fun onEmptyListNewPageButtonTapped() {
        pagesViewModel.onNewPageButtonTapped()
    }

    private val refreshPagesObserver = Observer<Unit> {
        loadPagesAsync()
    }

    private fun loadPagesAsync(isStarting: Boolean = false) = launch {
        val newPages = pagesViewModel.pages.values
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
            if (isStarting) {
                _pages.postValue(listOf(Empty(string.pages_fetching, isButtonVisible = false, isImageVisible = false)))
            } else {
                when (pageType) {
                    PUBLISHED -> _pages.postValue(listOf(Empty(string.pages_empty_published)))
                    SCHEDULED -> _pages.postValue(listOf(Empty(string.pages_empty_scheduled)))
                    DRAFT -> _pages.postValue(listOf(Empty(string.pages_empty_drafts)))
                    TRASHED -> _pages.postValue(listOf(Empty(string.pages_empty_trashed, isButtonVisible = false)))
                }
            }
        } else {
            val pagesWithBottomGap = newPages.toMutableList()
            pagesWithBottomGap.addAll(listOf(Divider(""), Divider("")))
            _pages.postValue(pagesWithBottomGap)
        }
    }

    private fun preparePublishedPages(pages: List<PageModel>): List<PageItem> {
        pages.forEach { clearNonPublishedParents(it) }
        return topologicalSort(pages)
                .map {
                    val label = if (it.hasLocalChanges) string.local_changes else null
                    PublishedPage(it.remoteId, it.title, label, getPageItemIndent(it))
                }
    }

    private fun clearNonPublishedParents(page: PageModel?) {
        if (page != null) {
            clearNonPublishedParents(page.parent)
            if (page.parent?.status != pageType) {
                page.parent = null
            }
        }
    }

    private fun prepareScheduledPages(pages: List<PageModel>): List<PageItem> {
        return pages.groupBy { it.date.toFormattedDateString() }
                .map { (date, results) -> listOf(Divider(date)) +
                        results.map { ScheduledPage(it.remoteId, it.title) }
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

    private fun topologicalSort(pages: List<PageModel>, parent: PageModel? = null): List<PageModel> {
        val sortedList = mutableListOf<PageModel>()
        pages.filter { it.parent?.remoteId == parent?.remoteId }.forEach {
            sortedList += it
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
