package org.wordpress.android.ui.main

import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.main.SpeedDialActionMenuItem.NEW_PAGE
import org.wordpress.android.ui.main.SpeedDialActionMenuItem.NEW_POST
import org.wordpress.android.ui.main.SpeedDialState.CLOSED
import org.wordpress.android.ui.main.SpeedDialState.HIDDEN
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class SpeedDialViewModel @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ViewModel(), CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job

    private var isStarted = false

    private val _uiState = MutableLiveData<SpeedDialUiState>()
    val uiState: LiveData<SpeedDialUiState> = _uiState

    private val _speedDialAction = SingleLiveEvent<SpeedDialAction>()
    val speedDialAction: LiveData<SpeedDialAction> = _speedDialAction

    fun start(isSpeedDialFabVisible: Boolean) {
        if (isStarted) return
        isStarted = true

        _uiState.value = SpeedDialUiState(
                speedDialState = if (isSpeedDialFabVisible) CLOSED else HIDDEN
        )
    }

    fun onSpeedDialAction(@IdRes actionId: Int): Job {
        val action = (SpeedDialActionMenuItem.fromId(actionId)).action

        updateUiState(
                speedDialFabState = CLOSED
        )
        return launch(Dispatchers.Default) {
            delay(300)
            _speedDialAction.postValue(action)
        }
    }

    fun onPageChanged(fabNewState: SpeedDialState) {
        updateUiState(
                speedDialFabState = fabNewState
        )
    }

    fun getDefaultActionsList(): List<SpeedDialActionMenuItem> = listOf(NEW_POST, NEW_PAGE)

    private fun updateUiState(speedDialFabState: SpeedDialState? = null) {
        val currentState = requireNotNull(uiState.value) {
            "updateUiState can be called only after the initial state is set"
        }

        _uiState.value = SpeedDialUiState(
                speedDialFabState ?: currentState.speedDialState
        )
    }
}
