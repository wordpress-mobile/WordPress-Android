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
class QRCodeAuthViewModel @Inject constructor() : ViewModel() {
    private val _actionEvents = Channel<QRCodeAuthActionEvent>(Channel.BUFFERED)
    val actionEvents = _actionEvents.receiveAsFlow()

    private var isStarted = false

    fun start() {
        if (isStarted) return
        isStarted = true
    }

    fun onBackPressed() {
        postActionEvent(LaunchDismissDialog(ShowDismissDialog))
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
    }
}
