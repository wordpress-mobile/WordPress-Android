package org.wordpress.android.viewmodel.pages

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.Page
import javax.inject.Inject

class PageListViewModel
@Inject constructor(val dispatcher: Dispatcher) : ViewModel() {
    private val _pages: MutableLiveData<List<PageItem>> = MutableLiveData()
    val pages: LiveData<List<PageItem>> = _pages

    private var isStarted: Boolean = false
    private var site: SiteModel? = null
    private lateinit var pageType: PostStatus

    private lateinit var pagesViewModel: PagesViewModel

    private val refreshPagesObserver = Observer<Unit> {
        loadPages()
    }

    private fun loadPages() {
        val newPages = pagesViewModel.pages
                .filter { it.status == pageType }
                .map { Page(it.pageId.toLong(), it.title, null) }

        if (newPages.isEmpty()) {
            _pages.value = listOf(Empty(string.empty_list_default))
        } else {
            _pages.value = newPages
        }
    }

    fun start(site: SiteModel, pageType: PostStatus, pagesViewModel: PagesViewModel) {
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
