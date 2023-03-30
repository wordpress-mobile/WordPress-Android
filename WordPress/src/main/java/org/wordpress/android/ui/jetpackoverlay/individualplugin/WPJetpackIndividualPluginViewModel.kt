package org.wordpress.android.ui.jetpackoverlay.individualplugin

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class WPJetpackIndividualPluginViewModel @Inject constructor(
    private val wpJetpackIndividualPluginHelper: WPJetpackIndividualPluginHelper,
    private val analyticsTracker: WPJetpackIndividualPluginAnalyticsTracker,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) : ScopedViewModel(bgDispatcher) {
    private val _uiState = MutableStateFlow<UiState>(UiState.None)
    val uiState = _uiState.asStateFlow()

    private val _actionEvents = MutableSharedFlow<ActionEvent>()
    val actionEvents = _actionEvents

    fun onScreenShown() {
        if (_uiState.value != UiState.None) return
        launch {
            val sites = wpJetpackIndividualPluginHelper.getJetpackConnectedSitesWithIndividualPlugins()
            _uiState.update { UiState.Loaded(sites) }
            wpJetpackIndividualPluginHelper.onJetpackIndividualPluginOverlayShown()
            analyticsTracker.trackScreenShown()
        }
    }

    fun onDismissScreenClick() {
        postActionEvent(ActionEvent.Dismiss)
        analyticsTracker.trackScreenDismissed()
    }

    fun onPrimaryButtonClick() {
        postActionEvent(ActionEvent.PrimaryButtonClick)
        analyticsTracker.trackPrimaryButtonClick()
    }

    private fun postActionEvent(actionEvent: ActionEvent) {
        launch { _actionEvents.emit(actionEvent) }
    }

    sealed class UiState {
        object None : UiState()
        data class Loaded(
            val sites: List<SiteWithIndividualJetpackPlugins>,
        ) : UiState()
    }

    sealed class ActionEvent {
        object PrimaryButtonClick : ActionEvent()
        object Dismiss : ActionEvent()
    }
}
