package org.wordpress.android.ui.main.jetpack.staticposter

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.models.JetpackPoweredScreen
import javax.inject.Inject

@HiltViewModel
class JetpackStaticPosterViewModel @Inject constructor(
) : ViewModel() {
    private var isStarted = false

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    private lateinit var data: UiData

    fun start(uiData: UiData) {
        if (isStarted) return else isStarted = true
        data = uiData
        _uiState.value = data.toContentUiState()
    }

    fun onPrimaryClick() {
        TODO("Not yet implemented")
    }

    fun onSecondaryClick() {
        TODO("Not yet implemented")
    }
}

sealed class UiState {
    object Loading : UiState()
    data class Content(
        val showTopBar: Boolean,
    ) : UiState()
}

sealed class Event {
    object Noop : Event()
}

typealias UiData = JetpackPoweredScreen.WithStaticPoster

fun UiData.toContentUiState() = UiState.Content(
    showTopBar = this == UiData.STATS,
)
