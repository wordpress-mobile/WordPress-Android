package org.wordpress.android.viewmodel.pages

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.pages.PageModel
import org.wordpress.android.models.pages.PageStatus
import org.wordpress.android.models.pages.PageStatus.DRAFT
import org.wordpress.android.models.pages.PageStatus.PENDING
import org.wordpress.android.models.pages.PageStatus.PUBLISHED
import org.wordpress.android.models.pages.PageStatus.SCHEDULED
import org.wordpress.android.models.pages.PageStatus.TRASHED
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Action.DELETE_PERMANENTLY
import org.wordpress.android.ui.pages.PageItem.Action.MOVE_TO_DRAFT
import org.wordpress.android.ui.pages.PageItem.Action.MOVE_TO_TRASH
import org.wordpress.android.ui.pages.PageItem.Action.PUBLISH_NOW
import org.wordpress.android.ui.pages.PageItem.Action.SET_PARENT
import org.wordpress.android.ui.pages.PageItem.Action.VIEW_PAGE
import org.wordpress.android.ui.pages.PageItem.DraftPage
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.PageItem.PublishedPage
import org.wordpress.android.ui.pages.PageItem.ScheduledPage
import org.wordpress.android.ui.pages.PageItem.TrashedPage
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.LOADING_MORE
import javax.inject.Inject

class PageListViewModel
@Inject constructor(val dispatcher: Dispatcher) : ViewModel() {
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

    private val refreshPagesObserver = Observer<Unit> {
        loadPagesAsync()
    }

    private fun loadPagesAsync() = launch {
        val newPages = pagesViewModel.pages
                .filter { it.status == pageType }
                .let {
                    var list = it
                    if (pageType == PUBLISHED) {
                        val mutableList = it.toMutableList()
                        list = topologicalSort(mutableList, it.map { it.parentId }.min() ?: 0)
                        list += mutableList
                    }
                    return@let list
                }
                .map {
                    when (it.status) {
                        PUBLISHED -> {
                            val label = if (it.hasLocalChanges) string.local_changes else null
                            PublishedPage(it.remoteId, it.title, label, getPageItemIndent(it))
                        }
                        PENDING, DRAFT -> {
                            val label = if (it.hasLocalChanges) string.local_draft else null
                            DraftPage(it.remoteId, it.title, label)
                        }
                        SCHEDULED -> ScheduledPage(it.remoteId, it.title)
                        TRASHED -> TrashedPage(it.remoteId, it.title)
                    }
                }

        if (newPages.isEmpty()) {
            _pages.postValue(listOf(Empty(string.empty_list_default)))
        } else {
            _pages.postValue(newPages)
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

    fun onScrolledToBottom() {
        launch(CommonPool) {
            pagesViewModel.refreshPages(LOADING_MORE)
        }
    }

    fun onMenuAction(action: Action, pageItem: Page): Boolean {
        when (action) {
            VIEW_PAGE -> _previewPage.postValue(pagesViewModel.pages.first { it.remoteId == pageItem.id })
            SET_PARENT -> _setPageParent.postValue(pagesViewModel.pages.first { it.remoteId == pageItem.id })
            MOVE_TO_DRAFT -> _movePageToDraft.postValue(pagesViewModel.pages.first { it.remoteId == pageItem.id })
            MOVE_TO_TRASH -> _movePageToTrash.postValue(pagesViewModel.pages.first { it.remoteId == pageItem.id })
            PUBLISH_NOW -> _publishPage.postValue(pagesViewModel.pages.first { it.remoteId == pageItem.id })
            DELETE_PERMANENTLY -> _deletePage.postValue(pagesViewModel.pages.first { it.remoteId == pageItem.id })
            else -> throw IllegalArgumentException("Unexpected action type")
        }
        return true
    }

    fun onItemTapped(pageItem: Page) {
        _editPage.postValue(pagesViewModel.pages.first { it.remoteId == pageItem.id })
    }

    enum class PageListState {
        CAN_LOAD_MORE,
        DONE,
        ERROR,
        REFRESHING,
        FETCHING,
        LOADING_MORE
    }
}
