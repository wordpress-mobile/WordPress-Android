package org.wordpress.android.ui.subscribers

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.dataview.DataViewItem
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SubscribersViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    appLogWrapper: AppLogWrapper,
    networkUtilsWrapper: NetworkUtilsWrapper
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableStateFlow(DataViewUiState.EMPTY)
    val uiState: StateFlow<DataViewUiState> = _uiState

    private val _subscribers = MutableStateFlow<List<DataViewItem>>(emptyList())
    val subscribers: StateFlow<List<DataViewItem>> = _subscribers

    init {
        appLogWrapper.d(AppLog.T.MAIN, "${this.javaClass.simpleName} init")
        if (networkUtilsWrapper.isNetworkAvailable()) {
            updateUiState(DataViewUiState.LOADING)
        } else {
            updateUiState(DataViewUiState.OFFLINE)
        }
    }

    enum class DataViewUiState {
        LOADING,
        LOADED,
        EMPTY,
        OFFLINE
    }

    fun updateUiState(uiState: DataViewUiState) {
        _uiState.value = uiState
    }

    fun updateData(data: List<DataViewItem>) {
        _subscribers.value = data
        if (data.isEmpty()) {
            updateUiState(DataViewUiState.EMPTY)
        } else {
            updateUiState(DataViewUiState.LOADED)
        }
    }

    fun onItemClick(item: DataViewItem) {
        // Handle item click
    }
}
