package org.wordpress.android.ui.jpfullplugininstall.install

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PluginActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.PluginStore
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginPayload
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginConfigured
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginInstalled
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.jpfullplugininstall.install.JetpackFullPluginInstallAnalyticsTracker.Status
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
@Suppress("UnusedPrivateMember")
class JetpackFullPluginInstallViewModel @Inject constructor(
    private val uiStateMapper: JetpackFullPluginInstallUiStateMapper,
    private val selectedSiteRepository: SelectedSiteRepository,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    // adding pluginStore seems needed to allow the events Subscribe to work
    private val pluginStore: PluginStore,
    private val dispatcher: Dispatcher,
    private val analyticsTracker: JetpackFullPluginInstallAnalyticsTracker,
) : ScopedViewModel(bgDispatcher) {
    private val _uiState = MutableStateFlow<UiState>(uiStateMapper.mapInitial())
    val uiState = _uiState.asStateFlow()

    private val _actionEvents = MutableSharedFlow<ActionEvent>()
    val actionEvents = _actionEvents

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(this)
    }

    fun onInitialShown() {
        analyticsTracker.trackScreenShown(Status.Initial)
    }

    fun onInstallingShown() {
        analyticsTracker.trackScreenShown(Status.Loading)
    }

    fun onErrorShown() {
        analyticsTracker.trackScreenShown(Status.Error)
    }

    fun onContinueClick() {
        analyticsTracker.trackInstallButtonClicked()
        postUiState(uiStateMapper.mapInstalling())
        installJetpackPlugin()
    }

    fun onDismissScreenClick() {
        when (uiState.value) {
            is UiState.Initial -> Status.Initial
            is UiState.Installing -> Status.Loading
            is UiState.Error -> Status.Error
            else -> null
        }?.let { status ->
            analyticsTracker.trackCancelButtonClicked(status)
        }
        postActionEvent(ActionEvent.Dismiss)
    }

    fun onDoneClick() {
        analyticsTracker.trackDoneButtonClicked()
        postActionEvent(ActionEvent.Dismiss)
    }

    fun onRetryClick() {
        analyticsTracker.trackRetryButtonClicked()
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

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSitePluginInstalled(event: OnSitePluginInstalled) {
        if (event.isError) {
            AppLog.d(
                AppLog.T.PLUGINS,
                "Error trying to install the full Jetpack plugin: ${event.error.type} - ${event.error.message}"
            )
        }

        // Refresh the site regardless any event error if possible
        event.site?.let {
            dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(it))
        } ?: postUiState(uiStateMapper.mapError())
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSitePluginConfigured(event: OnSitePluginConfigured) {
        if (event.isError) {
            AppLog.d(
                AppLog.T.PLUGINS,
                "Error trying to configure the full Jetpack plugin: ${event.error.type} - ${event.error.message}"
            )
        }

        // Refresh the site regardless any event error if possible
        event.site?.let {
            dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(it))
        } ?: postUiState(uiStateMapper.mapError())
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        val success = if (event.isError) {
            AppLog.d(
                AppLog.T.PLUGINS,
                "Error trying to update site while installing the full " +
                        "Jetpack plugin: ${event.error.type} - ${event.error.message}"
            )
            false
        } else {
            // Check if the final goal (JP installed and active) is matched
            val selectedSite = selectedSiteRepository.getSelectedSite()
            selectedSite?.let {
                it.isJetpackInstalled && it.isJetpackConnected
            } ?: false
        }

        if (success) {
            analyticsTracker.trackJetpackInstallationSuccess()
            postUiState(uiStateMapper.mapDone())
        } else {
            postUiState(uiStateMapper.mapError())
        }
    }

    private fun installJetpackPlugin() {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        selectedSite?.let {
            val payload = InstallSitePluginPayload(it, "jetpack")
            dispatcher.dispatch(PluginActionBuilder.newInstallJpForIndividualPluginSiteAction(payload))
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
