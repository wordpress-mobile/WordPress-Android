package org.wordpress.android.ui.jetpackconnection

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ScopedViewModel
import rs.wordpress.api.kotlin.WpComApiClient
import uniffi.wp_api.JetpackConnectionClient
import uniffi.wp_api.WpAuthentication
import uniffi.wp_api.WpAuthenticationProvider
import javax.inject.Inject
import javax.inject.Named

class JetpackConnectionViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val appLogWrapper: AppLogWrapper,
) : ScopedViewModel(mainDispatcher) {
    @Inject
    private lateinit var jetpackConnectionClient: JetpackConnectionClient

    private val _currentStep = MutableStateFlow<ConnectionStep?>(null)
    val currentStep = _currentStep

    private val _stepStatuses = MutableStateFlow(
        mapOf<ConnectionStep, ConnectionStatus>(
            ConnectionStep.LoginWpCom to ConnectionStatus.NotStarted,
            ConnectionStep.InstallJetpack to ConnectionStatus.NotStarted,
            ConnectionStep.ConnectSite to ConnectionStatus.NotStarted,
            ConnectionStep.ConnectWpCom to ConnectionStatus.NotStarted,
            ConnectionStep.Finalize to ConnectionStatus.NotStarted
        )
    )
    val stepStatuses = _stepStatuses

    private val wpComApiClient: WpComApiClient by lazy {
        WpComApiClient(
            WpAuthenticationProvider.staticWithAuth(
                WpAuthentication.Bearer(token = accountStore.accessToken!!)
            )
        )
    }

    private fun updateStepStatus(step: ConnectionStep, status: ConnectionStatus) {
        _stepStatuses.value = _stepStatuses.value.toMutableMap().apply {
            this[step] = status
        }
    }

    fun setCurrentStep(step: ConnectionStep) {
        appLogWrapper.d(AppLog.T.API, "$TAG: Setting current step to $step")
        _currentStep.value = step
        updateStepStatus(step, ConnectionStatus.InProgress)
    }

    private suspend fun networkRequest() {
        when (currentStep.value) {
            ConnectionStep.LoginWpCom -> {
                jetpackConnectionClient.connectSite(getSiteId().toString())
            }
            ConnectionStep.ConnectSite -> {
                jetpackConnectionClient.connectUser(accountStore.accessToken)
            }
        }
    }

    private fun getSiteId(): Long {
        return selectedSiteRepository.getSelectedSite()!!.siteId
    }

    sealed class ConnectionStep {
        data object LoginWpCom : ConnectionStep()
        data object InstallJetpack : ConnectionStep()
        data object ConnectSite : ConnectionStep()
        data object ConnectWpCom : ConnectionStep()
        data object Finalize : ConnectionStep()
    }

    sealed class ConnectionStatus {
        data object NotStarted : ConnectionStatus()
        data object InProgress : ConnectionStatus()
        data object Completed : ConnectionStatus()
    }

    companion object {
        private const val TAG = "JetpackConnectionViewModel"
    }
}
