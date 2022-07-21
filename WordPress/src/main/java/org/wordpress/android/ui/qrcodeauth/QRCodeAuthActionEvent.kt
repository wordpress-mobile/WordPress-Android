package org.wordpress.android.ui.qrcodeauth

sealed class QRCodeAuthActionEvent {
    class LaunchDismissDialog(val dialogModel: QRCodeAuthDialogModel) : QRCodeAuthActionEvent()
    object LaunchScanner : QRCodeAuthActionEvent()
    object FinishActivity : QRCodeAuthActionEvent()
}
