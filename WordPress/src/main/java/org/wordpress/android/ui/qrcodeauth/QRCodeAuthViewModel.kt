package org.wordpress.android.ui.qrcodeauth

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
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
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.AUTH_FAILED
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.DONE
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.EXPIRED
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.INVALID_DATA
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.LOADING
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.NO_INTERNET
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.SCANNING
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.VALIDATED
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class QRCodeAuthViewModel @Inject constructor(
    private val uiStateMapper: QRCodeAuthUiStateMapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val validator: QRCodeAuthValidator
) : ViewModel() {
    private val _actionEvents = Channel<QRCodeAuthActionEvent>(Channel.BUFFERED)
    val actionEvents = _actionEvents.receiveAsFlow()

    private val _uiState = MutableStateFlow<QRCodeAuthUiState>(Loading)
    val uiState: StateFlow<QRCodeAuthUiState> = _uiState

    private var data: String? = null
    private var token: String? = null
    private var location: String? = null
    private var browser: String? = null
    private var lastState: QRCodeAuthUiStateType? = null
    private var isStarted = false

    fun start(savedInstanceState: Bundle?) {
        if (isStarted) return
        isStarted = true

        extractSavedInstanceStateIfNeeded(savedInstanceState)
        startOrRestoreUiState()
    }

    private fun extractSavedInstanceStateIfNeeded(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            data = savedInstanceState.getString(DATA_KEY, null)
            token = savedInstanceState.getString(TOKEN_KEY, null)
            browser = savedInstanceState.getString(BROWSER_KEY, null)
            location = savedInstanceState.getString(LOCATION_KEY, null)
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
                            this::onCancelClicked
                    )
            )
            AUTHENTICATING -> postUiState(uiStateMapper.mapToAuthenticating(location = location, browser = browser))
            DONE -> postUiState(uiStateMapper.mapToDone(this::dismissClicked))
            // errors
            INVALID_DATA -> postUiState(uiStateMapper.mapToInvalidData(this::scanAgainClicked, this::onCancelClicked))
            AUTH_FAILED -> postUiState(uiStateMapper.mapToAuthFailed(this::scanAgainClicked, this::onCancelClicked))
            EXPIRED -> postUiState(uiStateMapper.mapToExpired(this::scanAgainClicked, this::onCancelClicked))
            NO_INTERNET -> {
                postUiState(uiStateMapper.mapToNoInternet(this::scanAgainClicked, this::onCancelClicked))
            }
            else -> updateUiStateAndLaunchScanner()
        }
    }

    //  https://apps.wordpress.com/get/?campaign=login-qr-code#qr-code-login?token=asdfadsfa&data=asdfasdf
    fun onScanSuccess(scannedValue: String?) {
        handleScan(scannedValue)
    }

    fun onScanFailure() {
        // Note: This is a result of the tap on "X" within the scanner view
        postActionEvent(FinishActivity)
    }

    fun onBackPressed() {
        postActionEvent(LaunchDismissDialog(ShowDismissDialog))
    }

    private fun onCancelClicked() {
        postActionEvent(FinishActivity)
    }

    private fun scanAgainClicked() {
        postActionEvent(LaunchScanner)
    }

    private fun dismissClicked() {
        postActionEvent(FinishActivity)
    }

    private fun onAuthenticateClicked() {
        postUiState(uiStateMapper.mapToAuthenticating(_uiState.value as Validated))
        if (data.isNullOrEmpty() || token.isNullOrEmpty()) {
            postUiState(uiStateMapper.mapToInvalidData(this::scanAgainClicked, this::onCancelClicked))
        } else {
            authenticate(data = data.toString(), token = token.toString())
        }
    }

    private fun handleScan(scannedValue: String?) {
        extractQueryParamsIfValid(scannedValue)

        if (data.isNullOrEmpty() || token.isNullOrEmpty()) {
            postUiState(uiStateMapper.mapToInvalidData(this::scanAgainClicked, this::onCancelClicked))
        } else {
            postUiState(uiStateMapper.mapToLoading())
            validateScan(data = data.toString(), token = token.toString())
        }
    }

    private fun validateScan(data: String, token: String) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            postUiState(uiStateMapper.mapToNoInternet(this::scanAgainClicked, this::onCancelClicked))
            return
        }

        // todo: add authStore.validate and remove below
        postUiState(
                uiStateMapper.mapToValidated(
                        "location",
                        "browser",
                        this::onAuthenticateClicked,
                        this::onCancelClicked
                )
        )
    }

    private fun extractQueryParamsIfValid(scannedValue: String?) {
        if (!validator.isValidUri(scannedValue)) return

        val queryParams = validator.extractQueryParams(scannedValue)
        if (queryParams.containsKey(DATA_KEY) && queryParams.containsKey(TOKEN_KEY)) {
            this.data = queryParams[DATA_KEY].toString()
            this.token = queryParams[TOKEN_KEY].toString()
        }
    }

    @Suppress("MagicNumber")
    private fun authenticate(data: String, token: String) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            postUiState(uiStateMapper.mapToNoInternet(this::scanAgainClicked, this::onCancelClicked))
            return
        }

        clearProperties()
        // todo: authStore.authenticate call and remove below
        viewModelScope.launch {
            delay(2000L)
            postUiState(uiStateMapper.mapToDone(::dismissClicked))
        }
    }

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
            is Negative -> { } // NO OP
            is Dismissed -> { } // NO OP
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
        outState.putString(DATA_KEY, data)
        outState.putString(TOKEN_KEY, data)
        outState.putString(BROWSER_KEY, browser)
        outState.putString(LOCATION_KEY, location)
        outState.putString(LAST_STATE_KEY, uiState.value.type?.label)
    }

    companion object {
        const val TAG_DISMISS_DIALOG = "TAG_DISMISS_DIALOG"
        const val TOKEN_KEY = "token"
        const val DATA_KEY = "data"
        const val BROWSER_KEY = "browser"
        const val LOCATION_KEY = "location"
        const val LAST_STATE_KEY = "last_state"
    }
}
