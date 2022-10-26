package org.wordpress.android.ui.main.jetpack

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString
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
        val title: UiStringRes = UiStringRes(R.string.jp_welcome_title)
        val subtitle: UiString = UiStringRes(R.string.jp_welcome_subtitle)
        val message: UiString = UiStringRes(R.string.jp_welcome_sites_found_message)
    }

    object Error : JetpackWelcomeUiState()
}

