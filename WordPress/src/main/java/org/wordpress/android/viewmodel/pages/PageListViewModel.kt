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
import org.wordpress.android.models.pages.PageStatus.PENDING
import org.wordpress.android.models.pages.PageStatus.PUBLISHED
import org.wordpress.android.models.pages.PageStatus.SCHEDULED
import org.wordpress.android.models.pages.PageStatus.TRASHED
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.DraftPage
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.PageItem.PublishedPage
import org.wordpress.android.ui.pages.PageItem.ScheduledPage
import org.wordpress.android.ui.pages.PageItem.TrashedPage
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

class PageListViewModel
@Inject constructor(val dispatcher: Dispatcher) : ViewModel() {
    private val _pages: MutableLiveData<List<PageItem>> = MutableLiveData()
    val pages: LiveData<List<PageItem>> = _pages

    private val _editPage = SingleLiveEvent<PageModel>()
    val editPage: LiveData<PageModel>
        get() = _editPage

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
                            val label = if (it.hasLocalChanges) string.local_draft else null
                            PublishedPage(it.remoteId, it.title, label, getPageItemIndent(it))
                        }
                        PENDING, DRAFT -> {
                            val label = if (it.hasLocalChanges) string.local_changes else null
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
        pagesViewModel.refreshPagesAsync(true)
    }

    fun onAction(action: Action, pageItem: Page): Boolean {
        when (action) {
            else -> _editPage.postValue(pagesViewModel.pages.first { it.remoteId == pageItem.id })
        }
        return true
    }

    enum class PageListState {
        CAN_LOAD_MORE,
        DONE,
        ERROR,
        FETCHING,
        LOADING_MORE
    }
}
