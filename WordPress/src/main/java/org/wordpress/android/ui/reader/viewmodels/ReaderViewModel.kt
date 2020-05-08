package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.reader.usecases.LoadReaderTabsUseCase
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ReaderViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val loadReaderTabsUseCase: LoadReaderTabsUseCase
) : ScopedViewModel(mainDispatcher) {
    private var initialized: Boolean = false
    private val _uiState = MutableLiveData<ReaderUiState>()
    val uiState: LiveData<ReaderUiState> = _uiState

    fun start() {
        if (initialized) return
        loadTabs()
    }

    private fun loadTabs() {
        launch {
            val tagList = loadReaderTabsUseCase.loadTabs()
            _uiState.value = ReaderUiState(
                    tagList.map { it.tagTitle },
                    tagList
            )
            if (tagList.isNotEmpty()) {
                initialized = true
            }
        }
    }

    data class ReaderUiState(val tabTitles: List<String>, val readerTagList: ReaderTagList)
}
