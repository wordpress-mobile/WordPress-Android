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
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.VersionUtils.checkMinimalVersion
import org.wordpress.android.viewmodel.ScopedViewModel
import uniffi.wp_api.PluginStatus
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class JetpackRestConnectionViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val jetpackInstaller: JetpackInstaller,
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

    /**
     * Called when all steps have completed successfully
     */
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

    /**
     * Mark current step as completed if it exists then start the next step if there is one
     */
    private fun startNextStep() {
        currentStep.value?.let {
            if (_stepStates.value[it]?.status == ConnectionStatus.InProgress) {
                updateStepStatus(it, ConnectionStatus.Completed)
            }
        }

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
            }
        }
    }

    /**
     * Updates the status of the passed step, starts the next step if the current step was completed successfully
     */
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

    /**
     * User clicked the button to start the connection flow
     */
    fun onStartClick() {
        appLogWrapper.d(AppLog.T.API, "$TAG: Start clicked")
        startConnectionJob()
    }

    /**
     * User clicked the close button, confirm closing if the connection is in progress, otherwise close immediately
     */
    fun onCloseClick() {
        appLogWrapper.d(AppLog.T.API, "$TAG: Close clicked")
        if (isActive()) {
            appLogWrapper.d(AppLog.T.API, "$TAG: Connection in progress, showing confirmation")
            setUiEvent(UiEvent.ShowCancelConfirmation)
        } else {
            setUiEvent(UiEvent.Close)
        }
    }

    /**
     * User confirmed the cancel dialog
     */
    fun onCancelConfirmed() {
        appLogWrapper.d(AppLog.T.API, "$TAG: Cancel confirmed")
        job?.cancel()
        setUiEvent(UiEvent.Close)
    }

    /**
     * User dismissed the cancel dialog
     */
    fun onCancelDismissed() {
        appLogWrapper.d(AppLog.T.API, "$TAG: Cancel dismissed, continuing connection")
    }

    /**
     * User clicked the retry button after a step failed, retry from the failed step
     */
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

    /**
     * Returns true if the connection job is active
     */
    private fun isActive(): Boolean = job?.isActive == true || run {
        val step = currentStep.value
        step != null && _stepStates.value[step]?.status != ConnectionStatus.Failed
    }

    /**
     * Sets the UI event to be observed by the UI. Note it's cleared first or else it won't be observed if it's
     * the same as the previous event
     */
    private fun setUiEvent(event: UiEvent) {
        appLogWrapper.d(AppLog.T.API, "$TAG: setUiEvent $event")
        _uiEvent.value = null
        _uiEvent.value = event
    }

    @Suppress("TooGenericExceptionCaught", "Unused", "UnusedPrivateMember")
    private suspend fun executeStepWithErrorHandling(step: ConnectionStep) {
        try {
            withContext(bgDispatcher) {
                withTimeout(STEP_TIMEOUT_MS) {
                    executeStep(step)
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

    private suspend fun executeStep(step: ConnectionStep) {
        when (step) {
            ConnectionStep.LoginWpCom -> {
                // handled separately since it doesn't require a coroutine and shouldn't time out
            }

            ConnectionStep.InstallJetpack -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Installing Jetpack")
                installJetpack()
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

    /**
     * Starts the wp.com login flow if the user isn't logged into wp.com
     */
    private fun loginWpCom() {
        if (accountStore.hasAccessToken()) {
            // User is already logged in, add a short delay before marking the step completed
            appLogWrapper.d(AppLog.T.API, "$TAG: WordPress.com access token already exists")
            launch {
                delay(UI_DELAY_MS)
                updateStepStatus(ConnectionStep.LoginWpCom, ConnectionStatus.Completed)
            }
        } else {
            isWaitingForWPComLogin = true
            setUiEvent(UiEvent.StartWPComLogin)
        }
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
        if (success) {
            appLogWrapper.d(AppLog.T.API, "$TAG: WordPress.com login successful")
            updateStepStatus(ConnectionStep.LoginWpCom, ConnectionStatus.Completed)
        } else {
            // Login failed or was cancelled
            appLogWrapper.e(AppLog.T.API, "$TAG: WordPress.com login failed or cancelled")
            updateStepStatus(
                ConnectionStep.LoginWpCom,
                ConnectionStatus.Failed,
                ErrorType.LoginWpComFailed
            )
        }
    }

    /**
     * Installs Jetpack to the current site if not already installed
     */
    private suspend fun installJetpack() {
        val result = jetpackInstaller.installJetpack(getSite())

        result.fold(
            onSuccess = { status ->
                when (status) {
                    PluginStatus.ACTIVE,
                    PluginStatus.NETWORK_ACTIVE -> {
                        updateStepStatus(
                            step = ConnectionStep.InstallJetpack,
                            status = ConnectionStatus.Completed
                        )
                    }
                    PluginStatus.INACTIVE -> {
                        updateStepStatus(
                            step = ConnectionStep.InstallJetpack,
                            status = ConnectionStatus.Failed,
                            error = ErrorType.InstallJetpackInactive
                        )
                    }
                }
            },
            onFailure = {
                updateStepStatus(
                    step = ConnectionStep.InstallJetpack,
                    status = ConnectionStatus.Failed,
                    error = ErrorType.InstallJetpackFailed
                )
            }
        )
    }

    /**
     * Gets the current site from the store
     */
    private fun getSite() =
        selectedSiteRepository.getSelectedSite() ?: error("No site is currently selected in SelectedSiteRepository")

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
        data object LoginWpComFailed : ErrorType()
        data object InstallJetpackFailed : ErrorType()
        data object InstallJetpackInactive : ErrorType()
        data object ConnectWpComFailed : ErrorType()
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
        private const val STEP_TIMEOUT_MS = 45 * 1000L
        private const val UI_DELAY_MS = 1000L

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
