package org.wordpress.android.ui.dataview

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class DataViewViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val appLogWrapper: AppLogWrapper,
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableStateFlow<DataViewUiState>(DataViewUiState.LOADING)
    val uiState: StateFlow<DataViewUiState> = _uiState

    init {
        appLogWrapper.d(AppLog.T.MAIN, "${this.javaClass.simpleName} init")
    }

    enum class DataViewUiState {
        LOADING,
        LOADED,
        ERROR,
        EMPTY,
        OFFLINE
    }

    fun updateUiState(uiState: DataViewUiState) {
        _uiState.value = uiState
    }
}
