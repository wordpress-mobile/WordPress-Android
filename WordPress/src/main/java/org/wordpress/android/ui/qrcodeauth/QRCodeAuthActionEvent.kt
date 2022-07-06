package org.wordpress.android.ui.qrcodeauth

sealed class QRCodeAuthActionEvent {
    class LaunchDismissDialog(val dialogModel: QRCodeAuthDialogModel) : QRCodeAuthActionEvent()
    object LaunchScanner : QRCodeAuthActionEvent()
    object FinishActivity : QRCodeAuthActionEvent()
    //TODO @RenanLukas action created as initial value to test viewModel.actionEvents.collectAsState
    object Idle : QRCodeAuthActionEvent()
}
