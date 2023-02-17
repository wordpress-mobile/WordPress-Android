package org.wordpress.android.ui.jpfullplugininstall.install

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class JetpackFullPluginInstallViewModel @Inject constructor(
    private val uiStateMapper: JetpackFullPluginInstallUiStateMapper,
    private val selectedSiteRepository: SelectedSiteRepository,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) : ScopedViewModel(bgDispatcher) {
    private val _uiState = MutableStateFlow<UiState>(uiStateMapper.mapInitial())
    val uiState = _uiState.asStateFlow()

    private val _actionEvents = MutableSharedFlow<ActionEvent>()
    val actionEvents = _actionEvents

    fun onContinueClick() {
        postUiState(uiStateMapper.mapInstalling())
        installJetpackPlugin()
    }

    fun onDismissScreenClick() {
        postActionEvent(ActionEvent.Dismiss)
    }

    fun onDoneClick() {
        postActionEvent(ActionEvent.Dismiss)
    }

    fun onRetryClick() {
        postUiState(uiStateMapper.mapInstalling())
        installJetpackPlugin()
    }

    fun onContactSupportClick() {
        postActionEvent(
            ActionEvent.ContactSupport(
                origin = HelpActivity.Origin.JETPACK_INSTALL_FULL_PLUGIN_ERROR,
                selectedSite = selectedSiteRepository.getSelectedSite(),
            )
        )
    }

    @Suppress("MagicNumber")
    private fun installJetpackPlugin() {
        // TODO replace mock with endpoint call and update unit tests
        launch {
            delay(2000L)
            val success = true
            if (success) {
                postUiState(uiStateMapper.mapDone())
            } else {
                postUiState(uiStateMapper.mapError())
            }
        }
    }

    private fun postUiState(uiState: UiState) {
        launch {
            _uiState.update { uiState }
        }
    }

    private fun postActionEvent(actionEvent: ActionEvent) {
        launch {
            _actionEvents.emit(actionEvent)
        }
    }
}
