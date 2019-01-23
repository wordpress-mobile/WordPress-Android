package org.wordpress.android.viewmodel.pages

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged
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
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.toFormattedDateString
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.FETCHING
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.DRAFTS
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.PUBLISHED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.SCHEDULED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.TRASHED
import javax.inject.Inject

class PageListViewModel @Inject constructor(
    private val mediaStore: MediaStore,
    private val dispatcher: Dispatcher
) : ViewModel() {
    private val _pages: MutableLiveData<List<PageItem>> = MutableLiveData()
    val pages: LiveData<Pair<List<PageItem>, Boolean>> = Transformations.map(_pages) {
        Pair(it, isSitePhotonCapable)
    }

    private val _scrollToPosition = SingleLiveEvent<Int>()
    val scrollToPosition: LiveData<Int> = _scrollToPosition

    private var isStarted: Boolean = false
    private lateinit var listType: PageListType

    private lateinit var pagesViewModel: PagesViewModel

    private val featuredImageMap = HashMap<Long, String>()

    private val isSitePhotonCapable: Boolean by lazy {
        SiteUtils.isPhotonCapable(pagesViewModel.site)
    }

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

            dispatcher.register(this)
        }
    }

    override fun onCleared() {
        pagesViewModel.pages.removeObserver(pagesObserver)

        dispatcher.unregister(this)
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
        val position = _pages.value?.indexOfFirst { it is Page && it.id == remotePageId } ?: -1
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

    private fun loadPagesAsync(pages: List<PageModel>) = GlobalScope.launch {
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

    private fun getFeaturedImageUrl(featuredImageId: Long): String? {
        if (featuredImageId == 0L) {
            return null
        } else if (featuredImageMap.containsKey(featuredImageId)) {
            return featuredImageMap[featuredImageId]
        }

        mediaStore.getSiteMediaWithId(pagesViewModel.site, featuredImageId)?.let { media ->
            // This should be a pretty rare case, but some media seems to be missing url
            return if (media.url != null) {
                featuredImageMap[featuredImageId] = media.url
                media.url
            } else null
        }

        // Media is not in the Store, we need to download it
        val mediaToDownload = MediaModel()
        mediaToDownload.mediaId = featuredImageId
        mediaToDownload.localSiteId = pagesViewModel.site.id

        val payload = MediaPayload(pagesViewModel.site, mediaToDownload)
        dispatcher.dispatch(MediaActionBuilder.newFetchMediaAction(payload))

        return null
    }

    private fun preparePublishedPages(pages: List<PageModel>, actionsEnabled: Boolean): List<PageItem> {
        return topologicalSort(pages, listType = PUBLISHED)
                .map {
                    val labels = mutableListOf<Int>()
                    if (it.status == PageStatus.PRIVATE)
                        labels.add(string.pages_private)
                    if (it.hasLocalChanges)
                        labels.add(string.local_changes)

                    PublishedPage(it.remoteId, it.title, it.date, labels, getPageItemIndent(it),
                            getFeaturedImageUrl(it.featuredImageId), actionsEnabled)
                }
    }

    private fun prepareScheduledPages(pages: List<PageModel>, actionsEnabled: Boolean): List<PageItem> {
        return pages.asSequence().groupBy { it.date.toFormattedDateString() }
                .map { (date, results) -> listOf(Divider(date)) +
                        results.map { ScheduledPage(it.remoteId, it.title, it.date,
                                getFeaturedImageUrl(it.featuredImageId), actionsEnabled) }
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

            DraftPage(it.remoteId, it.title, it.date, labels,
                    getFeaturedImageUrl(it.featuredImageId), actionsEnabled)
        }
    }

    private fun prepareTrashedPages(pages: List<PageModel>, actionsEnabled: Boolean): List<PageItem> {
        return pages.map {
            TrashedPage(it.remoteId, it.title, it.date, getFeaturedImageUrl(it.featuredImageId), actionsEnabled)
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

    private fun invalidateFeaturedMedia(vararg featuredImageIds: Long) {
        featuredImageIds.forEach { featuredImageMap.remove(it) }
        pagesViewModel.onImagesChanged()
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMediaChanged(event: OnMediaChanged) {
        if (!event.isError && event.mediaList != null) {
            invalidateFeaturedMedia(*event.mediaList.map { it.mediaId }.toLongArray())
        }
    }
}
