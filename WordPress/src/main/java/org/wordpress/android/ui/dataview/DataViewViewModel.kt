package org.wordpress.android.ui.dataview

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import rs.wordpress.api.kotlin.WpComApiClient
import uniffi.wp_api.WpApiParamOrder
import uniffi.wp_api.WpAuthentication
import uniffi.wp_api.WpAuthenticationProvider
import javax.inject.Inject
import javax.inject.Named

/**
 * Provides a basic view model for displaying, fetching, filtering,
 * and searching a list of [DataViewItem]s
 */
@OptIn(FlowPreview::class)
@HiltViewModel
open class DataViewViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val appLogWrapper: AppLogWrapper,
) : ScopedViewModel(mainDispatcher) {
    @Inject
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Inject
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Inject
    lateinit var accountStore: AccountStore

    @Inject
    @Named(IO_THREAD)
    lateinit var ioDispatcher: CoroutineDispatcher

    private val _uiState = MutableStateFlow(DataViewUiState.LOADING)
    val uiState: StateFlow<DataViewUiState> = _uiState

    private val _items = MutableStateFlow<List<DataViewItem>>(emptyList())
    val items = _items.asStateFlow()

    private val _itemFilter = MutableStateFlow<DataViewDropdownItem?>(null)
    val itemFilter = _itemFilter.asStateFlow()

    private val _itemSortBy = MutableStateFlow<DataViewDropdownItem?>(null)
    val itemSortBy = _itemSortBy.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val debouncedQuery = MutableStateFlow("")
    private var searchQuery: String = ""
    private var page = 0
    private var canLoadMore = true

    lateinit var wpComApiClient: WpComApiClient

    init {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag init")
        launch {
            // TODO this is strictly for wp.com sites, we'll need different auth for self-hosted
            wpComApiClient = WpComApiClient(
                WpAuthenticationProvider.staticWithAuth(
                    WpAuthentication.Bearer(token = accountStore.accessToken!!)
                )
            )

            _itemSortBy.value = getDefaultSort()

            fetchData()

            debouncedQuery
                .debounce(SEARCH_DELAY_MS)
                .collect { query ->
                    if (searchQuery != query) {
                        searchQuery = query
                        resetPaging()
                        fetchData()
                    }
                }
        }
    }

    fun siteId(): Long {
        return selectedSiteRepository.getSelectedSite()?.siteId ?: 0L
    }

    private fun fetchData() {
        if (networkUtilsWrapper.isNetworkAvailable()) {
            val isLoadingMore = page > 0
            if (isLoadingMore) {
                updateUiState(DataViewUiState.LOADING_MORE)
            } else {
                updateUiState(DataViewUiState.LOADING)
            }

            launch {
                val items = performNetworkRequest(
                    page = page,
                    searchQuery = searchQuery,
                    filter = _itemFilter.value,
                    sortBy = _itemSortBy.value,
                )
                if (uiState.value == DataViewUiState.ERROR) {
                    return@launch
                }

                if (isLoadingMore) {
                    _items.value += items
                } else {
                    _items.value = items
                }
                canLoadMore = items.size == PAGE_SIZE
                if (_items.value.isEmpty()) {
                    if (searchQuery.isNotEmpty()) {
                        updateUiState(DataViewUiState.EMPTY_SEARCH)
                    } else {
                        updateUiState(DataViewUiState.EMPTY)
                    }
                } else {
                    updateUiState(DataViewUiState.LOADED)
                }
            }
        } else {
            updateUiState(DataViewUiState.OFFLINE)
        }
    }

    private fun resetPaging() {
        page = 0
        canLoadMore = true
        _errorMessage.value = null
    }

    fun onRefreshData() {
        if (_uiState.value == DataViewUiState.LOADED) {
            resetPaging()
            appLogWrapper.d(AppLog.T.MAIN, "$logTag onRefreshData")
            fetchData()
        }
    }

    fun onFetchMoreData() {
        if (_uiState.value != DataViewUiState.LOADING_MORE && canLoadMore) {
            appLogWrapper.d(AppLog.T.MAIN, "$logTag onFetchMoreData")
            page++
            fetchData()
        }
    }

    fun onFilterClick(filter: DataViewDropdownItem?) {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag onFilterClick: $filter")
        resetPaging()
        // clear the filter if it's already selected
        _itemFilter.value = if (filter == _itemFilter.value) {
            null
        } else {
            filter
        }
        fetchData()
    }

    fun onSortClick(sort: DataViewDropdownItem) {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag onSortClick: $sort")
        if (sort != _itemSortBy.value) {
            _itemSortBy.value = sort
            resetPaging()
            fetchData()
        }
    }

    fun onSearchQueryChange(query: String) {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag onSearchQueryChange")
        debouncedQuery.value = query
    }

    fun onError(message: String?) {
        _errorMessage.value = message
        updateUiState(DataViewUiState.ERROR)
    }

    private fun updateUiState(state: DataViewUiState) {
        _uiState.value = state
        appLogWrapper.d(AppLog.T.MAIN, "$logTag updateUiState: $state")
    }

    /**
     * Descendants should override this to perform their specific network request
     */
    open suspend fun performNetworkRequest(
        page: Int = 0,
        searchQuery: String = "",
        filter: DataViewDropdownItem? = null,
        sortOrder: WpApiParamOrder = WpApiParamOrder.ASC,
        sortBy: DataViewDropdownItem? = null,
    ): List<DataViewItem> = withContext(ioDispatcher) {
        emptyList()
    }

    /**
     * Descendants should override this to return a list of supported filter items
     */
    open fun getSupportedFilters(): List<DataViewDropdownItem> {
        return emptyList()
    }

    /**
     * Descendants should override this to return a list of supported sort items
     */
    open fun getSupportedSorts(): List<DataViewDropdownItem> {
        return emptyList()
    }

    /**
     * Descendants can override this to return the default sorting
     */
    open fun getDefaultSort(): DataViewDropdownItem? {
        return if (getSupportedSorts().isNotEmpty()) {
            getSupportedSorts().first()
        } else {
            null
        }
    }

    /**
     * Descendants should override this to handle item clicks
     */
    open fun onItemClick(item: DataViewItem) {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag onItemClick: ${item.id}")
    }

    private val logTag
        get() = this::class.java.simpleName

    companion object {
        private const val SEARCH_DELAY_MS = 500L
        const val PAGE_SIZE = 25
    }
}
