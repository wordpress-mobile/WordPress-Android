package org.wordpress.android.ui.dataview

import android.content.SharedPreferences
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.wordpress.android.R
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
    private val _uiState = MutableStateFlow(DataViewUiState())
    val uiState: StateFlow<DataViewUiState> = _uiState.asStateFlow()

    private val debouncedQuery = MutableStateFlow("")

    open val emptyView = DataViewEmptyView()

    private fun updateState(update: (DataViewUiState) -> DataViewUiState) {
        _uiState.update { update(it) }
    }

    private fun updateItems(items: List<DataViewItem>, isLoadingMore: Boolean = false) {
        updateState { currentState ->
            currentState.copy(
                items = if (isLoadingMore) currentState.items + items else items,
                loadingState = if (items.isEmpty()) {
                    if (currentState.searchQuery.isNotEmpty()) LoadingState.EMPTY_SEARCH
                    else LoadingState.EMPTY
                } else LoadingState.LOADED,
                canLoadMore = items.size == PAGE_SIZE
            )
        }
    }

    private fun resetPaging() {
        updateState { currentState ->
            currentState.copy(
                currentPage = INITIAL_PAGE,
                canLoadMore = true,
                errorMessage = null
            )
        }
    }

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
    }

    protected open fun initialize() {
        launch {
            restorePrefs()
            fetchData(localData = getLocalData())

            debouncedQuery
                .debounce(SEARCH_DELAY_MS)
                .collect { query ->
                    if (_uiState.value.searchQuery != query) {
                        updateState { it.copy(searchQuery = query) }
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
        val sortOrder = WpApiParamOrder.entries.getOrNull(sortOrdinal) ?: WpApiParamOrder.ASC

        val sortById = sharedPrefs.getLong(getPrefKeyName(PrefKey.SORT_BY), -1)
        val sortBy = if (sortById > -1) {
            getSupportedSorts().firstOrNull { it.id == sortById }
        } else {
            getDefaultSort()
        }

        val filterId = sharedPrefs.getLong(getPrefKeyName(PrefKey.FILTER), -1)
        val filter = if (filterId > -1) {
            getSupportedFilters().firstOrNull { it.id == filterId }
        } else {
            null
        }

        updateState { currentState ->
            currentState.copy(
                sortOrder = sortOrder,
                currentSortBy = sortBy,
                currentFilter = filter
            )
        }
    }

    private fun fetchData(isRefreshing: Boolean = false, localData: List<DataViewItem> = emptyList()) {
        launch {
            if (localData.isNotEmpty()) {
                updateState { currentState ->
                    currentState.copy(
                        items = localData,
                        loadingState = LoadingState.LOADED,
                    )
                }
            }

            if (networkUtilsWrapper.isNetworkAvailable()) {
                val currentState = _uiState.value
                val isLoadingMore = currentState.currentPage > INITIAL_PAGE

                updateState { state ->
                    state.copy(
                        loadingState = if (isLoadingMore || localData.isNotEmpty()) {
                            LoadingState.LOADING_MORE
                        } else {
                            LoadingState.LOADING
                        },
                        isRefreshing = isRefreshing
                    )
                }

                val items = performNetworkRequest(
                    page = currentState.currentPage,
                    searchQuery = currentState.searchQuery,
                    filter = currentState.currentFilter,
                    sortOrder = currentState.sortOrder,
                    sortBy = currentState.currentSortBy,
                )

                if (_uiState.value.loadingState == LoadingState.ERROR) {
                    updateState { it.copy(isRefreshing = false) }
                    return@launch
                }

                updateItems(items, isLoadingMore)
                updateState { it.copy(isRefreshing = false) }
            } else if (localData.isEmpty()) {
                // Only show error if local data is empty
                updateState {
                    it.copy(
                        loadingState = LoadingState.OFFLINE,
                        isRefreshing = false
                    )
                }
            }
        }
    }

    open fun getLocalData(): List<DataViewItem> = emptyList()

    // resetPaging() is now handled by the helper function above

    fun onRefreshData() {
        if (_uiState.value.loadingState == LoadingState.LOADED) {
            resetPaging()
            appLogWrapper.d(AppLog.T.MAIN, "$logTag onRefreshData")
            fetchData(isRefreshing = true)
        }
    }

    fun onFetchMoreData() {
        val currentState = _uiState.value
        if (currentState.loadingState != LoadingState.LOADING_MORE && currentState.canLoadMore) {
            appLogWrapper.d(AppLog.T.MAIN, "$logTag onFetchMoreData")
            updateState { it.copy(currentPage = it.currentPage + 1) }
            fetchData()
        }
    }

    fun onFilterClick(filter: DataViewDropdownItem?) {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag onFilterClick: $filter")
        resetPaging()
        val keyName = getPrefKeyName(PrefKey.FILTER)
        // clear the filter if it's already selected
        if (filter == _uiState.value.currentFilter || filter == null) {
            updateState { it.copy(currentFilter = null) }
            sharedPrefs.edit { remove(keyName) }
        } else {
            updateState { it.copy(currentFilter = filter) }
            sharedPrefs.edit { putLong(keyName, filter.id) }
        }
        fetchData()
    }

    /**
     * Returns the name of the preference key for the given [prefKey]. This relies on
     * the [logTag] so descendants will have unique names for each key.
     */
    private fun getPrefKeyName(prefKey: PrefKey): String {
        return "${logTag}_${prefKey.name}"
    }

    fun onSortClick(sort: DataViewDropdownItem) {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag onSortClick: $sort")
        if (sort != _uiState.value.currentSortBy) {
            sharedPrefs.edit { putLong(getPrefKeyName(PrefKey.SORT_BY), sort.id) }
            updateState { it.copy(currentSortBy = sort) }
            resetPaging()
            fetchData()
        }
    }

    fun onSortOrderClick(order: WpApiParamOrder) {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag onSortOrderClick: $order")
        if (order != _uiState.value.sortOrder) {
            sharedPrefs.edit { putInt(getPrefKeyName(PrefKey.SORT_ORDER), order.ordinal) }
            updateState { it.copy(sortOrder = order) }
            resetPaging()
            fetchData()
        }
    }

    fun onSearchQueryChange(query: String) {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag onSearchQueryChange")
        debouncedQuery.value = query
    }

    fun onError(message: String?) {
        updateState { it.copy(errorMessage = message, loadingState = LoadingState.ERROR) }
    }

    // updateUiState is now handled by updateState and updateLoadingState helper functions

    /**
     * Removes an item from the local list of items
     */
    fun removeItem(id: Long) {
        updateState { currentState ->
            currentState.copy(items = currentState.items.filter { it.id != id })
        }
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

    class DataViewEmptyView(
        @StringRes val messageRes: Int = R.string.dataview_default_empty_message,
        @DrawableRes val imageRes: Int = R.drawable.img_jetpack_empty_state,
    )

    companion object {
        private const val SEARCH_DELAY_MS = 500L
        const val PAGE_SIZE = 25
        private const val INITIAL_PAGE = 1
    }
}
