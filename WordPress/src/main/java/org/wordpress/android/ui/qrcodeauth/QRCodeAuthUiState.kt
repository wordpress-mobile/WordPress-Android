package org.wordpress.android.ui.qrcodeauth

import androidx.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.AuthenticatingPrimaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.AuthenticatingSecondaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.DonePrimaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.DoneSecondaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.ErrorPrimaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.ErrorSecondaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.ValidatedPrimaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.ValidatedSecondaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.AUTHENTICATING
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.AUTHENTICATION_FAILED
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.CONTENT
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.DONE
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.ERROR
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.EXPIRED_TOKEN
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.INVALID_DATA
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.LOADING
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.NO_INTERNET
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.SCANNING
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.VALIDATED
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText

const val BASE_ALPHA = 1.0f
const val BLURRED_ALPHA = 0.75f

sealed class QRCodeAuthUiState {
    open val type: QRCodeAuthUiStateType? = null

    object Scanning : QRCodeAuthUiState() {
        override val type = SCANNING
    }

    object Loading : QRCodeAuthUiState() {
        override val type = LOADING
    }

    sealed class Error : QRCodeAuthUiState() {
        override val type = ERROR
        abstract val title: UiString
        abstract val subtitle: UiString
        abstract val image: Int
        open val primaryActionButton: ErrorPrimaryActionButton? = null
        open val secondaryActionButton: ErrorSecondaryActionButton? = null

        data class AuthFailed(
            override val primaryActionButton: ErrorPrimaryActionButton,
            override val secondaryActionButton: ErrorSecondaryActionButton
        ) : Error() {
            override val type = AUTHENTICATION_FAILED
            override val title: UiString = UiStringRes(R.string.qrcode_auth_flow_error_auth_failed_title)
            override val subtitle: UiString = UiStringRes(R.string.qrcode_auth_flow_error_auth_failed_subtitle)

            @DrawableRes
            override val image = R.drawable.img_illustration_empty_results_216dp
        }

        data class Expired(
            override val primaryActionButton: ErrorPrimaryActionButton,
            override val secondaryActionButton: ErrorSecondaryActionButton
        ) : Error() {
            override val type = EXPIRED_TOKEN
            override val title: UiString = UiStringRes(R.string.qrcode_auth_flow_error_expired_title)
            override val subtitle: UiString = UiStringRes(R.string.qrcode_auth_flow_error_expired_subtitle)

            @DrawableRes
            override val image = R.drawable.img_illustration_empty_results_216dp
        }

        data class InvalidData(
            override val primaryActionButton: ErrorPrimaryActionButton,
            override val secondaryActionButton: ErrorSecondaryActionButton
        ) : Error() {
            override val type = INVALID_DATA
            override val title: UiString = UiStringRes(R.string.qrcode_auth_flow_error_invalid_data_title)
            override val subtitle: UiString = UiStringRes(R.string.qrcode_auth_flow_error_invalid_data_subtitle)

            @DrawableRes
            override val image = R.drawable.img_illustration_empty_results_216dp
        }

        data class NoInternet(
            override val primaryActionButton: ErrorPrimaryActionButton,
            override val secondaryActionButton: ErrorSecondaryActionButton
        ) : Error() {
            override val type = NO_INTERNET
            override val title: UiString = UiStringRes(R.string.qrcode_auth_flow_error_no_connection_title)
            override val subtitle: UiString = UiStringRes(R.string.qrcode_auth_flow_error_no_connection_subtitle)

            @DrawableRes
            override val image = R.drawable.img_illustration_cloud_off_152dp
        }
    }

    sealed class Content : QRCodeAuthUiState() {
        override val type = CONTENT
        open val title: UiString? = null
        open val subtitle: UiString? = null

        @DrawableRes
        open val image: Int? = null
        open val isProgressShowing: Boolean = false
        open val alpha: Float = BASE_ALPHA
        open val primaryActionButton: ActionButton? = null
        open val secondaryActionButton: ActionButton? = null
        open val browser: String? = null
        open val location: String? = null

