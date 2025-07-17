package org.wordpress.android.ui.dataview

import android.content.SharedPreferences
import androidx.core.content.edit
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
    private val sharedPrefs: SharedPreferences,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    @Named(IO_THREAD) protected val ioDispatcher: CoroutineDispatcher,
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableStateFlow(DataViewUiState.LOADING)
    val uiState: StateFlow<DataViewUiState> = _uiState

    private val _items = MutableStateFlow<List<DataViewItem>>(emptyList())
    val items = _items.asStateFlow()

    private val _itemFilter = MutableStateFlow<DataViewDropdownItem?>(null)
    val itemFilter = _itemFilter.asStateFlow()

    private val _itemSortBy = MutableStateFlow<DataViewDropdownItem?>(null)
    val itemSortBy = _itemSortBy.asStateFlow()

    private val _sortOrder = MutableStateFlow(WpApiParamOrder.ASC)
    val sortOrder = _sortOrder.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _refreshState = MutableStateFlow(false)
    val refreshState = _refreshState.asStateFlow()

    private val debouncedQuery = MutableStateFlow("")
    private var searchQuery: String = ""
    private var page = INITIAL_PAGE
    private var canLoadMore = true

    // TODO this is strictly for wp.com sites, we'll need different auth for self-hosted
    protected val wpComApiClient: WpComApiClient by lazy {
        WpComApiClient(
            WpAuthenticationProvider.staticWithAuth(
                requireNotNull(accountStore.accessToken) { "Access token is required but was null" }.let { token ->
                    WpAuthentication.Bearer(token = token)
                }
            )
        )
    }

    init {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag init")
        this.initialize()
    }

    protected open fun initialize() {
        launch {
            restorePrefs()
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

    /**
     * Restores the sort order, sort by, and filter from saved preferences
     */
    protected open fun restorePrefs() {
        val sortOrdinal = sharedPrefs.getInt(getPrefKeyName(PrefKey.SORT_ORDER), -1)
        if (sortOrdinal > -1) {
            WpApiParamOrder.entries.toTypedArray().getOrNull(sortOrdinal)?.let {
                _sortOrder.value = it
            }
        }

        val sortById = sharedPrefs.getLong(getPrefKeyName(PrefKey.SORT_BY), -1)
        if (sortById > -1) {
            _itemSortBy.value = getSupportedSorts().firstOrNull { it.id == sortById }
        } else {
            _itemSortBy.value = getDefaultSort()
        }

        val filterId = sharedPrefs.getLong(getPrefKeyName(PrefKey.FILTER), -1)
        if (filterId > -1) {
            _itemFilter.value = getSupportedFilters().firstOrNull { it.id == filterId }
        }
    }

    private fun fetchData(isRefreshing: Boolean = false) {
        if (networkUtilsWrapper.isNetworkAvailable()) {
            val isLoadingMore = page > INITIAL_PAGE
            if (isLoadingMore) {
                updateUiState(DataViewUiState.LOADING_MORE)
            } else {
                updateUiState(DataViewUiState.LOADING)
            }
            if (isRefreshing) {
                _refreshState.value = true
            }

            launch {
                val items = performNetworkRequest(
                    page = page,
                    searchQuery = searchQuery,
                    filter = _itemFilter.value,
                    sortOrder = _sortOrder.value,
                    sortBy = _itemSortBy.value,
                )
                if (uiState.value == DataViewUiState.ERROR) {
                    _refreshState.value = false
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
                _refreshState.value = false
            }
        } else {
            updateUiState(DataViewUiState.OFFLINE)
        }
    }

    private fun resetPaging() {
        page = INITIAL_PAGE
        canLoadMore = true
        _errorMessage.value = null
    }

    fun onRefreshData() {
        if (_uiState.value == DataViewUiState.LOADED) {
            resetPaging()
            appLogWrapper.d(AppLog.T.MAIN, "$logTag onRefreshData")
            fetchData(isRefreshing = true)
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
        val keyName = getPrefKeyName(PrefKey.FILTER)
        // clear the filter if it's already selected
        if (filter == _itemFilter.value || filter == null) {
            _itemFilter.value = null
            sharedPrefs.edit { remove(keyName) }
        } else {
            _itemFilter.value = filter
            sharedPrefs.edit { putLong(keyName, filter.id) }
        }
        fetchData()
    }

    /**
     * Returns the name of the preference key for the given [prefKey]. This relies on
     * the [logTag] so descendants will have unique names for each key.
     */
    private fun getPrefKeyName(prefKey: PrefKey) : String {
        return "${logTag}_${prefKey.name}"
    }

    fun onSortClick(sort: DataViewDropdownItem) {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag onSortClick: $sort")
        if (sort != _itemSortBy.value) {
            sharedPrefs.edit { putLong(getPrefKeyName(PrefKey.SORT_BY), sort.id) }
            _itemSortBy.value = sort
            resetPaging()
            fetchData()
        }
    }

    fun onSortOrderClick(order: WpApiParamOrder) {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag onSortOrderClick: $order")
        if (order != _sortOrder.value) {
            sharedPrefs.edit { putInt(getPrefKeyName(PrefKey.SORT_ORDER), order.ordinal) }
            _sortOrder.value = order
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
     * Removes an item from the local list of items
     */
    fun removeItem(id: Long) {
        _items.value = items.value.filter { it.id != id }
    }

    /**
     * Descendants should override this to perform their specific network request
     */
    open suspend fun performNetworkRequest(
        page: Int = INITIAL_PAGE,
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

    private enum class PrefKey {
        SORT_ORDER,
        SORT_BY,
        FILTER,
    }

    companion object {
        private const val SEARCH_DELAY_MS = 500L
        const val PAGE_SIZE = 25
        private const val INITIAL_PAGE = 1
    }
}
