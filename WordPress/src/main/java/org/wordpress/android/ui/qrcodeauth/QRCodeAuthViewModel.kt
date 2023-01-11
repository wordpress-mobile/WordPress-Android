package org.wordpress.android.ui.qrcodeauth

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.QRLOGIN_VERIFY_FAILED
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthError
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthErrorType.DATA_INVALID
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthErrorType.NOT_AUTHORIZED
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthErrorType.REST_INVALID_PARAM
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthErrorType.TIMEOUT
import org.wordpress.android.fluxc.store.qrcodeauth.QRCodeAuthStore
import org.wordpress.android.fluxc.store.qrcodeauth.QRCodeAuthStore.QRCodeAuthResult
import org.wordpress.android.fluxc.store.qrcodeauth.QRCodeAuthStore.QRCodeAuthValidateResult
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Dismissed
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Negative
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Positive
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthActionEvent.FinishActivity
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthActionEvent.LaunchDismissDialog
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthActionEvent.LaunchScanner
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthDialogModel.ShowDismissDialog
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Content.Validated
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Loading
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.AUTHENTICATING
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.AUTHENTICATION_FAILED
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.DONE
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.EXPIRED_TOKEN
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.INVALID_DATA
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.LOADING
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.NO_INTERNET
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.SCANNING
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.VALIDATED
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

