package org.wordpress.android.ui.newstats.subscribers.subscribersgraph

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SubscribersGraphViewModel @Inject constructor()
    : ViewModel() {
    private val _uiState = MutableStateFlow<
        SubscribersGraphUiState>(
        SubscribersGraphUiState.Placeholder
    )
    val uiState: StateFlow<SubscribersGraphUiState> =
        _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> =
        _isRefreshing.asStateFlow()

    @Suppress("UnusedParameter")
    fun loadDataIfNeeded() {
        // Placeholder - no data to load yet
    }

    fun refresh() {
        // Placeholder - no data to refresh yet
    }

    fun loadData() {
        // Placeholder - no data to load yet
    }
}
