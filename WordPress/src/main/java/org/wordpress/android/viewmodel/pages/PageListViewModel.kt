package org.wordpress.android.viewmodel.pages

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.pages.PageModel
import org.wordpress.android.models.pages.PageStatus
import org.wordpress.android.models.pages.PageStatus.DRAFT
import org.wordpress.android.models.pages.PageStatus.PUBLISHED
import org.wordpress.android.models.pages.PageStatus.SCHEDULED
import org.wordpress.android.models.pages.PageStatus.TRASHED
import org.wordpress.android.networking.PageStore
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
@Inject constructor(val dispatcher: Dispatcher, private val pageStore: PageStore) : ViewModel() {
    private val _pages: MutableLiveData<List<PageItem>> = MutableLiveData()
    val pages: LiveData<List<PageItem>> = _pages

    private val _editPage = SingleLiveEvent<PageModel>()
    val editPage: LiveData<PageModel>
        get() = _editPage

    private val _previewPage = SingleLiveEvent<PageModel>()
    val previewPage: LiveData<PageModel>
        get() = _previewPage

    private val _setPageParent = SingleLiveEvent<PageModel>()
    val setPageParent: LiveData<PageModel>
        get() = _setPageParent

    private val _movePageToDraft = SingleLiveEvent<PageModel>()
    val movePageToDraft: LiveData<PageModel>
        get() = _movePageToDraft

    private val _movePageToTrash = SingleLiveEvent<PageModel>()
    val movePageToTrash: LiveData<PageModel>
        get() = _movePageToTrash

    private val _publishPage = SingleLiveEvent<PageModel>()
    val publishPage: LiveData<PageModel>
        get() = _publishPage

    private val _deletePage = SingleLiveEvent<PageModel>()
    val deletePage: LiveData<PageModel>
        get() = _deletePage

    private var isStarted: Boolean = false
    private var site: SiteModel? = null
    private lateinit var pageType: PageStatus

    private lateinit var pagesViewModel: PagesViewModel

    fun start(site: SiteModel, pageType: PageStatus, pagesViewModel: PagesViewModel) {
        this.site = site
        this.pageType = pageType
        this.pagesViewModel = pagesViewModel

        if (!isStarted) {
            isStarted = true
            loadPagesAsync()

            pagesViewModel.refreshPages.observeForever(refreshPagesObserver)
        }
    }

    override fun onCleared() {
        this.site = null
        pagesViewModel.refreshPages.removeObserver(refreshPagesObserver)
    }

    fun onMenuAction(action: Action, pageItem: Page): Boolean {
        when (action) {
            VIEW_PAGE -> _previewPage.postValue(pagesViewModel.pages.first { it.remoteId == pageItem.id })
            SET_PARENT -> _setPageParent.postValue(pagesViewModel.pages.first { it.remoteId == pageItem.id })
            MOVE_TO_DRAFT -> changePageStatus(pageItem, DRAFT)
            MOVE_TO_TRASH -> changePageStatus(pageItem, TRASHED)
            PUBLISH_NOW -> changePageStatus(pageItem, PUBLISHED)
            DELETE_PERMANENTLY -> _deletePage.postValue(pagesViewModel.pages.first { it.remoteId == pageItem.id })
        }
        return true
    }

    fun onItemTapped(pageItem: Page) {
        _editPage.postValue(pagesViewModel.pages.first { it.remoteId == pageItem.id })
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
        return topologicalSort(pages.toMutableList(), pages.map { it.parentId }.min() ?: 0)
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
                    acc
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

    private fun topologicalSort(pages: MutableList<PageModel>, parentId: Long): List<PageModel> {
        val sortedList = mutableListOf<PageModel>()
        pages.filter { it.parentId == parentId }.forEach {
            sortedList += it
            pages -= it
            sortedList += topologicalSort(pages, it.remoteId)
        }
        return sortedList
    }

    private fun getPageItemIndent(page: PageModel?): Int {
        return if (page == null) -1 else getPageItemIndent(page.parent) + 1
    }

    private fun changePageStatus(pageItem: Page, status: PageStatus) {
        pagesViewModel.pages
                .firstOrNull { it.remoteId == pageItem.id }
                ?.let { page ->
                    launch {
                        page.status = status
                        pageStore.savePage(page)
                    }
                }
    }

    enum class PageListState {
        DONE,
        ERROR,
        REFRESHING,
        FETCHING
    }
}