        data class Validated(
            override val browser: String? = null,
            override val location: String? = null,
            override val primaryActionButton: ValidatedPrimaryActionButton,
            override val secondaryActionButton: ValidatedSecondaryActionButton
        ) : Content() {
            override val type = VALIDATED
            override val title: UiString = if (browser == null) {
                UiStringResWithParams(
                    R.string.qrcode_auth_flow_validated_default_title,
                    listOf(UiStringText(location ?: " "))
                )
            } else {
                UiStringResWithParams(
                    R.string.qrcode_auth_flow_validated_title,
                    listOf(UiStringText(browser), UiStringText(location ?: " "))
                )
            }
            override val subtitle: UiString = UiStringRes(R.string.qrcode_auth_flow_validated_subtitle)

            @DrawableRes
            override val image = R.drawable.img_illustration_qrcode_auth_validated_152dp
        }

        data class Authenticating(
            override val browser: String? = null,
            override val location: String? = null,
            override val primaryActionButton: AuthenticatingPrimaryActionButton,
            override val secondaryActionButton: AuthenticatingSecondaryActionButton
        ) : Content() {
            override val type = AUTHENTICATING
            override val title: UiString = if (browser == null) {
                UiStringResWithParams(
                    R.string.qrcode_auth_flow_validated_default_title,
                    listOf(UiStringText(location ?: " "))
                )
            } else {
                UiStringResWithParams(
                    R.string.qrcode_auth_flow_validated_title,
                    listOf(UiStringText(browser), UiStringText(location ?: " "))
                )
            }
            override val subtitle: UiString = UiStringRes(R.string.qrcode_auth_flow_validated_subtitle)

            @DrawableRes
            override val image = R.drawable.img_illustration_qrcode_auth_validated_152dp
            override val alpha: Float = BLURRED_ALPHA
            override val isProgressShowing: Boolean = true
        }

        data class Done(
            override val primaryActionButton: DonePrimaryActionButton,
            override val secondaryActionButton: DoneSecondaryActionButton
        ) : Content() {
            override val type = DONE
            override val title: UiString = UiStringRes(R.string.qrcode_auth_flow_done_title)
            override val subtitle: UiString = UiStringRes(R.string.qrcode_auth_flow_done_subtitle)

            @DrawableRes
            override val image = R.drawable.img_illustration_qrcode_auth_login_success_218dp
        }
    }

    sealed class ActionButton {
        open val label: UiString? = null
        open val isEnabled: Boolean = true
        open val isVisible: Boolean = true
        open val clickAction: (() -> Unit)? = null

        data class ValidatedPrimaryActionButton(override val clickAction: (() -> Unit)) : ActionButton() {
            override val label: UiString = UiStringRes(R.string.qrcode_auth_flow_validated_primary_action)
        }

        data class ValidatedSecondaryActionButton(override val clickAction: (() -> Unit)) : ActionButton() {
            override val label: UiString = UiStringRes(R.string.cancel)
        }

        object AuthenticatingPrimaryActionButton : ActionButton() {
            override val label: UiString = UiStringRes(R.string.qrcode_auth_flow_validated_primary_action)
            override val isEnabled = false
        }

        object AuthenticatingSecondaryActionButton : ActionButton() {
            override val label: UiString = UiStringRes(R.string.cancel)
            override val isEnabled = false
        }

        data class DonePrimaryActionButton(override val clickAction: (() -> Unit)) : ActionButton() {
            override val label: UiString = UiStringRes(R.string.qrcode_auth_flow_dismiss)
        }

        object DoneSecondaryActionButton : ActionButton() {
            override val isVisible = false
        }

        data class ErrorPrimaryActionButton(override val clickAction: (() -> Unit)) : ActionButton() {
            override val label: UiString = UiStringRes(R.string.qrcode_auth_flow_scan_again)
        }

        data class ErrorSecondaryActionButton(override val clickAction: (() -> Unit)) : ActionButton() {
            override val label: UiString = UiStringRes(R.string.cancel)
        }
    }
}
