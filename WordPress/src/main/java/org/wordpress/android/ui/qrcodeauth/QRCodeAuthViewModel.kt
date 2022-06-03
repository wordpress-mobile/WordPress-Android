package org.wordpress.android.ui.qrcodeauth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Dismissed
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Negative
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Positive
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthActionEvent.FinishActivity
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthActionEvent.LaunchDismissDialog
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthDialogModel.ShowDismissDialog
import javax.inject.Inject

@HiltViewModel
class QRCodeAuthViewModel @Inject constructor(
    private val validator: QRCodeAuthValidator
) : ViewModel() {
    private val _actionEvents = Channel<QRCodeAuthActionEvent>(Channel.BUFFERED)
    val actionEvents = _actionEvents.receiveAsFlow()

    private var data: String? = null
    private var token: String? = null
    private var isStarted = false

    fun start() {
        if (isStarted) return
        isStarted = true
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

    private fun handleScan(scannedValue: String?) {
        extractQueryParamsIfValid(scannedValue)
    }

    private fun extractQueryParamsIfValid(scannedValue: String?) {
        if (!validator.isValidUri(scannedValue)) return

        val queryParams = validator.extractQueryParams(scannedValue)
        if (queryParams.containsKey(DATA_KEY) && queryParams.containsKey(TOKEN_KEY)) {
            this.data = queryParams[DATA_KEY].toString()
            this.token = queryParams[TOKEN_KEY].toString()
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
