package org.wordpress.android.ui.jetpackconnection

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class JetpackConnectionViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val appLogWrapper: AppLogWrapper,
) : ScopedViewModel(mainDispatcher) {
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

    fun updateStepStatus(step: ConnectionStep, status: ConnectionStatus) {
        _stepStatuses.value = _stepStatuses.value.toMutableMap().apply {
            this[step] = status
        }
    }

    fun setCurrentStep(step: ConnectionStep) {
        _currentStep.value = step
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
}
