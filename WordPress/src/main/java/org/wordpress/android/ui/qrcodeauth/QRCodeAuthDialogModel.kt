package org.wordpress.android.ui.qrcodeauth

import androidx.annotation.StringRes
import org.wordpress.android.R

sealed class QRCodeAuthDialogModel(
    val tag: String,
    @StringRes val title: Int,
    @StringRes open val message: Int,
    @StringRes val positiveButtonLabel: Int,
    @StringRes val negativeButtonLabel: Int? = null,
    @StringRes val cancelButtonLabel: Int? = null
) {
    object ShowDismissDialog : QRCodeAuthDialogModel(
        QRCodeAuthViewModel.TAG_DISMISS_DIALOG,
        R.string.qrcode_auth_flow_dismiss_dialog_title,
        R.string.qrcode_auth_flow_dismiss_dialog_message,
        R.string.ok,
        R.string.cancel
    )
}
