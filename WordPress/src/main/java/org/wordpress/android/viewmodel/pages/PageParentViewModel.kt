package org.wordpress.android.viewmodel.pages

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat.PAGES_SET_PARENT_CHANGES_SAVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus.PUBLISHED
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.ParentPage
import org.wordpress.android.ui.pages.PageItem.Type.PARENT
import org.wordpress.android.ui.pages.PageItem.Type.TOP_LEVEL_PARENT
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

private const val SEARCH_DELAY = 200L
private const val SEARCH_COLLAPSE_DELAY = 500L

class PageParentViewModel
@Inject constructor(
    private val pageStore: PageStore,
    private val resourceProvider: ResourceProvider,
    @Named(UI_THREAD) private val uiDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val defaultDispatcher: CoroutineDispatcher
) : ScopedViewModel(uiDispatcher) {
    private val _pages: MutableLiveData<List<PageItem>> = MutableLiveData()
    val pages: LiveData<List<PageItem>> = _pages

    private lateinit var _currentParent: ParentPage
    val currentParent: ParentPage
        get() = _currentParent

    private lateinit var _initialParent: ParentPage
    val initialParent: ParentPage
        get() = _initialParent

    private val _isSaveButtonVisible = MutableLiveData<Boolean>()
    val isSaveButtonVisible: LiveData<Boolean> = _isSaveButtonVisible

    private val _saveParent = SingleLiveEvent<Unit>()
    val saveParent: LiveData<Unit> = _saveParent

    private val _searchPages: MutableLiveData<List<PageItem>> = MutableLiveData()
    val searchPages: LiveData<List<PageItem>> = _searchPages

    private var _lastSearchQuery = ""
    val lastSearchQuery: String
        get() = _lastSearchQuery

    private var searchJob: Job? = null

    private val _isSearchExpanded = MutableLiveData<Boolean>()
    val isSearchExpanded: LiveData<Boolean> = _isSearchExpanded

    private lateinit var site: SiteModel
    private var isStarted: Boolean = false

    private var page: PageModel? = null

    fun start(site: SiteModel, pageId: Long) {
        this.site = site

        if (!isStarted) {
            _pages.postValue(listOf(Empty(string.empty_list_default)))
            isStarted = true

            loadPages(pageId)

            _isSaveButtonVisible.postValue(false)
        }
    }

    private fun loadPages(pageId: Long) = launch(defaultDispatcher) {
        page = if (pageId < 0) {
            // negative local page ID used as a temp remote post ID for local-only pages (assigned by the PageStore)
            pageStore.getPageByLocalId(-pageId.toInt(), site)
        } else {
            pageStore.getPageByRemoteId(pageId, site)
        }

        val parents = mutableListOf<PageItem>(
                ParentPage(
                        0, resourceProvider.getString(R.string.top_level),
                        page?.parent == null,
                        TOP_LEVEL_PARENT
                )
        )

        if (page != null) {
            val choices = pageStore.getPagesFromDb(site)
                    .filter { it.remoteId != pageId && it.status == PUBLISHED }
            val parentChoices = choices.filter { isNotChild(it, choices) }
            if (parentChoices.isNotEmpty()) {
                parents.add(Divider(resourceProvider.getString(R.string.pages)))
                parents.addAll(parentChoices.map {
                    ParentPage(it.remoteId, it.title, page?.parent?.remoteId == it.remoteId, PARENT)
                })
            }
        }

        _currentParent = parents.firstOrNull { it is ParentPage && it.isSelected } as? ParentPage
                ?: parents.first() as ParentPage

        _initialParent = _currentParent

        _pages.postValue(parents)
    }

    fun onParentSelected(page: ParentPage) {
        _currentParent.isSelected = false
        _currentParent = page
        _currentParent.isSelected = true
        setSaveButton()
    }

    fun onSaveButtonTapped() {
        trackSaveEvent()

        _saveParent.asyncCall()
    }

    private fun trackSaveEvent() {
        val properties = mutableMapOf(
                "page_id" to page?.remoteId as Any,
                "new_parent_id" to currentParent.id
        )
        AnalyticsUtils.trackWithSiteDetails(PAGES_SET_PARENT_CHANGES_SAVED, site, properties)
    }

    private fun isNotChild(choice: PageModel, choices: List<PageModel>): Boolean {
        return !getChildren(page!!, choices).contains(choice)
    }

    private fun getChildren(page: PageModel, pages: List<PageModel>): List<PageModel> {
        val children = pages.filter { it.parent?.remoteId == page.remoteId }
        val grandchildren = mutableListOf<PageModel>()
        children.forEach {
            grandchildren += getChildren(it, pages)
        }
        return children + grandchildren
    }

    fun onSearchExpanded(restorePreviousSearch: Boolean) {
        if (isSearchExpanded.value != true) {
            if (!restorePreviousSearch) {
                clearSearch()
            }
            _isSearchExpanded.value = true
            _isSaveButtonVisible.postValue(false)
        }
    }

    fun onSearchCollapsed() {
        _isSearchExpanded.value = false
        clearSearch()
        setSaveButton()

        launch {
            delay(SEARCH_COLLAPSE_DELAY)
        }
    }

    private fun clearSearch() {
        _lastSearchQuery = ""
        _searchPages.postValue(null)
    }

    fun onSearch(searchQuery: String, delay: Long = SEARCH_DELAY) {
        searchJob?.cancel()
        if (searchQuery.isNotEmpty()) {
            searchJob = launch {
                delay(delay)
                searchJob = null
                if (isActive) {
                    _lastSearchQuery = searchQuery
                    val result = search(searchQuery)
                    _searchPages.postValue(result)
                }
            }
        } else {
            _isSaveButtonVisible.postValue(false)
            clearSearch()
        }
    }

    private suspend fun search(
        searchQuery: String
    ): List<PageItem> = withContext(defaultDispatcher) {
        _pages.value?.let {
            if (it.isNotEmpty()) return@withContext it
                    .filterIsInstance(ParentPage::class.java)
                    .filter { parentPage -> parentPage.id != 0L }
                    .filter { parentPage ->
                        parentPage.title.contains(searchQuery, true)
                    }
        }
        return@withContext mutableListOf<PageItem>()
    }

    private fun setSaveButton() {
        if (_currentParent == _initialParent) {
            _isSaveButtonVisible.postValue(false)
        } else {
            _isSaveButtonVisible.postValue(true)
        }
    }
}
