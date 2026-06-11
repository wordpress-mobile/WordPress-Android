package org.wordpress.android.ui.pagesrs

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.postsrs.PostRsErrorUtils
import org.wordpress.android.ui.postsrs.SnackbarMessage
import org.wordpress.android.ui.postsrs.data.WpServiceProvider
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import rs.wordpress.cache.kotlin.ObservableMetadataCollection
import rs.wordpress.cache.kotlin.getObservablePostMetadataCollectionWithEditContext
import rs.wordpress.cache.kotlin.hasMorePages
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.WpApiParamPostsOrderBy
import uniffi.wp_mobile.PostListFilter
import uniffi.wp_mobile_cache.ListState
import javax.inject.Inject

@HiltViewModel
internal class PagesRsListViewModel @Inject constructor(
    selectedSiteRepository: SelectedSiteRepository,
    private val serviceProvider: WpServiceProvider,
    private val resourceProvider: ResourceProvider,
    private val fluxCBridge: PageRsFluxCBridge,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel() {
    private val _tabStates = MutableStateFlow<Map<PageRsListTab, PageTabUiState>>(emptyMap())
    val tabStates: StateFlow<Map<PageRsListTab, PageTabUiState>> = _tabStates.asStateFlow()

    private val _isOpeningPage = MutableStateFlow(false)
    val isOpeningPage: StateFlow<Boolean> = _isOpeningPage.asStateFlow()

    private val collections = mutableMapOf<PageRsListTab, ObservableMetadataCollection>()
    private val initializingTabs = mutableSetOf<PageRsListTab>()
    private val userRefreshingTabs = mutableSetOf<PageRsListTab>()
    private var lastTrackedTab: PageRsListTab? = null

    private val _events = Channel<PageRsListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _snackbarMessages = Channel<SnackbarMessage>(Channel.BUFFERED)
    val snackbarMessages = _snackbarMessages.receiveAsFlow()

    private val _site: SiteModel? = selectedSiteRepository.getSelectedSite()

    init {
        if (_site == null) {
            _events.trySend(PageRsListEvent.ShowToast(R.string.blog_not_found))
            _events.trySend(PageRsListEvent.Finish)
        }
    }

    @MainThread
    fun onTabChanged(tab: PageRsListTab) {
        val site = _site ?: return
        if (tab == lastTrackedTab) return
        lastTrackedTab = tab
        analyticsTracker.track(
            Stat.PAGES_TAB_PRESSED,
            site,
            mapOf(TRACKS_SELECTED_TAB to tab.name.lowercase())
        )
    }

    @MainThread
    fun initTab(tab: PageRsListTab) {
        val site = _site ?: return
        if (collections.containsKey(tab) || initializingTabs.contains(tab)) return

        initializingTabs.add(tab)
        // Reset to a loading state so a retry after a failed init clears the prior error UI.
        updateTabUiState(tab) { PageTabUiState(isLoading = true) }

        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val collection = createCollection(site, tab)
                collections[tab] = collection
                initializingTabs.remove(tab)
                registerObservers(tab, collection)
                loadItemsForTab(tab)
                refreshTab(tab)
            } catch (e: Exception) {
                AppLog.e(AppLog.T.PAGES, "Failed to init RS page list tab", e)
                initializingTabs.remove(tab)
                updateTabUiState(tab) {
                    PageTabUiState(
                        error = friendlyErrorMessage(e),
                        isAuthError = PostRsErrorUtils.isAuthError(e)
                    )
                }
            }
        }
    }

    private suspend fun createCollection(
        site: SiteModel,
        tab: PageRsListTab
    ): ObservableMetadataCollection = withContext(Dispatchers.IO) {
        val service = serviceProvider.getService(site)
        val filter = PostListFilter(
            status = tab.statuses,
            order = tab.order,
            orderby = WpApiParamPostsOrderBy.DATE,
            search = null,
            author = emptyList()
        )
        service.posts().getObservablePostMetadataCollectionWithEditContext(
            endpointType = PostEndpointType.Pages,
            filter = filter,
            perPage = PAGE_SIZE.toUInt()
        )
    }

    private fun registerObservers(tab: PageRsListTab, collection: ObservableMetadataCollection) {
        collection.addDataObserver {
            viewModelScope.launch { loadItemsForTab(tab) }
        }
        collection.addListInfoObserver {
            viewModelScope.launch { updateListInfoForTab(tab) }
        }
    }

    @MainThread
    fun refreshTab(tab: PageRsListTab, isUserRefresh: Boolean = false) {
        val collection = collections[tab] ?: run {
            // The collection wasn't created (init failed or hasn't run). Re-attempt init so
            // a Retry tap from the error UI can recover instead of silently doing nothing.
            initTab(tab)
            return
        }

        if (isUserRefresh) {
            userRefreshingTabs.add(tab)
            updateTabUiState(tab) { copy(isRefreshing = true, error = null) }
        } else {
            updateTabUiState(tab) { copy(error = null) }
        }

        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                withContext(Dispatchers.IO) { collection.refresh() }
            } catch (e: Exception) {
                AppLog.e(AppLog.T.PAGES, "Failed to refresh tab $tab", e)
                userRefreshingTabs.remove(tab)
                val message = friendlyErrorMessage(e)
                val authError = PostRsErrorUtils.isAuthError(e)
                if (getTabUiState(tab).pages.isNotEmpty()) {
                    updateTabUiState(tab) {
                        copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                            isAuthError = authError
                        )
                    }
                    _snackbarMessages.trySend(
                        SnackbarMessage(
                            message = message,
                            actionLabel = if (authError) null
                                else resourceProvider.getString(R.string.retry),
                            onAction = if (authError) null
                                else ({ refreshTab(tab) })
                        )
                    )
                } else {
                    updateTabUiState(tab) {
                        copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = message,
                            isAuthError = authError
                        )
                    }
                }
            }
        }
    }

    @MainThread
    fun loadMorePages(tab: PageRsListTab) {
        val collection = collections[tab] ?: return
        val current = getTabUiState(tab)
        if (current.isLoadingMore || current.isRefreshing || !current.canLoadMore) return

        updateTabUiState(tab) { copy(isLoadingMore = true) }

        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                withContext(Dispatchers.IO) { collection.loadNextPage() }
            } catch (e: Exception) {
                AppLog.e(AppLog.T.PAGES, "Failed to load more for tab $tab", e)
                updateTabUiState(tab) { copy(isLoadingMore = false) }
                _snackbarMessages.trySend(
                    SnackbarMessage(friendlyErrorMessage(e))
                )
            }
        }
    }

    /**
     * Bridges the page into FluxC's database and emits [PageRsListEvent.EditPage] to open
     * it in the editor. Trashed pages are ignored for Phase 1 (Phase 4 will add the
     * "move to draft" confirmation flow).
     */
    @MainThread
    fun openPage(remotePageId: Long, tab: PageRsListTab) {
        val site = _site
        if (site == null || _isOpeningPage.value || tab == PageRsListTab.TRASHED) return
        if (!checkNetwork()) return

        _isOpeningPage.value = true
        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val lastModified = findPage(remotePageId, tab)?.lastModified
                val page = withContext(Dispatchers.IO) {
                    fluxCBridge.fetchAndBridge(remotePageId, site, lastModified)
                }
                _events.trySend(PageRsListEvent.EditPage(site, page))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.T.PAGES, "Bridge page failed", e)
                _snackbarMessages.trySend(
                    SnackbarMessage(friendlyErrorMessage(e, R.string.page_not_found))
                )
            } finally {
                _isOpeningPage.value = false
            }
        }
    }

    private fun checkNetwork(): Boolean {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _snackbarMessages.trySend(
                SnackbarMessage(resourceProvider.getString(R.string.no_network_message))
            )
            return false
        }
        return true
    }

    private fun friendlyErrorMessage(
        e: Exception,
        defaultResId: Int? = null,
    ): String = PostRsErrorUtils.friendlyErrorMessage(
        e, defaultResId, resourceProvider, networkUtilsWrapper
    )

    private fun findPage(remotePageId: Long, tab: PageRsListTab): PageRsUiModel? =
        _tabStates.value[tab]?.pages?.firstOrNull { it.remotePageId == remotePageId }

    private suspend fun loadItemsForTab(tab: PageRsListTab) {
        val collection = collections[tab] ?: return

        @Suppress("TooGenericExceptionCaught")
        try {
            val items = withContext(Dispatchers.IO) {
                collection.loadItems().map { item ->
                    item.state.toPageUiModel(item.id)
                }
            }
            updateTabUiState(tab) {
                copy(pages = items, isLoading = false, error = null, isAuthError = false)
            }
        } catch (e: Exception) {
            AppLog.e(AppLog.T.PAGES, "Failed to load items for tab $tab", e)
        }
    }

    private suspend fun updateListInfoForTab(tab: PageRsListTab) {
        val collection = collections[tab] ?: return

        val listInfo = withContext(Dispatchers.IO) { collection.listInfo() }
        val morePages = listInfo?.hasMorePages ?: false
        val fetchingFirstPage = listInfo?.state == ListState.FETCHING_FIRST_PAGE
        val isUserRefresh = userRefreshingTabs.contains(tab)

        if (!fetchingFirstPage) userRefreshingTabs.remove(tab)

        val isError = listInfo?.state == ListState.ERROR
        val hasPages = getTabUiState(tab).pages.isNotEmpty()
        val errorMessage = if (isError) {
            PostRsErrorUtils.friendlyErrorMessage(null, null, resourceProvider, networkUtilsWrapper)
        } else null

        if (isError && hasPages) {
            // Just sync state here; the snackbar is emitted by whichever action's catch
            // block (refreshTab / loadMorePages) caused the ERROR, since it can classify
            // the exception (e.g. auth) and choose the right action label.
            updateTabUiState(tab) {
                copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    canLoadMore = morePages,
                    error = null
                )
            }
        } else {
            updateTabUiState(tab) {
                copy(
                    isLoading = isLoading && fetchingFirstPage,
                    isRefreshing = isUserRefresh && fetchingFirstPage,
                    isLoadingMore = listInfo?.state == ListState.FETCHING_NEXT_PAGE,
                    canLoadMore = morePages,
                    error = errorMessage
                )
            }
        }
    }

    private fun getTabUiState(tab: PageRsListTab): PageTabUiState {
        return _tabStates.value[tab] ?: PageTabUiState(isLoading = true)
    }

    private fun updateTabUiState(tab: PageRsListTab, update: PageTabUiState.() -> PageTabUiState) {
        val current = getTabUiState(tab)
        val next = current.update()
        if (next == current) return
        _tabStates.value = _tabStates.value + (tab to next)
    }

    override fun onCleared() {
        super.onCleared()
        collections.values.forEach { it.close() }
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val TRACKS_SELECTED_TAB = "selected_tab"
    }
}
