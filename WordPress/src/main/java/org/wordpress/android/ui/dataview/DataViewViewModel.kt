package org.wordpress.android.ui.dataview

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
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
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val appLogWrapper: AppLogWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableStateFlow(DataViewUiState.LOADING)
    val uiState: StateFlow<DataViewUiState> = _uiState

    private val _items = MutableStateFlow<List<DataViewItem>>(emptyList())
    val items = _items.asStateFlow()

    private val _itemFilter = MutableStateFlow<DataViewItemFilter?>(null)
    val itemFilter = _itemFilter.asStateFlow()

    private val debouncedQuery = MutableStateFlow("")
    private var searchQuery: String = ""
    private var offset = 0

    init {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag init")
        launch {
            fetchData()
            debouncedQuery
                .debounce(SEARCH_DELAY_MS)
                .collect { query ->
                    if (searchQuery != query) {
                        searchQuery = query
                        offset = 0
                        fetchData()
                    }
                }
        }
    }

    @Suppress("MagicNumber")
    private fun fetchData() {
        if (networkUtilsWrapper.isNetworkAvailable()) {
            val isLoadingMore = offset > 0
            if (isLoadingMore) {
                updateUiState(DataViewUiState.LOADING_MORE)
            } else {
                updateUiState(DataViewUiState.LOADING)
            }

            launch {
                // simulate network delay
                delay(1000L)
                val items = performNetworkRequest(
                    offset = offset,
                    searchQuery = searchQuery,
                    filter = _itemFilter.value
                )
                if (isLoadingMore) {
                    _items.value += items
                } else {
                    _items.value = items
                }
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

    fun onRefreshData() {
        if (_uiState.value == DataViewUiState.LOADED) {
            offset = 0
            appLogWrapper.d(AppLog.T.MAIN, "$logTag onRefreshData")
            fetchData()
        }
    }

    fun onFetchMoreData() {
        if (_uiState.value != DataViewUiState.LOADING_MORE) {
            appLogWrapper.d(AppLog.T.MAIN, "$logTag onFetchMoreData")
            offset += PAGE_SIZE
            fetchData()
        }
    }

    fun onFilterClick(filter: DataViewItemFilter?) {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag onFilterClick: $filter")
        offset = 0
        // clear the filter if it's already selected
        _itemFilter.value = if (filter == _itemFilter.value) {
            null
        } else {
            filter
        }
        fetchData()
    }

    fun onSearchQueryChange(query: String) {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag onSearchQueryChange")
        debouncedQuery.value = query
    }

    private fun updateUiState(state: DataViewUiState) {
        _uiState.value = state
        appLogWrapper.d(AppLog.T.MAIN, "$logTag updateUiState: $state")
    }

    /**
     * Descendants should override this to perform their specific network request
     */
    open suspend fun performNetworkRequest(
        offset: Int = 0,
        searchQuery: String = "",
        filter: DataViewItemFilter? = null
    ): List<DataViewItem> = withContext(ioDispatcher) {
        emptyList()
    }

    /**
     * Descendants should override this to return a list of supported filters
     */
    open fun getSupportedFilters(): List<DataViewItemFilter> {
        return emptyList()
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
