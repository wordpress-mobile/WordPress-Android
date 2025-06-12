package org.wordpress.android.ui.dataview

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import org.wordpress.android.fluxc.utils.AppLogWrapper
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
@HiltViewModel
open class DataViewViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val appLogWrapper: AppLogWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableStateFlow(DataViewUiState.EMPTY)
    val uiState: StateFlow<DataViewUiState> = _uiState

    private val _items = MutableStateFlow<List<DataViewItem>>(emptyList())
    val items = _items.asStateFlow()

    private val _itemFilter = MutableStateFlow<DataViewItemFilter?>(null)
    val itemFilter = _itemFilter.asStateFlow()

    private var searchQuery: String = ""
    private var offset = 0

    init {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag init")
        launch {
            fetchData()
        }
    }

    @Suppress("MagicNumber")
    private fun fetchData() {
        if (networkUtilsWrapper.isNetworkAvailable()) {
            if (offset == 0) {
                appLogWrapper.d(AppLog.T.MAIN, "$logTag refreshing")
                updateUiState(DataViewUiState.LOADING)
            } else {
                appLogWrapper.d(AppLog.T.MAIN, "$logTag loading more")
                updateUiState(DataViewUiState.LOADING_MORE)
            }

            launch {
                // simulate network delay
                delay(1000L)
                val items = performNetworkRequest(
                    offset = offset,
                    searchQuery = searchQuery,
                    filter = _itemFilter.value
                )
                _items.value = items
                if (items.isEmpty()) {
                    if (searchQuery.isNotEmpty()) {
                        appLogWrapper.d(AppLog.T.MAIN, "$logTag empty search")
                        updateUiState(DataViewUiState.EMPTY_SEARCH)
                    } else {
                        appLogWrapper.d(AppLog.T.MAIN, "$logTag empty subscribers")
                        updateUiState(DataViewUiState.EMPTY)
                    }
                } else {
                    appLogWrapper.d(AppLog.T.MAIN, "$logTag loaded")
                    updateUiState(DataViewUiState.LOADED)
                }
            }
        } else {
            appLogWrapper.d(AppLog.T.MAIN, "$logTag offline")
            updateUiState(DataViewUiState.OFFLINE)
        }
    }

    fun onRefreshData() {
        offset = 0
        appLogWrapper.d(AppLog.T.MAIN, "$logTag onRefreshData")
        fetchData()
    }

    fun onFetchMoreData() {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag onFetchMoreData")
        offset += PAGE_SIZE
        fetchData()
    }

    fun onFilterClick(filter: DataViewItemFilter?) {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag filter clicked: $filter")
        offset = 0
        launch {
            _itemFilter.value = if (filter == _itemFilter.value) {
                null
            } else {
                filter
            }
            fetchData()
        }
    }

    @OptIn(FlowPreview::class)
    fun onSearchQueryChange(query: String) {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag search query changed")
        val searchFlow = MutableStateFlow(query)
        launch {
            searchFlow
                .debounce(SEARCH_DELAY_MS)
                .collect {
                    searchQuery = it
                    appLogWrapper.d(AppLog.T.MAIN, "$logTag searching")
                    delay(SEARCH_DELAY_MS)
                    offset = 0
                    fetchData()
                }
        }
    }

    private fun updateUiState(state: DataViewUiState) {
        _uiState.value = state
    }

    /**
     * Descendants should override this to perform their specific network request
     */
    open suspend fun performNetworkRequest(
        offset: Int = 0,
        searchQuery: String = "",
        filter: DataViewItemFilter? = null
    ): List<DataViewItem> {
        return emptyList()
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
        appLogWrapper.d(AppLog.T.MAIN, "$logTag item clicked: $item")
    }

    private val logTag
        get() = this::class.java.simpleName

    companion object {
        private const val SEARCH_DELAY_MS = 500L
        const val PAGE_SIZE = 25
    }
}
