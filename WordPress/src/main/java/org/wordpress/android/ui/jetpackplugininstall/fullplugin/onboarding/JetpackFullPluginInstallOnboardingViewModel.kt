package org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding.JetpackFullPluginInstallOnboardingViewModel.ActionEvent.ContactSupport
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding.JetpackFullPluginInstallOnboardingViewModel.ActionEvent.Dismiss
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding.JetpackFullPluginInstallOnboardingViewModel.ActionEvent.OpenInstallJetpackFullPlugin
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding.JetpackFullPluginInstallOnboardingViewModel.ActionEvent.OpenTermsAndConditions
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class JetpackFullPluginInstallOnboardingViewModel @Inject constructor(
    private val uiStateMapper: JetpackFullPluginInstallOnboardingUiStateMapper,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val analyticsTracker: JetpackFullPluginInstallOnboardingAnalyticsTracker,
    private val appPrefsWrapper: AppPrefsWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) : ScopedViewModel(bgDispatcher) {
    private val _uiState = MutableStateFlow<UiState>(UiState.None)
    val uiState = _uiState.asStateFlow()

    private val _actionEvents = MutableSharedFlow<ActionEvent>()
    val actionEvents = _actionEvents

    fun onScreenShown() {
        updateOnboardingShownForCurrentSite()
        analyticsTracker.trackScreenShown()
        postUiState(uiStateMapper.mapLoaded())
    }

    fun onTermsAndConditionsClick() {
        postActionEvent(OpenTermsAndConditions)
    }

    fun onInstallFullPluginClick() {
        analyticsTracker.trackInstallButtonClick()
        postActionEvent(OpenInstallJetpackFullPlugin)
    }

    fun onContactSupportClick() {
        postActionEvent(
            ContactSupport(
                origin = HelpActivity.Origin.JETPACK_INSTALL_FULL_PLUGIN_ONBOARDING,
                selectedSite = selectedSiteRepository.getSelectedSite(),
            )
        )
    }

    fun onDismissScreenClick() {
        analyticsTracker.trackScreenDismissed()
        postActionEvent(Dismiss)
    }

    private fun updateOnboardingShownForCurrentSite() {
        selectedSiteRepository.getSelectedSite()?.id?.let {
            appPrefsWrapper.setShouldShowJetpackInstallOnboarding(it, false)
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

    sealed class UiState {
        object None : UiState()
        data class Loaded(
            val siteUrl: String,
            val pluginNames: List<String>,
        ) : UiState()
    }

    sealed class ActionEvent {
        object OpenTermsAndConditions : ActionEvent()
        object OpenInstallJetpackFullPlugin : ActionEvent()
        data class ContactSupport(
            val origin: HelpActivity.Origin,
            val selectedSite: SiteModel?,
        ) : ActionEvent()

        object Dismiss : ActionEvent()
    }
}