@HiltViewModel
class QRCodeAuthViewModel @Inject constructor(
    private val authStore: QRCodeAuthStore,
    private val uiStateMapper: QRCodeAuthUiStateMapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val validator: QRCodeAuthValidator,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ViewModel() {
    private val _actionEvents = Channel<QRCodeAuthActionEvent>(Channel.BUFFERED)
    val actionEvents = _actionEvents.receiveAsFlow()

    private val _uiState = MutableStateFlow<QRCodeAuthUiState>(Loading)
    val uiState: StateFlow<QRCodeAuthUiState> = _uiState

    private var trackingOrigin: String? = null
    private var data: String? = null
    private var token: String? = null
    private var location: String? = null
    private var browser: String? = null
    private var lastState: QRCodeAuthUiStateType? = null
    private var isStarted = false

    fun start(uri: String? = null, isDeepLink: Boolean = false, savedInstanceState: Bundle? = null) {
        if (isStarted) return
        isStarted = true

        extractSavedInstanceStateIfNeeded(savedInstanceState)

        if (isDeepLink && savedInstanceState == null) {
            trackingOrigin = ORIGIN_DEEPLINK
            process(uri)
        } else {
            if (trackingOrigin.isNullOrEmpty()) trackingOrigin = ORIGIN_MENU
            startOrRestoreUiState()
        }
    }

    private fun extractSavedInstanceStateIfNeeded(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            data = savedInstanceState.getString(DATA_KEY, null)
            token = savedInstanceState.getString(TOKEN_KEY, null)
            browser = savedInstanceState.getString(BROWSER_KEY, null)
            location = savedInstanceState.getString(LOCATION_KEY, null)
            trackingOrigin = savedInstanceState.getString(TRACKING_ORIGIN_KEY, ORIGIN_MENU)
            lastState = QRCodeAuthUiStateType.fromString(savedInstanceState.getString(LAST_STATE_KEY, null))
        }
    }

    private fun startOrRestoreUiState() {
        when (lastState) {
            LOADING, SCANNING -> updateUiStateAndLaunchScanner()
            VALIDATED -> postUiState(
                uiStateMapper.mapToValidated(
                    location,
                    browser,
                    this::onAuthenticateClicked,
                    this::onAuthenticateCancelClicked
                )
            )
            AUTHENTICATING -> postUiState(uiStateMapper.mapToAuthenticating(location = location, browser = browser))
            DONE -> postUiState(uiStateMapper.mapToDone(this::onDismissClicked))
            // errors
            INVALID_DATA -> postUiState(uiStateMapper.mapToInvalidData(this::onScanAgainClicked, this::onCancelClicked))
            AUTHENTICATION_FAILED -> postUiState(
                uiStateMapper.mapToAuthFailed(
                    this::onScanAgainClicked,
                    this::onCancelClicked
                )
            )
            EXPIRED_TOKEN -> postUiState(uiStateMapper.mapToExpired(this::onScanAgainClicked, this::onCancelClicked))
            NO_INTERNET -> {
                postUiState(uiStateMapper.mapToNoInternet(this::onScanAgainClicked, this::onCancelClicked))
            }
            else -> updateUiStateAndLaunchScanner()
        }
    }

    //  https://apps.wordpress.com/get/?campaign=login-qr-code#qr-code-login?token=asdfadsfa&data=asdfasdf
    fun onScanSuccess(scannedValue: String?) {
        track(Stat.QRLOGIN_SCANNER_SCANNED_CODE)
        process(scannedValue)
    }

    fun onScanFailure() {
        // Note: This is a result of the tap on "X" within the scanner view
        track(Stat.QRLOGIN_SCANNER_DISMISSED)
        postActionEvent(FinishActivity)
    }

    fun onBackPressed() {
        postActionEvent(LaunchDismissDialog(ShowDismissDialog))
    }

    private fun onCancelClicked() {
        track(Stat.QRLOGIN_VERIFY_CANCELLED)
        postActionEvent(FinishActivity)
    }

    private fun onScanAgainClicked() {
        track(Stat.QRLOGIN_VERIFY_SCAN_AGAIN)
        postActionEvent(LaunchScanner)
    }

    private fun onDismissClicked() {
        track(Stat.QRLOGIN_VERIFY_DISMISS)
        postActionEvent(FinishActivity)
    }

    private fun onAuthenticateClicked() {
        track(Stat.QRLOGIN_VERIFY_APPROVED)
        postUiState(uiStateMapper.mapToAuthenticating(_uiState.value as Validated))
        if (data.isNullOrEmpty() || token.isNullOrEmpty()) {
            track(QRLOGIN_VERIFY_FAILED, TRACK_INVALID_DATA)
            postUiState(uiStateMapper.mapToInvalidData(this::onScanAgainClicked, this::onCancelClicked))
        } else {
            authenticate(data = data.toString(), token = token.toString())
        }
    }

    private fun onAuthenticateCancelClicked() {
        track(Stat.QRLOGIN_VERIFY_CANCELLED)
        postActionEvent(FinishActivity)
    }

    private fun process(input: String?) {
        clearProperties()
        extractQueryParamsIfValid(input)

        track(Stat.QRLOGIN_VERIFY_DISPLAYED)

        if (data.isNullOrEmpty() || token.isNullOrEmpty()) {
            track(QRLOGIN_VERIFY_FAILED, TRACK_INVALID_DATA)
            postUiState(uiStateMapper.mapToInvalidData(this::onScanAgainClicked, this::onCancelClicked))
        } else {
            postUiState(uiStateMapper.mapToLoading())
            validateScan(data = data.toString(), token = token.toString())
        }
    }

    private fun validateScan(data: String, token: String) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            track(QRLOGIN_VERIFY_FAILED, TRACK_NO_INTERNET)
            postUiState(uiStateMapper.mapToNoInternet(this::onScanAgainClicked, this::onCancelClicked))
            return
        }

        viewModelScope.launch {
            val result = authStore.validate(data = data, token = token)
            if (result.isError) {
                val error = mapScanErrorToErrorState(result.error)
                track(QRLOGIN_VERIFY_FAILED, error.type.label)
                postUiState(error)
            } else {
                browser = result.model?.browser
                location = result.model?.location
                track(Stat.QRLOGIN_VERIFY_TOKEN_VALIDATED)
                postUiState(mapScanSuccessToValidatedState(result))
            }
        }
    }

    private fun mapScanSuccessToValidatedState(result: QRCodeAuthResult<QRCodeAuthValidateResult>) =
        uiStateMapper.mapToValidated(
            browser = result.model?.browser,
            location = result.model?.location,
            onAuthenticateClick = this::onAuthenticateClicked,
            onCancelClick = this::onAuthenticateCancelClicked
        )

    private fun mapScanErrorToErrorState(error: QRCodeAuthError) = when (error.type) {
        NOT_AUTHORIZED -> uiStateMapper.mapToAuthFailed(this::onScanAgainClicked, this::onCancelClicked)
        AUTHORIZATION_REQUIRED -> {
            if (error.message?.lowercase().equals(EXPIRED_MESSAGE)) {
                uiStateMapper.mapToExpired(this::onScanAgainClicked, this::onCancelClicked)
            } else {
                uiStateMapper.mapToAuthFailed(this::onScanAgainClicked, this::onCancelClicked)
            }
        }
        GENERIC_ERROR,
        INVALID_RESPONSE,
        REST_INVALID_PARAM,
        API_ERROR,
        DATA_INVALID,
        TIMEOUT -> uiStateMapper.mapToInvalidData(this::onScanAgainClicked, this::onCancelClicked)
    }

    private fun extractQueryParamsIfValid(scannedValue: String?) {
        if (!validator.isValidUri(scannedValue)) return

        val queryParams = validator.extractQueryParams(scannedValue)
        if (queryParams.containsKey(DATA_KEY) && queryParams.containsKey(TOKEN_KEY)) {
            this.data = queryParams[DATA_KEY].toString()
            this.token = queryParams[TOKEN_KEY].toString()
        }
    }

    private fun authenticate(data: String, token: String) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            track(QRLOGIN_VERIFY_FAILED, TRACK_NO_INTERNET)
            postUiState(uiStateMapper.mapToNoInternet(this::onScanAgainClicked, this::onCancelClicked))
            return
        }

        viewModelScope.launch {
            val result = authStore.authenticate(data = data, token = token)
            if (result.isError) {
                val error = mapScanErrorToErrorState(result.error)
                track(QRLOGIN_VERIFY_FAILED, error.type.label)
                postUiState(error)
            } else {
                clearProperties()
                if (result.model?.authenticated == true) {
                    track(Stat.QRLOGIN_AUTHENTICATED)
                    postUiState(mapAuthenticateSuccessToDoneState())
                } else {
                    track(QRLOGIN_VERIFY_FAILED, TRACK_AUTHENTICATION_FAILED)
                    postUiState(uiStateMapper.mapToAuthFailed(::onScanAgainClicked, ::onCancelClicked))
                }
            }
        }
    }

    private fun mapAuthenticateSuccessToDoneState() = uiStateMapper.mapToDone(this::onDismissClicked)

    private fun updateUiStateAndLaunchScanner() {
        postUiState(uiStateMapper.mapToScanning())
        postActionEvent(LaunchScanner)
    }

    private fun postUiState(state: QRCodeAuthUiState) {
        viewModelScope.launch {
            _uiState.value = state
        }
    }

    private fun postActionEvent(actionEvent: QRCodeAuthActionEvent) {
        viewModelScope.launch {
            _actionEvents.send(actionEvent)
        }
    }

    fun onDialogInteraction(interaction: DialogInteraction) {
        when (interaction) {
            is Positive -> postActionEvent(FinishActivity)
            is Negative -> Unit
            is Dismissed -> Unit
        }
    }

    private fun clearProperties() {
        data = null
        token = null
        browser = null
        location = null
        lastState = null
    }

    fun writeToBundle(outState: Bundle) {
        outState.putString(TRACKING_ORIGIN_KEY, trackingOrigin)
        outState.putString(DATA_KEY, data)
        outState.putString(TOKEN_KEY, token)
        outState.putString(BROWSER_KEY, browser)
        outState.putString(LOCATION_KEY, location)
        outState.putString(LAST_STATE_KEY, uiState.value.type?.label)
    }

    fun track(stat: Stat, error: String? = null) {
        val props = if (error.isNullOrEmpty()) {
            mapOf(ORIGIN to trackingOrigin)
        } else {
            mapOf(ORIGIN to trackingOrigin, ERROR to error)
        }
        analyticsTrackerWrapper.track(stat, props)
    }

    companion object {
        const val TAG_DISMISS_DIALOG = "TAG_DISMISS_DIALOG"
        const val TOKEN_KEY = "token"
        const val DATA_KEY = "data"
        const val BROWSER_KEY = "browser"
        const val LOCATION_KEY = "location"
        const val LAST_STATE_KEY = "last_state"
        const val TRACKING_ORIGIN_KEY = "tracking_origin"
        const val ORIGIN = "origin"
        const val ORIGIN_MENU = "menu"
        const val ORIGIN_DEEPLINK = "deep_link"
        const val EXPIRED_MESSAGE = "qr code data expired"
        const val ERROR = "error"
        const val TRACK_INVALID_DATA = "invalid_data"
        const val TRACK_NO_INTERNET = "no_internet"
        const val TRACK_AUTHENTICATION_FAILED = "authentication_failed"
    }
}
