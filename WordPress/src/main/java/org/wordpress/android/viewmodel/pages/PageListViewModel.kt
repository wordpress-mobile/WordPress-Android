package org.wordpress.android.viewmodel.pages

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.DraftPage
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.PageItem.PublishedPage
import org.wordpress.android.ui.pages.PageItem.ScheduledPage
import org.wordpress.android.ui.pages.PageItem.TrashedPage
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.toFormattedDateString
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.FETCHING
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.DRAFTS
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.PUBLISHED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.SCHEDULED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.TRASHED
import javax.inject.Inject

class PageListViewModel @Inject constructor() : ViewModel() {
    private val _pages: MutableLiveData<List<PageItem>> = MutableLiveData()
    val pages: LiveData<List<PageItem>> = _pages

    private val _scrollToPosition = SingleLiveEvent<Int>()
    val scrollToPosition: LiveData<Int> = _scrollToPosition

    private var isStarted: Boolean = false
    private lateinit var listType: PageListType

    private lateinit var pagesViewModel: PagesViewModel

    enum class PageListType(val pageStatuses: List<PageStatus>) {
        PUBLISHED(listOf(PageStatus.PUBLISHED, PageStatus.PRIVATE)),
        DRAFTS(listOf(PageStatus.DRAFT, PageStatus.PENDING)),
        SCHEDULED(listOf(PageStatus.SCHEDULED)),
        TRASHED(listOf(PageStatus.TRASHED));

        companion object {
            fun fromPageStatus(status: PageStatus): PageListType {
                return when (status) {
                    PageStatus.PUBLISHED, PageStatus.PRIVATE -> PUBLISHED
                    PageStatus.DRAFT, PageStatus.PENDING -> DRAFTS
                    PageStatus.TRASHED -> TRASHED
                    PageStatus.SCHEDULED -> SCHEDULED
                }
            }
        }
        val title: Int
            get() = when (this) {
                PUBLISHED -> string.pages_published
                DRAFTS -> string.pages_drafts
                SCHEDULED -> string.pages_scheduled
                TRASHED -> string.pages_trashed
            }
    }

    enum class PageListState {
        DONE,
        ERROR,
        REFRESHING,
        FETCHING
    }

    fun start(listType: PageListType, pagesViewModel: PagesViewModel) {
        this.listType = listType
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

    fun onScrollToPageRequested(remotePageId: Long) {
        val position = pages.value?.indexOfFirst { it is Page && it.id == remotePageId } ?: -1
        if (position != -1) {
            _scrollToPosition.postValue(position)
        } else {
            AppLog.e(AppLog.T.PAGES, "Attempt to scroll to a missing page with ID $remotePageId")
        }
    }

    private val pagesObserver = Observer<List<PageModel>> { pages ->
        pages?.let {
            loadPagesAsync(pages)

            pagesViewModel.checkIfNewPageButtonShouldBeVisible()
        }
    }

    private fun loadPagesAsync(pages: List<PageModel>) = launch {
        val pageItems = pages
                .sortedBy { it.title }
                .filter { listType.pageStatuses.contains(it.status) }
                .let {
                    when (listType) {
                        PUBLISHED -> preparePublishedPages(it, pagesViewModel.arePageActionsEnabled)
                        SCHEDULED -> prepareScheduledPages(it, pagesViewModel.arePageActionsEnabled)
                        DRAFTS -> prepareDraftPages(it, pagesViewModel.arePageActionsEnabled)
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
                when (listType) {
                    PUBLISHED -> _pages.postValue(listOf(Empty(string.pages_empty_published)))
                    SCHEDULED -> _pages.postValue(listOf(Empty(string.pages_empty_scheduled)))
                    DRAFTS -> _pages.postValue(listOf(Empty(string.pages_empty_drafts)))
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
        return topologicalSort(pages, listType = PUBLISHED)
                .map {
                    val labels = mutableListOf<Int>()
                    if (it.status == PageStatus.PRIVATE)
                        labels.add(string.pages_private)
                    if (it.hasLocalChanges)
                        labels.add(string.local_changes)

                    PublishedPage(it.remoteId, it.title, labels, getPageItemIndent(it), actionsEnabled)
                }
    }

    private fun prepareScheduledPages(pages: List<PageModel>, actionsEnabled: Boolean): List<PageItem> {
        return pages.asSequence().groupBy { it.date.toFormattedDateString() }
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
            val labels = mutableListOf<Int>()
            if (it.status == PageStatus.PENDING)
                labels.add(string.pages_pending)
            if (it.hasLocalChanges)
                labels.add(string.local_draft)

            DraftPage(it.remoteId, it.title, labels, actionsEnabled)
        }
    }

    private fun prepareTrashedPages(pages: List<PageModel>, actionsEnabled: Boolean): List<PageItem> {
        return pages.map {
            TrashedPage(it.remoteId, it.title, actionsEnabled)
        }
    }

    private fun topologicalSort(
        pages: List<PageModel>,
        listType: PageListType,
        parent: PageModel? = null
    ): List<PageModel> {
        val sortedList = mutableListOf<PageModel>()
        pages.filter {
            it.parent?.remoteId == parent?.remoteId ||
                    (parent == null && !listType.pageStatuses.contains(it.parent?.status))
        }.forEach {
            sortedList += it
            sortedList += topologicalSort(pages, listType, it)
        }
        return sortedList
    }

    private fun getPageItemIndent(page: PageModel?): Int {
        return if (page == null || !PageListType.PUBLISHED.pageStatuses.contains(page.status))
            -1
        else
            getPageItemIndent(page.parent) + 1
    }
}
