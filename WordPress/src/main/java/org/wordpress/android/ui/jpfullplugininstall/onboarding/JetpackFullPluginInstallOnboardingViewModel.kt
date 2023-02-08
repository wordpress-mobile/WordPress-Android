package org.wordpress.android.ui.jpfullplugininstall.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.wordpress.android.ui.jpfullplugininstall.JetpackFullPluginInstallOnboardingUiStateMapper
import javax.inject.Inject

@HiltViewModel
class JetpackFullPluginInstallOnboardingViewModel @Inject constructor(
    private val uiStateMapper: JetpackFullPluginInstallOnboardingUiStateMapper
) : ViewModel() {
    private val _actionEvents = Channel<ActionEvent>(Channel.BUFFERED)
    val actionEvents = _actionEvents.receiveAsFlow()

    private val _uiState = Channel<UiState>(Channel.BUFFERED)
    val uiState = _uiState.receiveAsFlow()

    fun start() {
        postUiState(
            uiStateMapper.mapLoaded(::onTermsAndConditionsClick, ::onInstallFullPluginClick, ::onContactSupportClick)
        )
    }

    private fun postUiState(uiState: UiState) {
        viewModelScope.launch {
            _uiState.send(uiState)
        }
    }

    private fun postActionEvent(actionEvent: ActionEvent) {
        viewModelScope.launch {
            _actionEvents.send(actionEvent)
        }
    }

    private fun onTermsAndConditionsClick() {

    }

    private fun onInstallFullPluginClick() {

    }

    private fun onContactSupportClick() {

    }

    sealed class UiState {
        data class Loaded(
            val siteName: String,
            val pluginName: String,
            val onTermsAndConditionsClick: () -> Unit,
            val onInstallFullPluginClick: () -> Unit,
            val onContactSupportClick: () -> Unit,
        ) : UiState()
    }

    sealed class ActionEvent {

    }
}
