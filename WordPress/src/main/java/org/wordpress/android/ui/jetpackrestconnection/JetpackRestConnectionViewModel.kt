package org.wordpress.android.ui.jetpackrestconnection

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.wordpress.android.BuildConfig
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.JETPACK_REST_CONNECT_SOURCE_KEY
import org.wordpress.android.analytics.AnalyticsTracker.JETPACK_REST_CONNECT_STATE_COMPLETED
import org.wordpress.android.analytics.AnalyticsTracker.JETPACK_REST_CONNECT_STATE_FAILED
import org.wordpress.android.analytics.AnalyticsTracker.JETPACK_REST_CONNECT_STATE_KEY
import org.wordpress.android.analytics.AnalyticsTracker.JETPACK_REST_CONNECT_STATE_STARTED
import org.wordpress.android.analytics.AnalyticsTracker.JETPACK_REST_CONNECT_STATE_STEP_KEY
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.WpAppNotifierHandler
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.VersionUtils.checkMinimalVersion
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import uniffi.wp_api.PluginStatus
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class JetpackRestConnectionViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val jetpackInstaller: JetpackInstaller,
    private val jetpackConnector: JetpackConnector,
    private val jetpackModuleHelper: JetpackStatsModuleHelper,
    private val appLogWrapper: AppLogWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val wpAppNotifierHandler: WpAppNotifierHandler,
) : ScopedViewModel(mainDispatcher), WpAppNotifierHandler.NotifierListener {
    // Internal variables that can be overridden for testing
    internal var uiDelayMs: Long = UI_DELAY_MS
    internal var stepTimeoutMs: Long = STEP_TIMEOUT_MS

    private val _currentStep = MutableStateFlow<ConnectionStep?>(null)
    val currentStep = _currentStep

    private val _uiEvent = MutableStateFlow<UiEvent?>(null)
    val uiEvent = _uiEvent

    private val _buttonType = MutableStateFlow<ButtonType?>(ButtonType.Start)
    val buttonType = _buttonType

    private val _stepStates = MutableStateFlow(initialStepStates)
    val stepStates = _stepStates

    private var isWaitingForWPComLogin = false

    private var connectionSource: ConnectionSource = DEFAULT_CONNECTION_SOURCE
    private var site: SiteModel = selectedSiteRepository.getSelectedSite() ?: error("No site selected")

    /**
     * This will be used for analytics tracking
     */
    fun setConnectionSource(source: ConnectionSource) {
        connectionSource = source
        appLogWrapper.d(AppLog.T.API, "$TAG: Connection source set to: $source")
    }

    private fun startConnectionFlow(fromStep: ConnectionStep? = null) {
        val stepInfo = fromStep?.let { " from step: $it" } ?: ""
        appLogWrapper.d(AppLog.T.API, "$TAG: Starting Jetpack connection flow $stepInfo")

        _buttonType.value = null
        _uiEvent.value = null

        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.JETPACK_REST_CONNECT_STARTED)
        wpAppNotifierHandler.addListener(this)

        startStep(fromStep ?: ConnectionStep.LoginWpCom)
    }

    private fun onFlowCompleted(wasSuccessful: Boolean) {
        if (wasSuccessful) {
            appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack connection flow completed successfully")
            _buttonType.value = ButtonType.Done
            analyticsTrackerWrapper.track(AnalyticsTracker.Stat.JETPACK_REST_CONNECT_COMPLETED)
        } else {
            appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack connection flow failed")
            _buttonType.value = ButtonType.Retry
        }

        wpAppNotifierHandler.removeListener(this)
        _currentStep.value = null
    }

    private fun getNextStep(): ConnectionStep? = when (currentStep.value) {
        null -> ConnectionStep.LoginWpCom
        ConnectionStep.LoginWpCom -> ConnectionStep.InstallJetpack
        ConnectionStep.InstallJetpack -> ConnectionStep.ConnectSite
        ConnectionStep.ConnectSite -> ConnectionStep.ConnectUser
        ConnectionStep.ConnectUser -> ConnectionStep.Finalize
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
        trackStepWithState(step, JETPACK_REST_CONNECT_STATE_STARTED)
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
                trackStepWithState(step, JETPACK_REST_CONNECT_STATE_FAILED)
                onFlowCompleted(false)
            }

            ConnectionStatus.Completed -> {
                trackStepWithState(step, JETPACK_REST_CONNECT_STATE_COMPLETED)
                if (step == ConnectionStep.Finalize) {
                    onFlowCompleted(true)
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
        startConnectionFlow()
    }

    /**
     * Connection flow completed successfully and user clicked the Done button
     */
    fun onDoneClick() {
        appLogWrapper.d(AppLog.T.API, "$TAG: Done clicked")
        setUiEvent(UiEvent.Done)
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
            analyticsTrackerWrapper.track(
                stat = AnalyticsTracker.Stat.JETPACK_REST_CONNECT_STEP_RETRIED,
                properties = mapOf(
                    JETPACK_REST_CONNECT_STATE_STEP_KEY to step.toString()
                )
            )
            startConnectionFlow(fromStep = step)
        } ?: run {
            // Fallback to original behavior if no failed step found
            clearValues()
            startConnectionFlow()
        }
    }

    private fun clearValues() {
        _uiEvent.value = null
        _stepStates.value = initialStepStates
        _buttonType.value = null
        _currentStep.value = null
    }

    /**
     * Returns true if the connection flow is active
     */
    private fun isActive(): Boolean = run {
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
                withTimeout(stepTimeoutMs) {
                    executeStep(step)
                }
            }
        } catch (e: Exception) {
            appLogWrapper.e(AppLog.T.API, "$TAG: Error in step $step: ${e.message}")
            val errorType = when (e) {
                is TimeoutCancellationException -> ErrorType.Timeout
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
                connectSite()
            }

            ConnectionStep.ConnectUser -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Connecting WordPress.com user")
                connectUser()
            }

            ConnectionStep.Finalize -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Finalizing connection")
                finalize()
            }
        }
    }

    /**
     * Step 1: Starts the wp.com login flow if the user isn't logged into wp.com
     */
    private fun loginWpCom() {
        if (accountStore.hasAccessToken() && site.isUsingWpComRestApi) {
            // User is already logged in with an app password, add a short delay before marking the step completed
            appLogWrapper.d(AppLog.T.API, "$TAG: WordPress.com access token already exists")
            launch {
                delay(uiDelayMs)
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
     * Step 2: Installs Jetpack to the current site if not already installed
     */
    private suspend fun installJetpack() {
        val result = jetpackInstaller.installJetpack(site)

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
                    error = ErrorType.InstallJetpackFailed(it.message)
                )
            }
        )
    }

    /**
     * Step 3: Connects the current site to Jetpack
     */
    private suspend fun connectSite() {
        val result = jetpackConnector.connectSite(site)
        result.fold(
            onSuccess = { wpComSiteId ->
                // the local site won't have a siteId since it's self-hosted and previously unconnected to Jetpack,
                // so assign it the siteId retrieved when connecting the site
                site.siteId = wpComSiteId.toLong()
                updateStepStatus(
                    step = ConnectionStep.ConnectSite,
                    status = ConnectionStatus.Completed
                )
            },
            onFailure = {
                updateStepStatus(
                    step = ConnectionStep.ConnectSite,
                    status = ConnectionStatus.Failed,
                    error = ErrorType.ConnectSiteFailed(it.message)
                )
            }
        )
    }

    /**
     * Step 4: Connects the user to the current site to Jetpack
     */
    private suspend fun connectUser() {
        if (!accountStore.hasAccessToken()) {
            updateStepStatus(
                step = ConnectionStep.ConnectUser,
                status = ConnectionStatus.Failed,
                error = ErrorType.MissingAccessToken
            )
            return
        }
        val result = jetpackConnector.connectUser(
            site = site,
            accessToken = accountStore.accessToken!!
        )
        result.fold(
            onSuccess = { wpComUserId ->
                updateStepStatus(
                    step = ConnectionStep.ConnectUser,
                    status = ConnectionStatus.Completed
                )
            },
            onFailure = {
                updateStepStatus(
                    step = ConnectionStep.ConnectUser,
                    status = ConnectionStatus.Failed,
                    error = ErrorType.ConnectUserFailed(it.message)
                )
            }
        )
    }

    /**
     * Step 5: Finalize the connection by activating the stats module for the site
     */
    private suspend fun finalize() {
        val result = jetpackModuleHelper.activateStatsModule(site)
        result.fold(
            onSuccess = {
                updateStepStatus(
                    step = ConnectionStep.Finalize,
                    status = ConnectionStatus.Completed
                )
            },
            onFailure = {
                updateStepStatus(
                    step = ConnectionStep.Finalize,
                    status = ConnectionStatus.Failed,
                    error = ErrorType.ActivateStatsFailed(it.message)
                )
            }
        )
    }

    /**
     * Called when auth fails in the WpApiClient created in JetpackConnectionHelper.initWpApiClient, reset the
     * access token and restart the connection flow so the user sees the login page
     */
    override fun onRequestedWithInvalidAuthentication(siteUrl: String) {
        appLogWrapper.d(AppLog.T.API, "$TAG: Invalid authentication, restarting")
        wpAppNotifierHandler.removeListener(this)
        accountStore.resetAccessToken()
        clearValues()
        startConnectionFlow()
    }

    private fun trackStepWithState(step: ConnectionStep, stateValue: String) {
        val event = when (step) {
            ConnectionStep.LoginWpCom -> AnalyticsTracker.Stat.JETPACK_REST_CONNECT_LOGIN
            ConnectionStep.InstallJetpack -> AnalyticsTracker.Stat.JETPACK_REST_CONNECT_INSTALL
            ConnectionStep.ConnectSite -> AnalyticsTracker.Stat.JETPACK_REST_CONNECT_SITE_CONNECTION
            ConnectionStep.ConnectUser -> AnalyticsTracker.Stat.JETPACK_REST_CONNECT_USER_CONNECTION
            ConnectionStep.Finalize -> AnalyticsTracker.Stat.JETPACK_REST_CONNECT_FINALIZE
        }
        analyticsTrackerWrapper.track(
            stat = event,
            properties = mapOf(
                JETPACK_REST_CONNECT_STATE_KEY to stateValue,
                JETPACK_REST_CONNECT_SOURCE_KEY to connectionSource.name
            )
        )
    }

    sealed class ConnectionStep {
        data object LoginWpCom : ConnectionStep()
        data object InstallJetpack : ConnectionStep()
        data object ConnectSite : ConnectionStep()
        data object ConnectUser : ConnectionStep()
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
        data object Done : UiEvent()
        data object Close : UiEvent()
        data object ShowCancelConfirmation : UiEvent()
    }

    sealed class ErrorType(open val message: String? = null) {
        data object LoginWpComFailed : ErrorType()
        data class InstallJetpackFailed(override val message: String? = null) : ErrorType(message)
        data object InstallJetpackInactive : ErrorType()
        data class ConnectSiteFailed(override val message: String? = null) : ErrorType(message)
        data class ConnectUserFailed(override val message: String? = null) : ErrorType(message)
        data class ActivateStatsFailed(override val message: String? = null) : ErrorType(message)
        data object MissingAccessToken : ErrorType()
        data object Timeout : ErrorType()
        data object Offline : ErrorType()
        data class Unknown(override val message: String? = null) : ErrorType(message)
    }

    sealed class ButtonType {
        data object Start : ButtonType()
        data object Done : ButtonType()
        data object Retry : ButtonType()
    }

    data class StepState(
        val status: ConnectionStatus = ConnectionStatus.NotStarted,
        val errorType: ErrorType? = null,
    )

    enum class ConnectionSource {
        STATS,
        NOTIFS
    }

    companion object {
        private const val TAG = "JetpackRestConnectionViewModel"
        private const val STEP_TIMEOUT_MS = 45 * 1000L
        private const val UI_DELAY_MS = 1000L
        private const val JETPACK_LIMIT_VERSION = "14.2"

        val DEFAULT_CONNECTION_SOURCE = ConnectionSource.STATS

        private val initialStepStates = mapOf(
            ConnectionStep.LoginWpCom to StepState(),
            ConnectionStep.InstallJetpack to StepState(),
            ConnectionStep.ConnectSite to StepState(),
            ConnectionStep.ConnectUser to StepState(),
            ConnectionStep.Finalize to StepState()
        )

        /**
         * Returns true if the Jetpack REST Connection flow is available and this site is able to use it.
         * Requirements:
         * - Jetpack app
         * - Self-hosted site using REST API
         * - Application password has been set
         * - Site isn't already connected to Jetpack
         * - Jetpack is not installed or the installed jetpack version is 14.2 or above
         */
        fun canInitiateJetpackRestConnection(site: SiteModel): Boolean {
            return BuildConfig.IS_JETPACK_APP
                    && site.isUsingSelfHostedRestApi
                    && !site.wpApiRestUrl.isNullOrEmpty()
                    && !site.isJetpackConnected
                    && (!site.isJetpackInstalled || checkMinimalVersion(site.jetpackVersion, JETPACK_LIMIT_VERSION))
        }
    }
}
