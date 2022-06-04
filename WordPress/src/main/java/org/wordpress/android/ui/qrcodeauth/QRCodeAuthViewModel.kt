package org.wordpress.android.ui.qrcodeauth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
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
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Loading
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

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
    private var isStarted = false

    fun start() {
        if (isStarted) return
        isStarted = true

        updateUiStateAndLaunchScanner()
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

    private fun cancelClicked() {
        postActionEvent(FinishActivity)
    }

    private fun authenticateClicked() {
        // todo: implement
    }

    private fun handleScan(scannedValue: String?) {
        extractQueryParamsIfValid(scannedValue)

        if (data.isNullOrEmpty() || token.isNullOrEmpty()) {
           // todo: handle error
        } else {
            postUiState(uiStateMapper.mapLoading())
            validateScan(data = data.toString(), token = token.toString())
        }
    }

    private fun validateScan(data: String, token: String) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
           // todo: handle error
            return
        }

        // todo: add authStore.validate and remove below
        postUiState(uiStateMapper.mapValidated(
                "location",
                "browser",
                this::authenticateClicked,
                this::cancelClicked))
    }

    private fun extractQueryParamsIfValid(scannedValue: String?) {
        if (!validator.isValidUri(scannedValue)) return

        val queryParams = validator.extractQueryParams(scannedValue)
        if (queryParams.containsKey(DATA_KEY) && queryParams.containsKey(TOKEN_KEY)) {
            this.data = queryParams[DATA_KEY].toString()
            this.token = queryParams[TOKEN_KEY].toString()
        }
    }

    private fun updateUiStateAndLaunchScanner() {
        postUiState(uiStateMapper.mapScanning())
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

    companion object {
        const val TAG_DISMISS_DIALOG = "TAG_DISMISS_DIALOG"
        const val TOKEN_KEY = "token"
        const val DATA_KEY = "data"
    }
}
