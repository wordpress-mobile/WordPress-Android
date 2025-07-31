package org.wordpress.android.ui.jetpackconnection

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
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
import uniffi.wp_api.JetpackConnectionClient
import uniffi.wp_api.WpAuthentication
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class JetpackConnectionViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val appLogWrapper: AppLogWrapper,
) : ScopedViewModel(mainDispatcher) {
    private val _currentStep = MutableStateFlow<ConnectionStep?>(null)
    val currentStep = _currentStep

    private val _uiEvent = MutableStateFlow<UiEvent?>(null)
    val uiEvent = _uiEvent

    private val _buttonType = MutableStateFlow<ButtonType?>(null)
    val buttonType = _buttonType

    data class StepState(
        val status: ConnectionStatus = ConnectionStatus.NotStarted,
        val errorMessage: String? = null
    )

    private val _stepStates = MutableStateFlow(initialStepStates)
    val stepStates = _stepStates

    private var job: Job? = null

    // TODO: Inject or initialize this properly when the actual implementation is ready
    private var jetpackConnectionClient: JetpackConnectionClient? = null

    init {
        startConnection()
    }

    private fun startConnection() {
        appLogWrapper.d(AppLog.T.API, "$TAG: Starting Jetpack connection process")
        job?.cancel()
        job = launch {
            startNextStep()
        }
    }

    private suspend fun startNextStep() {
        // Mark current step as completed if exists
        currentStep.value?.let {
            if (_stepStates.value[it]?.status == ConnectionStatus.InProgress) {
                updateStepStatus(it, ConnectionStatus.Completed)
            }
        }

        val nextStep = when (currentStep.value) {
            null -> ConnectionStep.LoginWpCom
            ConnectionStep.LoginWpCom -> ConnectionStep.InstallJetpack
            ConnectionStep.InstallJetpack -> ConnectionStep.ConnectSite
            ConnectionStep.ConnectSite -> ConnectionStep.ConnectWpCom
            ConnectionStep.ConnectWpCom -> ConnectionStep.Finalize
            ConnectionStep.Finalize -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Connection process completed")
                return
            }
        }

        appLogWrapper.d(AppLog.T.API, "$TAG: Starting step: $nextStep")
        _currentStep.value = nextStep
        updateStepStatus(nextStep, ConnectionStatus.InProgress)

        // Execute the network request for this step
        executeStepWithErrorHandling(nextStep)
    }

    private fun updateStepStatus(step: ConnectionStep, status: ConnectionStatus, error: String? = null) {
        appLogWrapper.d(AppLog.T.API, "$TAG: updateStepStatus $step -> $status${error?.let { " (error: $it)" } ?: ""}")
        _stepStates.value = _stepStates.value.toMutableMap().apply {
            this[step] = StepState(status = status, errorMessage = error)
        }

        when (status) {
            ConnectionStatus.Failed -> {
                job?.cancel()
                _buttonType.value = ButtonType.Retry
            }
            ConnectionStatus.Completed -> {
                if (step == ConnectionStep.Finalize) {
                    _buttonType.value = ButtonType.Done
                } else {
                    launch {
                        startNextStep()
                    }
                }
            }
            else -> {}
        }
    }

    fun onCloseClick() {
        _uiEvent.value = UiEvent.Close
    }

    fun onRetryClick() {
        appLogWrapper.d(AppLog.T.API, "$TAG: Retry clicked")
        clearValues()
        startConnection()
    }

    private fun clearValues() {
        _buttonType.value = null
        _stepStates.value = initialStepStates
        _currentStep.value = null
    }

    private suspend fun executeStepWithErrorHandling(step: ConnectionStep) {
        try {
            withContext(bgDispatcher) {
                withTimeout(STEP_TIMEOUT_MS) {
                    executeNetworkRequest(step)
                }
            }
            updateStepStatus(step, ConnectionStatus.Completed)
        } catch (e: Exception) {
            appLogWrapper.e(AppLog.T.API, "$TAG: Error in step $step: ${e.message}")
            val errorMessage = when (e) {
                is kotlinx.coroutines.TimeoutCancellationException -> "Operation timed out"
                else -> e.message ?: "Unknown error occurred"
            }
            updateStepStatus(step, ConnectionStatus.Failed, errorMessage)
        }
    }

    private suspend fun executeNetworkRequest(step: ConnectionStep) {
        when (step) {
            ConnectionStep.LoginWpCom -> {
                // Check if user is already logged in
                if (accountStore.hasAccessToken()) {
                    appLogWrapper.d(AppLog.T.API, "$TAG: User already logged in")
                } else {
                    throw IllegalStateException("User must be logged in to WordPress.com")
                }
            }

            ConnectionStep.InstallJetpack -> {
                val site = getSite()
                if (site.isJetpackInstalled) {
                    appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack already installed")
                } else {
                    installJetpackPlugin()
                }
            }

            ConnectionStep.ConnectSite -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Connecting site")
                jetpackConnectionClient?.connectSite(
                    from = getSiteId().toString()
                ) ?: throw IllegalStateException("JetpackConnectionClient not initialized")
            }

            ConnectionStep.ConnectWpCom -> {
                val token = accountStore.accessToken 
                    ?: throw IllegalStateException("No access token available")
                
                appLogWrapper.d(AppLog.T.API, "$TAG: Connecting WordPress.com user")
                jetpackConnectionClient?.connectUser(
                    wpComAuthentication = WpAuthentication.Bearer(token = token),
                    from = getSiteId().toString()
                ) ?: throw IllegalStateException("JetpackConnectionClient not initialized")
            }

            ConnectionStep.Finalize -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Finalizing connection")
                // Add any finalization logic here
                delay(500) // Small delay to show completion
            }
        }
    }

    private suspend fun installJetpackPlugin() {
        appLogWrapper.d(AppLog.T.API, "$TAG: Installing Jetpack plugin")
        // TODO: Implement actual plugin installation API call when ready
        // val params = PluginCreateParams(
        //     slug = PluginWpOrgDirectorySlug("jetpack"),
        //     status = PluginStatus.ACTIVE,
        // )
        // TODO: Implement actual plugin installation API call
        // For now, simulate network delay
        delay(2000)
    }

    private fun getSiteId() = getSite().siteId

    private fun getSite() = selectedSiteRepository.getSelectedSite()!!

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
        data object Close : UiEvent()
    }

    sealed class ButtonType {
        data object Done : ButtonType()
        data object Retry : ButtonType()
    }

    companion object {
        private const val TAG = "JetpackConnectionViewModel"
        private const val LIMIT_VERSION = "14.2"
        private const val STEP_TIMEOUT_MS = 30000L // 30 seconds timeout per step

        /**
         * Requirements:
         * - Self-hosted site, and
         * - The site is authenticated with application password, and
         * - the site isn't already connected to Jetpack, and
         * - Jetpack is not installed or the installed jetpack version is 14.2 or above
         */
        fun canInitiateJetpackConnection(site: SiteModel): Boolean {
            if (site.isSelfHostedAdmin && site.isApplicationPasswordsSupported) {
                return if (site.applicationPasswordsAuthorizeUrl.isNullOrEmpty()) {
                    false
                } else if (site.wpApiRestUrl.isNullOrEmpty()) {
                    false
                } else if (site.isJetpackConnected) {
                    false
                } else if (site.isJetpackInstalled) {
                    checkMinimalVersion(site.jetpackVersion, LIMIT_VERSION)
                } else {
                    true
                }
            }
            return false
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
