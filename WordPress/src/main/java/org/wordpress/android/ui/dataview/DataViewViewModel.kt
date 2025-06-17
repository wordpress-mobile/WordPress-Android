package org.wordpress.android.ui.dataview

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.ToastUtilsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import rs.wordpress.api.kotlin.WpComApiClient
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
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val appLogWrapper: AppLogWrapper,
    private val toastUtilsWrapper: ToastUtilsWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val selectedSiteRepository: SelectedSiteRepository,
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableStateFlow(DataViewUiState.LOADING)
    val uiState: StateFlow<DataViewUiState> = _uiState

    private val _items = MutableStateFlow<List<DataViewItem>>(emptyList())
    val items = _items.asStateFlow()

    private val _itemFilter = MutableStateFlow<DataViewItemFilter?>(null)
    val itemFilter = _itemFilter.asStateFlow()

    private val debouncedQuery = MutableStateFlow("")
    private var searchQuery: String = ""
    private var page = 0

    lateinit var apiClient: WpComApiClient

    init {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag init")
        launch {
            // These credentials are from a dummy site, so it's safe to check them in during testing
            val authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(
                username = "demo",
                password = "FKnT 3P5E aIUs xCIz vb6T 20Ni"
            )
            apiClient = WpComApiClient(authProvider)
            fetchData()

            debouncedQuery
                .debounce(SEARCH_DELAY_MS)
                .collect { query ->
                    if (searchQuery != query) {
                        searchQuery = query
                        page = 0
                        fetchData()
                    }
                }
        }
    }

    fun siteId(): Long {
        return selectedSiteRepository.getSelectedSite()?.siteId ?: 0L
    }

    @Suppress("MagicNumber")
    private fun fetchData() {
        if (networkUtilsWrapper.isNetworkAvailable()) {
            val isLoadingMore = page > 0
            if (isLoadingMore) {
                updateUiState(DataViewUiState.LOADING_MORE)
            } else {
                updateUiState(DataViewUiState.LOADING)
            }

            launch {
                // simulate network delay
                delay(1000L)
                val items = performNetworkRequest(
                    page = page,
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
            page = 0
            appLogWrapper.d(AppLog.T.MAIN, "$logTag onRefreshData")
            fetchData()
        }
    }

    fun onFetchMoreData() {
        if (_uiState.value != DataViewUiState.LOADING_MORE) {
            appLogWrapper.d(AppLog.T.MAIN, "$logTag onFetchMoreData")
            page++
            fetchData()
        }
    }

    fun onFilterClick(filter: DataViewItemFilter?) {
        appLogWrapper.d(AppLog.T.MAIN, "$logTag onFilterClick: $filter")
        page = 0
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

    fun showError(@StringRes msgId: Int) {
        viewModelScope.launch {
            toastUtilsWrapper.showToast(msgId)
        }
    }

    fun showError(message: String) {
        viewModelScope.launch {
            toastUtilsWrapper.showToast(message)
        }
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
        pageSize: Int = DEFAULT_PAGE_SIZE,
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
        const val DEFAULT_PAGE_SIZE = 25
    }
}
