package org.wordpress.android.ui.qrcodeauth

sealed class QRCodeAuthActionEvent {
    data object LaunchScanner : QRCodeAuthActionEvent()
    data object FinishActivity : QRCodeAuthActionEvent()
}
