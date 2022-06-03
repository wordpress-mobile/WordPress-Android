package org.wordpress.android.ui.qrcodeauth

import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.DonePrimaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.DoneSecondaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.AuthenticatingPrimaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.AuthenticatingSecondaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.ErrorPrimaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.ErrorSecondaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.ValidatedPrimaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.ValidatedSecondaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Content
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Error
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Loading
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Scanning
import javax.inject.Inject

class QRCodeAuthUiStateMapper @Inject constructor() {
    fun mapLoading() = Loading
    fun mapScanning() = Scanning
    fun mapAuthFailed(onScanAgainClicked: () -> Unit, onCancelClicked: () -> Unit) =
        Error.AuthFailed(
                primaryAction = ErrorPrimaryAction(onScanAgainClicked),
                secondaryAction = ErrorSecondaryAction(onCancelClicked)
        )

    fun mapExpired(onScanAgainClicked: () -> Unit, onCancelClicked: () -> Unit) =
            Error.Expired(
                    primaryAction = ErrorPrimaryAction(onScanAgainClicked),
                    secondaryAction = ErrorSecondaryAction(onCancelClicked)
            )

    fun mapInvalidData(onScanAgainClicked: () -> Unit, onCancelClicked: () -> Unit) =
            Error.InvalidData(
                    primaryAction = ErrorPrimaryAction(onScanAgainClicked),
                    secondaryAction = ErrorSecondaryAction(onCancelClicked)
            )

    fun mapNoInternet(onScanAgainClicked: () -> Unit, onCancelClicked: () -> Unit) =
            Error.NoInternet(
                    primaryAction = ErrorPrimaryAction(onScanAgainClicked),
                    secondaryAction = ErrorSecondaryAction(onCancelClicked)
            )

    fun mapValidated(location: String?, browser: String?, onAuthenticateClick: () -> Unit, onCancelClick: () -> Unit) =
        Content.Validated(
                primaryAction = ValidatedPrimaryAction(onAuthenticateClick),
                secondaryAction = ValidatedSecondaryAction(onCancelClick),
                location = location,
                browser = browser
        )

    fun mapAuthenticating(fromValidated: Content.Validated) =
        Content.Authenticating(
                primaryAction = AuthenticatingPrimaryAction,
                secondaryAction = AuthenticatingSecondaryAction,
                location = fromValidated.location,
                browser = fromValidated.browser
        )

    fun mapAuthenticating(location: String?, browser: String?) =
            Content.Authenticating(
                    primaryAction = AuthenticatingPrimaryAction,
                    secondaryAction = AuthenticatingSecondaryAction,
                    location = location,
                    browser = browser
            )

    fun mapDone(onDismissClicked: () -> Unit) =
            Content.Done(
                    primaryAction = DonePrimaryAction(onDismissClicked),
                    secondaryAction = DoneSecondaryAction
            )
}
