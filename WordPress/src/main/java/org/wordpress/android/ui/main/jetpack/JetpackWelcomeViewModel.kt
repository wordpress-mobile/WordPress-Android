package org.wordpress.android.ui.main.jetpack

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.R.string
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

@HiltViewModel
class JetpackWelcomeViewModel @Inject constructor(
) : ViewModel() {
    private val _uiState = MutableStateFlow<JetpackWelcomeUiState>(JetpackWelcomeUiState.Initial)
    val uiState: StateFlow<JetpackWelcomeUiState> = _uiState

    fun start() {
        initOrRestoreUiState()
    }

    private fun initOrRestoreUiState() {
        // TODO load sites list
    }
}

sealed class JetpackWelcomeUiState {
    object Initial : JetpackWelcomeUiState() {
        val title: UiStringRes = UiStringRes(string.jp_welcome_title)
    }

    object Error : JetpackWelcomeUiState()
}

