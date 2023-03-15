package org.wordpress.android.ui.main.jetpack.move

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class JetpackMoveViewModel @Inject constructor(
) : ViewModel() {
    private var isStarted = false

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    private lateinit var data: Data

    fun start(data: Data) {
        if (isStarted) return else isStarted = true
        this.data = data

        _uiState.value = UiState.Content(
            featureName = data.featureName
        )
    }

    sealed class UiState {
        object Loading : UiState()
        data class Content(
            val featureName: String
        ) : UiState()
    }

    sealed class Event {
        object Noop : Event()
    }

    sealed class Data(
        val featureName: String
    ) : Parcelable

    @Parcelize
    object StatsData : Data("Stats")
}
