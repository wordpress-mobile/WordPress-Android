package org.wordpress.android.viewmodel.pages

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.pages.PageModel
import org.wordpress.android.models.pages.PageStatus
import org.wordpress.android.models.pages.PageStatus.DRAFT
import org.wordpress.android.models.pages.PageStatus.PUBLISHED
import org.wordpress.android.models.pages.PageStatus.SCHEDULED
import org.wordpress.android.models.pages.PageStatus.TRASHED
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.DraftPage
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.PublishedPage
import org.wordpress.android.ui.pages.PageItem.ScheduledPage
import org.wordpress.android.ui.pages.PageItem.TrashedPage
import javax.inject.Inject

class PageListViewModel
@Inject constructor(val dispatcher: Dispatcher) : ViewModel() {
    private val _pages: MutableLiveData<List<PageItem>> = MutableLiveData()
    val pages: LiveData<List<PageItem>> = _pages

    private var isStarted: Boolean = false
    private var site: SiteModel? = null
    private lateinit var pageType: PageStatus

    private lateinit var pagesViewModel: PagesViewModel

    private val refreshPagesObserver = Observer<Unit> {
        loadPages()
    }

    private fun loadPages() {
        val newPages = pagesViewModel.pages
                .filter { it.status == pageType }
                .let {
                    topologicalSort(it, 0)
                }
                .map {
                    when (it.status) {
                        PUBLISHED -> PublishedPage(it.pageId, it.title, getPageItemIndent(it))
                        DRAFT -> DraftPage(it.pageId, it.title, getPageItemIndent(it))
                        SCHEDULED -> ScheduledPage(it.pageId, it.title, getPageItemIndent(it))
                        TRASHED -> TrashedPage(it.pageId, it.title, getPageItemIndent(it))
                    }
                }

        if (newPages.isEmpty()) {
            _pages.postValue(listOf(Empty(string.empty_list_default)))
        } else {
            _pages.postValue(newPages)
        }
    }

    private fun topologicalSort(pages: List<PageModel>, parentId: Long): List<PageModel> {
        val sortedList = mutableListOf<PageModel>()
        pages.filter { it.parentId == parentId }.forEach {
            sortedList += it
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
            loadPages()

            pagesViewModel.refreshPages.observeForever(refreshPagesObserver)
        }
    }

    override fun onCleared() {
        this.site = null
        pagesViewModel.refreshPages.removeObserver(refreshPagesObserver)
    }

    fun onAction(action: Action, pageItem: PageItem): Boolean {
        TODO("not implemented")
    }

    enum class PageListState {
        CAN_LOAD_MORE,
        DONE,
        ERROR,
        FETCHING,
        LOADING_MORE
    }
}
