package org.wordpress.android.ui.qrcodeauth

import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.AuthenticatingPrimaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.AuthenticatingSecondaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.DonePrimaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.DoneSecondaryAction
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
    fun mapToLoading() = Loading
    fun mapToScanning() = Scanning
    fun mapToAuthFailed(onScanAgainClicked: () -> Unit, onCancelClicked: () -> Unit) =
        Error.AuthFailed(
                primaryAction = ErrorPrimaryAction(onScanAgainClicked),
                secondaryAction = ErrorSecondaryAction(onCancelClicked)
        )

    fun mapToExpired(onScanAgainClicked: () -> Unit, onCancelClicked: () -> Unit) =
            Error.Expired(
                    primaryAction = ErrorPrimaryAction(onScanAgainClicked),
                    secondaryAction = ErrorSecondaryAction(onCancelClicked)
            )

    fun mapToInvalidData(onScanAgainClicked: () -> Unit, onCancelClicked: () -> Unit) =
            Error.InvalidData(
                    primaryAction = ErrorPrimaryAction(onScanAgainClicked),
                    secondaryAction = ErrorSecondaryAction(onCancelClicked)
            )

    fun mapToNoInternet(onScanAgainClicked: () -> Unit, onCancelClicked: () -> Unit) =
            Error.NoInternet(
                    primaryAction = ErrorPrimaryAction(onScanAgainClicked),
                    secondaryAction = ErrorSecondaryAction(onCancelClicked)
            )

    fun mapToValidated(
        location: String?,
        browser: String?,
        onAuthenticateClick: () -> Unit,
        onCancelClick: () -> Unit
    ) =
            Content.Validated(
                    primaryAction = ValidatedPrimaryAction(onAuthenticateClick),
                    secondaryAction = ValidatedSecondaryAction(onCancelClick),
                    location = location,
                    browser = browser
            )

    fun mapToAuthenticating(fromValidated: Content.Validated) =
        Content.Authenticating(
                primaryAction = AuthenticatingPrimaryAction,
                secondaryAction = AuthenticatingSecondaryAction,
                location = fromValidated.location,
                browser = fromValidated.browser
        )

    fun mapToAuthenticating(location: String?, browser: String?) =
            Content.Authenticating(
                    primaryAction = AuthenticatingPrimaryAction,
                    secondaryAction = AuthenticatingSecondaryAction,
                    location = location,
                    browser = browser
            )

    fun mapToDone(onDismissClicked: () -> Unit) =
            Content.Done(
                    primaryAction = DonePrimaryAction(onDismissClicked),
                    secondaryAction = DoneSecondaryAction
            )
}
