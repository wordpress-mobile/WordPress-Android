package org.wordpress.android.ui.subscribers

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.R
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.dataview.DataViewFilterItem
import org.wordpress.android.ui.dataview.DataViewItem
import org.wordpress.android.ui.dataview.DataViewUiState
import org.wordpress.android.ui.dataview.DummyDataViewItems.getDummyData
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

    fun getFilters(context: Context) =
        listOf(
            DataViewFilterItem(
                id = ID_FILTER_EMAIL,
                title = context.getString(R.string.subscribers_filter_email_subscription)
            ),
            DataViewFilterItem(
                id = ID_FILER__TYPE,
                title = context.getString(R.string.subscribers_filter_subscription_type)
            )
        )

    init {
        appLogWrapper.d(AppLog.T.MAIN, "$TAG init")
        launch {
            fetchData()
        }
    }

    private suspend fun fetchData() {
        if (networkUtilsWrapper.isNetworkAvailable()) {
            appLogWrapper.d(AppLog.T.MAIN, "$TAG fetching")
            _uiState.value = DataViewUiState.LOADING
            delay(1000L)
            val items = getDummyData()
            _subscribers.value = items
            if (items.isEmpty()) {
                appLogWrapper.d(AppLog.T.MAIN, "$TAG empty")
                _uiState.value = DataViewUiState.EMPTY
            } else {
                appLogWrapper.d(AppLog.T.MAIN, "$TAG loaded")
                _uiState.value = DataViewUiState.LOADED
            }
        } else {
            appLogWrapper.d(AppLog.T.MAIN, "$TAG offline")
            _uiState.value = DataViewUiState.OFFLINE
        }
    }

    fun onItemClick(item: DataViewItem) {
        appLogWrapper.d(AppLog.T.MAIN, "$TAG item clicked: $item")
    }

    fun onFilterClick(filter: DataViewFilterItem) {
        appLogWrapper.d(AppLog.T.MAIN, "$TAG filter clicked: $filter")
    }

    fun onSearchQueryChange(query: String) {
        appLogWrapper.d(AppLog.T.MAIN, "$TAG search query changed")
    }

    companion object {
        private const val ID_FILTER_EMAIL = 1L
        private const val ID_FILER__TYPE = 2L
        private const val TAG = "SubscribersViewModel"
    }
}
