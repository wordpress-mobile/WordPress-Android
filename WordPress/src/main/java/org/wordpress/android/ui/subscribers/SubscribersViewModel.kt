package org.wordpress.android.ui.subscribers

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import org.wordpress.android.R
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.dataview.DataViewItem
import org.wordpress.android.ui.dataview.DataViewItemFilter
import org.wordpress.android.ui.dataview.DataViewUiState
import org.wordpress.android.ui.dataview.DummyDataViewItems.getDummyDataViewItems
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SubscribersViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val appLogWrapper: AppLogWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableStateFlow(DataViewUiState.EMPTY)
    val uiState: StateFlow<DataViewUiState> = _uiState

    private val _subscribers = MutableStateFlow<List<DataViewItem>>(emptyList())
    val subscribers = _subscribers.asStateFlow()

    private var searchQuery: String = ""
    private var itemFilter: DataViewItemFilter? = null

    fun getFilters(context: Context) =
        listOf(
            DataViewItemFilter(
                id = ID_FILTER_EMAIL,
                title = context.getString(R.string.subscribers_filter_email_subscription)
            ),
            DataViewItemFilter(
                id = ID_FILTER__TYPE,
                title = context.getString(R.string.subscribers_filter_subscription_type)
            )
        )

    init {
        appLogWrapper.d(AppLog.T.MAIN, "$TAG init")
        launch {
            fetchData()
        }
    }

    @Suppress("MagicNumber")
    private suspend fun fetchData() {
        if (networkUtilsWrapper.isNetworkAvailable()) {
            appLogWrapper.d(AppLog.T.MAIN, "$TAG fetching")
            updateUiState(DataViewUiState.LOADING)
            delay(1000L)
            val items = getDummyDataViewItems()
            _subscribers.value = items
            if (items.isEmpty()) {
                if (searchQuery.isNotEmpty()) {
                    appLogWrapper.d(AppLog.T.MAIN, "$TAG empty search")
                    updateUiState(DataViewUiState.EMPTY_SEARCH)
                } else {
                    appLogWrapper.d(AppLog.T.MAIN, "$TAG empty subscribers")
                    updateUiState(DataViewUiState.EMPTY)
                }
            } else {
                appLogWrapper.d(AppLog.T.MAIN, "$TAG loaded")
                updateUiState(DataViewUiState.LOADED)
            }
        } else {
            appLogWrapper.d(AppLog.T.MAIN, "$TAG offline")
            updateUiState(DataViewUiState.OFFLINE)
        }
    }

    private fun updateUiState(state: DataViewUiState) {
        _uiState.value = state
    }

    fun onItemClick(item: DataViewItem) {
        appLogWrapper.d(AppLog.T.MAIN, "$TAG item clicked: $item")
    }

    fun onFilterClick(filter: DataViewItemFilter?) {
        appLogWrapper.d(AppLog.T.MAIN, "$TAG filter clicked: $filter")
        launch {
            itemFilter = filter
            fetchData()
        }
    }

    @OptIn(FlowPreview::class)
    fun onSearchQueryChange(query: String) {
        appLogWrapper.d(AppLog.T.MAIN, "$TAG search query changed")
        val searchFlow = MutableStateFlow(query)
        launch {
            searchFlow
                .debounce(SEARCH_DELAY_MS)
                .collect {
                    searchQuery = it
                    appLogWrapper.d(AppLog.T.MAIN, "$TAG searching")
                    delay(SEARCH_DELAY_MS)
                    fetchData()
                }
        }
    }

companion object {
    private const val ID_FILTER_EMAIL = 1L
    private const val ID_FILTER__TYPE = 2L
    private const val SEARCH_DELAY_MS = 500L
    private const val TAG = "SubscribersViewModel"
}
}
