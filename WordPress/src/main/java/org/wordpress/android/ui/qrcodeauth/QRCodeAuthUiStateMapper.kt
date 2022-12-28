package org.wordpress.android.ui.qrcodeauth

import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.AuthenticatingPrimaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.AuthenticatingSecondaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.DonePrimaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.DoneSecondaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.ErrorPrimaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.ErrorSecondaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.ValidatedPrimaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.ValidatedSecondaryActionButton
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
            primaryActionButton = ErrorPrimaryActionButton(onScanAgainClicked),
            secondaryActionButton = ErrorSecondaryActionButton(onCancelClicked)
        )

    fun mapToExpired(onScanAgainClicked: () -> Unit, onCancelClicked: () -> Unit) =
        Error.Expired(
            primaryActionButton = ErrorPrimaryActionButton(onScanAgainClicked),
            secondaryActionButton = ErrorSecondaryActionButton(onCancelClicked)
        )

    fun mapToInvalidData(onScanAgainClicked: () -> Unit, onCancelClicked: () -> Unit) =
        Error.InvalidData(
            primaryActionButton = ErrorPrimaryActionButton(onScanAgainClicked),
            secondaryActionButton = ErrorSecondaryActionButton(onCancelClicked)
        )

    fun mapToNoInternet(onScanAgainClicked: () -> Unit, onCancelClicked: () -> Unit) =
        Error.NoInternet(
            primaryActionButton = ErrorPrimaryActionButton(onScanAgainClicked),
            secondaryActionButton = ErrorSecondaryActionButton(onCancelClicked)
        )

    fun mapToValidated(
        location: String?,
        browser: String?,
        onAuthenticateClick: () -> Unit,
        onCancelClick: () -> Unit
    ) =
        Content.Validated(
            primaryActionButton = ValidatedPrimaryActionButton(onAuthenticateClick),
            secondaryActionButton = ValidatedSecondaryActionButton(onCancelClick),
            location = location,
            browser = browser
        )

    fun mapToAuthenticating(fromValidated: Content.Validated) =
        Content.Authenticating(
            primaryActionButton = AuthenticatingPrimaryActionButton,
            secondaryActionButton = AuthenticatingSecondaryActionButton,
            location = fromValidated.location,
            browser = fromValidated.browser
        )

    fun mapToAuthenticating(location: String?, browser: String?) =
        Content.Authenticating(
            primaryActionButton = AuthenticatingPrimaryActionButton,
            secondaryActionButton = AuthenticatingSecondaryActionButton,
            location = location,
            browser = browser
        )

    fun mapToDone(onDismissClicked: () -> Unit) =
        Content.Done(
            primaryActionButton = DonePrimaryActionButton(onDismissClicked),
            secondaryActionButton = DoneSecondaryActionButton
        )
}
