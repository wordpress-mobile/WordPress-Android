package org.wordpress.android.viewmodel.pages

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.R.string
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
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.FETCHING
import javax.inject.Inject

class PageListViewModel @Inject constructor() : ViewModel() {
    private val _pages: MutableLiveData<List<PageItem>> = MutableLiveData()
    val pages: LiveData<List<PageItem>> = _pages

    private var isStarted: Boolean = false
    private lateinit var pageStatus: PageStatus

    private lateinit var pagesViewModel: PagesViewModel

    enum class PageListState {
        DONE,
        ERROR,
        REFRESHING,
        FETCHING
    }

    fun start(pageStatus: PageStatus, pagesViewModel: PagesViewModel) {
        this.pageStatus = pageStatus
        this.pagesViewModel = pagesViewModel

        if (!isStarted) {
            isStarted = true

            pagesViewModel.pages.observeForever(pagesObserver)
        }
    }

    override fun onCleared() {
        pagesViewModel.pages.removeObserver(pagesObserver)
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

    private val pagesObserver = Observer<List<PageModel>> { pages ->
        pages?.let {
            loadPagesAsync(pages)

            pagesViewModel.checkIfNewPageButtonShouldBeVisible()
        }
    }

    private fun loadPagesAsync(pages: List<PageModel>) = launch {
        val pageItems = pages
                .filter { pageStatus == it.status }
                .let {
                    when (pageStatus) {
                        PUBLISHED -> preparePublishedPages(it, pagesViewModel.arePageActionsEnabled)
                        SCHEDULED -> prepareScheduledPages(it, pagesViewModel.arePageActionsEnabled)
                        DRAFT -> prepareDraftPages(it, pagesViewModel.arePageActionsEnabled)
                        TRASHED -> prepareTrashedPages(it, pagesViewModel.arePageActionsEnabled)
                    }
                }

        displayListItems(pageItems)
    }

    private fun displayListItems(newPages: List<PageItem>) {
        if (newPages.isEmpty()) {
            if (pagesViewModel.listState.value == FETCHING || pagesViewModel.listState.value == null) {
                _pages.postValue(listOf(Empty(string.pages_fetching, isButtonVisible = false, isImageVisible = false)))
            } else {
                when (pageStatus) {
                    PUBLISHED -> _pages.postValue(listOf(Empty(string.pages_empty_published)))
                    SCHEDULED -> _pages.postValue(listOf(Empty(string.pages_empty_scheduled)))
                    DRAFT -> _pages.postValue(listOf(Empty(string.pages_empty_drafts)))
                    TRASHED -> _pages.postValue(listOf(Empty(string.pages_empty_trashed, isButtonVisible = false)))
                }
            }
        } else {
            val pagesWithBottomGap = newPages.toMutableList()
            pagesWithBottomGap.addAll(listOf(Divider(), Divider()))
            _pages.postValue(pagesWithBottomGap)
        }
    }

    private fun preparePublishedPages(pages: List<PageModel>, actionsEnabled: Boolean): List<PageItem> {
        return topologicalSort(pages, pageStatus = PageStatus.PUBLISHED)
                .map {
                    val label = if (it.hasLocalChanges) string.local_changes else null
                    PublishedPage(it.remoteId, it.title, label, getPageItemIndent(it), actionsEnabled)
                }
    }

    private fun prepareScheduledPages(pages: List<PageModel>, actionsEnabled: Boolean): List<PageItem> {
        return pages.groupBy { it.date.toFormattedDateString() }
                .map { (date, results) -> listOf(Divider(date)) +
                        results.map { ScheduledPage(it.remoteId, it.title, actionsEnabled) }
                }
                .fold(mutableListOf()) { acc: MutableList<PageItem>, list: List<PageItem> ->
                    acc.addAll(list)
                    return@fold acc
                }
    }

    private fun prepareDraftPages(pages: List<PageModel>, actionsEnabled: Boolean): List<PageItem> {
        return pages.map {
            val label = if (it.hasLocalChanges) string.local_draft else null
            DraftPage(it.remoteId, it.title, label, actionsEnabled)
        }
    }

    private fun prepareTrashedPages(pages: List<PageModel>, actionsEnabled: Boolean): List<PageItem> {
        return pages.map {
            TrashedPage(it.remoteId, it.title, actionsEnabled)
        }
    }

    private fun topologicalSort(
        pages: List<PageModel>,
        pageStatus: PageStatus,
        parent: PageModel? = null
    ): List<PageModel> {
        val sortedList = mutableListOf<PageModel>()
        pages.filter {
            it.parent?.remoteId == parent?.remoteId || (parent == null && it.parent?.status != pageStatus)
        }
                .forEach {
                    sortedList += it
                    sortedList += topologicalSort(pages, pageStatus, it)
                }
        return sortedList
    }

    private fun getPageItemIndent(page: PageModel?): Int {
        return if (page == null || page.status != PageStatus.PUBLISHED) -1 else getPageItemIndent(page.parent) + 1
    }
}
