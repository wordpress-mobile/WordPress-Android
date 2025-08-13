package org.wordpress.android.ui.jetpackrestconnection

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.VersionUtils.checkMinimalVersion
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class JetpackRestConnectionViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val appLogWrapper: AppLogWrapper,
) : ScopedViewModel(mainDispatcher) {
    private val _currentStep = MutableStateFlow<ConnectionStep?>(null)
    val currentStep = _currentStep

    private val _uiEvent = MutableStateFlow<UiEvent?>(null)
    val uiEvent = _uiEvent

    private val _buttonType = MutableStateFlow<ButtonType?>(ButtonType.Start)
    val buttonType = _buttonType

    data class StepState(
        val status: ConnectionStatus = ConnectionStatus.NotStarted,
        val errorType: ErrorType? = null,
    )

    private val _stepStates = MutableStateFlow(initialStepStates)
    val stepStates = _stepStates

    private var job: Job? = null

    private var isWaitingForWPComLogin = false

    private fun startConnectionJob(fromStep: ConnectionStep? = null) {
        val stepInfo = fromStep?.let { " from step: $it" } ?: ""
        appLogWrapper.d(AppLog.T.API, "$TAG: Starting Jetpack connection job$stepInfo")

        _buttonType.value = null
        _uiEvent.value = null

        job?.cancel()
        job = launch {
            startStep(fromStep ?: ConnectionStep.LoginWpCom)
        }
    }

    private fun onJobCompleted() {
        appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack connection job completed")
        job?.cancel()
        _buttonType.value = ButtonType.Done
        _currentStep.value = null
    }

    private fun getNextStep(): ConnectionStep? = when (currentStep.value) {
        null -> ConnectionStep.LoginWpCom
        ConnectionStep.LoginWpCom -> ConnectionStep.InstallJetpack
        ConnectionStep.InstallJetpack -> ConnectionStep.ConnectSite
        ConnectionStep.ConnectSite -> ConnectionStep.ConnectWpCom
        ConnectionStep.ConnectWpCom -> ConnectionStep.Finalize
        ConnectionStep.Finalize -> null
    }

    private fun startNextStep() {
        // Mark current step as completed if exists
        currentStep.value?.let {
            if (_stepStates.value[it]?.status == ConnectionStatus.InProgress) {
                updateStepStatus(it, ConnectionStatus.Completed)
            }
        }

        // Start the next step if there is one
        getNextStep()?.let {
            startStep(it)
        }
    }

    private fun startStep(step: ConnectionStep) {
        appLogWrapper.d(AppLog.T.API, "$TAG: Starting step: $step")
        _currentStep.value = step
        updateStepStatus(step, ConnectionStatus.InProgress)
        if (step == ConnectionStep.LoginWpCom) {
            loginWpCom()
        } else {
            launch {
                executeStepWithErrorHandling(step)
                // TODO this is just to test the UI
                delay(STEP_DELAY_MS)
                updateStepStatus(step, ConnectionStatus.Completed)
            }
        }
    }

    private fun updateStepStatus(
        step: ConnectionStep,
        status: ConnectionStatus,
        error: ErrorType? = null
    ) {
        appLogWrapper.d(AppLog.T.API, "$TAG: updateStepStatus $step -> $status${error?.let { " (error: $it)" } ?: ""}")
        _stepStates.value = _stepStates.value.toMutableMap().apply {
            this[step] = StepState(status = status, errorType = error)
        }

        when (status) {
            ConnectionStatus.Failed -> {
                job?.cancel()
                _currentStep.value = null
                _buttonType.value = ButtonType.Retry
            }

            ConnectionStatus.Completed -> {
                if (step == ConnectionStep.Finalize) {
                    onJobCompleted()
                } else {
                    startNextStep()
                }
            }

            else -> {}
        }
    }

    fun onStartClick() {
        appLogWrapper.d(AppLog.T.API, "$TAG: Start clicked")
        startConnectionJob()
    }

    fun onCloseClick() {
        appLogWrapper.d(AppLog.T.API, "$TAG: Close clicked")
        if (isActive()) {
            // Connection is in progress, show confirmation dialog
            appLogWrapper.d(AppLog.T.API, "$TAG: Connection in progress, showing confirmation")
            setUiEvent(UiEvent.ShowCancelConfirmation)
        } else {
            // No active connection, close immediately
            setUiEvent(UiEvent.Close)
        }
    }

    fun onCancelConfirmed() {
        appLogWrapper.d(AppLog.T.API, "$TAG: Cancel confirmed")
        job?.cancel()
        setUiEvent(UiEvent.Close)
    }

    fun onCancelDismissed() {
        appLogWrapper.d(AppLog.T.API, "$TAG: Cancel dismissed, continuing connection")
    }

    fun onRetryClick() {
        appLogWrapper.d(AppLog.T.API, "$TAG: Retry clicked")
        // Find the failed step from stepStates
        val stepToRetry = _stepStates.value.entries.find { (_, state) ->
            state.status == ConnectionStatus.Failed
        }?.key

        stepToRetry?.let { step ->
            // Only reset the failed step status, keep other steps intact
            _stepStates.value = _stepStates.value.toMutableMap().apply {
                this[step] = StepState()
            }
            startConnectionJob(fromStep = step)
        } ?: run {
            // Fallback to original behavior if no failed step found
            clearValues()
            startConnectionJob()
        }
    }

    private fun clearValues() {
        _uiEvent.value = null
        _stepStates.value = initialStepStates
        _buttonType.value = null
        _currentStep.value = null
    }

    private fun isActive(): Boolean = job?.isActive == true || run {
        // if there's a current step, and it's not failed, then it's active
        val step = currentStep.value
        step != null && _stepStates.value[step]?.status != ConnectionStatus.Failed
    }

    private fun setUiEvent(event: UiEvent) {
        appLogWrapper.d(AppLog.T.API, "$TAG: setUiEvent $event")
        // Clear the event first or else it won't be observed if its the same as the previous event
        _uiEvent.value = null
        _uiEvent.value = event
    }

    @Suppress("TooGenericExceptionCaught", "Unused", "UnusedPrivateMember")
    private suspend fun executeStepWithErrorHandling(step: ConnectionStep) {
        try {
            withContext(bgDispatcher) {
                withTimeout(STEP_TIMEOUT_MS) {
                    executeNetworkRequest(step)
                }
            }
        } catch (e: Exception) {
            appLogWrapper.e(AppLog.T.API, "$TAG: Error in step $step: ${e.message}")
            val errorType = when (e) {
                is TimeoutCancellationException -> ErrorType.Timeout(e.message)
                else -> ErrorType.Unknown(e.message)
            }
            updateStepStatus(
                step = step,
                status = ConnectionStatus.Failed,
                error = errorType,
            )
        }
    }

    private fun executeNetworkRequest(step: ConnectionStep) {
        when (step) {
            ConnectionStep.LoginWpCom -> {
                // noop - this is handled separately since it doesn't use a coroutine
            }

            ConnectionStep.InstallJetpack -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Installing Jetpack")
                // TODO
            }

            ConnectionStep.ConnectSite -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Connecting site")
                // TODO
            }

            ConnectionStep.ConnectWpCom -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Connecting WordPress.com user")
                // TODO
            }

            ConnectionStep.Finalize -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Finalizing connection")
                // TODO
            }
        }
    }

    private fun loginWpCom() {
        appLogWrapper.d(AppLog.T.API, "$TAG: Starting WordPress.com login")
        // TODO skip if the account store token already exists, but for now don't do this to make testing easier
        isWaitingForWPComLogin = true
        _uiEvent.value = UiEvent.StartWPComLogin
    }

    /**
     * Called by the activity when WordPress.com login flow completes
     */
    fun onWPComLoginCompleted(success: Boolean) {
        if (!isWaitingForWPComLogin) {
            appLogWrapper.w(AppLog.T.API, "$TAG: WordPress.com login completed, but not waiting for it")
            return
        }

        isWaitingForWPComLogin = false
        launch {
            if (success) {
                // Login successful
                appLogWrapper.d(AppLog.T.API, "$TAG: WordPress.com login successful")
                updateStepStatus(ConnectionStep.LoginWpCom, ConnectionStatus.Completed)
            } else {
                // Login failed or was cancelled
                appLogWrapper.e(AppLog.T.API, "$TAG: WordPress.com login failed or cancelled")
                updateStepStatus(
                    ConnectionStep.LoginWpCom,
                    ConnectionStatus.Failed,
                    ErrorType.FailedToLoginWpCom
                )
            }
        }
    }

    @Suppress("Unused", "UnusedPrivateMember")
    private fun getSite() = selectedSiteRepository.getSelectedSite()
        ?: error("No site is currently selected in SelectedSiteRepository")

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
        data object Failed : ConnectionStatus()
    }

    sealed class UiEvent {
        data object StartWPComLogin : UiEvent()
        data object Close : UiEvent()
        data object ShowCancelConfirmation : UiEvent()
    }

    sealed class ErrorType(open val message: String? = null) {
        data object FailedToLoginWpCom : ErrorType()
        data object FailedToConnectWpCom : ErrorType()
        data class Timeout(override val message: String? = null) : ErrorType(message)
        data class Offline(override val message: String? = null) : ErrorType(message)
        data class Unknown(override val message: String? = null) : ErrorType(message)
    }

    sealed class ButtonType {
        data object Start : ButtonType()
        data object Done : ButtonType()
        data object Retry : ButtonType()
    }

    companion object {
        private const val TAG = "JetpackRestConnectionViewModel"
        private const val LIMIT_VERSION = "14.2"
        private const val STEP_TIMEOUT_MS = 30000L // 30 seconds timeout per step
        private const val STEP_DELAY_MS = 2000L

        /**
         * Requirements:
         * - Self-hosted site authenticated with application password, and
         * - the site isn't already connected to Jetpack, and
         * - Jetpack is not installed or the installed jetpack version is 14.2 or above
         */
        fun canInitiateJetpackRestConnection(site: SiteModel): Boolean {
            return site.isUsingSelfHostedRestApi
                    && !site.wpApiRestUrl.isNullOrEmpty()
                    && !site.isJetpackConnected
                    && (!site.isJetpackInstalled || checkMinimalVersion(site.jetpackVersion, LIMIT_VERSION))
        }

        private val initialStepStates = mapOf(
            ConnectionStep.LoginWpCom to StepState(),
            ConnectionStep.InstallJetpack to StepState(),
            ConnectionStep.ConnectSite to StepState(),
            ConnectionStep.ConnectWpCom to StepState(),
            ConnectionStep.Finalize to StepState()
        )
    }
}
