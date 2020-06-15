package org.wordpress.android.ui.reader.discover

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.LoadingUiState
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import javax.inject.Inject

class ReaderDiscoverViewModel @Inject constructor(private val readerPostRepository: ReaderPostRepository) : ViewModel() {
    private val _uiState = MutableLiveData<DiscoverUiState>(LoadingUiState)
    private val uiState: LiveData<DiscoverUiState> = _uiState

    sealed class DiscoverUiState() {
        data class ContentUiState() : DiscoverUiState()
        object LoadingUiState : DiscoverUiState()
        data class ErrorUiState() : DiscoverUiState()
    }
}
