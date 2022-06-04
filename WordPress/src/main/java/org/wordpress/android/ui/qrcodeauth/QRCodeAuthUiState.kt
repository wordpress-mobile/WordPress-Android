package org.wordpress.android.ui.qrcodeauth

import androidx.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.AuthenticatingPrimaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.AuthenticatingSecondaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.DonePrimaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.DoneSecondaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.ErrorPrimaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.ErrorSecondaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.ValidatedPrimaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Action.ValidatedSecondaryAction
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.AUTHENTICATING
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.AUTH_FAILED
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.CONTENT
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.DONE
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.ERROR
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiStateType.EXPIRED
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

@Suppress("LongParameterList")
sealed class QRCodeAuthUiState {
    open val type: QRCodeAuthUiStateType? = null
    open val scanningVisibility = false
    open val loadingVisibility = false
    open val errorVisibility = false
    open val contentVisibility = false

    object Scanning : QRCodeAuthUiState() {
        override val type = SCANNING
        override val scanningVisibility = true
    }

    object Loading : QRCodeAuthUiState() {
        override val type = LOADING
        override val loadingVisibility = true
    }

    sealed class Error : QRCodeAuthUiState() {
        override val type = ERROR
        override val errorVisibility = true
        abstract val title: UiString
        abstract val subtitle: UiString
        abstract val image: Int
        open val primaryAction: ErrorPrimaryAction? = null
        open val secondaryAction: ErrorSecondaryAction? = null

        data class AuthFailed(
            override val primaryAction: ErrorPrimaryAction,
            override val secondaryAction: ErrorSecondaryAction
        ) : Error() {
            override val type = AUTH_FAILED
            override val title: UiString = UiStringRes(R.string.qrcode_auth_flow_error_auth_failed_title)
            override val subtitle: UiString = UiStringRes(R.string.qrcode_auth_flow_error_auth_failed_subtitle)
            @DrawableRes override val image = R.drawable.img_illustration_empty_results_216dp
        }

        data class Expired(
            override val primaryAction: ErrorPrimaryAction,
            override val secondaryAction: ErrorSecondaryAction
        ) : Error() {
            override val type = EXPIRED
            override val title: UiString = UiStringRes(R.string.qrcode_auth_flow_error_expired_title)
            override val subtitle: UiString = UiStringRes(R.string.qrcode_auth_flow_error_expired_subtitle)
            @DrawableRes override val image = R.drawable.img_illustration_empty_results_216dp
        }

        data class InvalidData(
            override val primaryAction: ErrorPrimaryAction,
            override val secondaryAction: ErrorSecondaryAction
        ) : Error() {
            override val type = INVALID_DATA
            override val title: UiString = UiStringRes(R.string.qrcode_auth_flow_error_invalid_data_title)
            override val subtitle: UiString = UiStringRes(R.string.qrcode_auth_flow_error_invalid_data_subtitle)
            @DrawableRes override val image = R.drawable.img_illustration_empty_results_216dp
        }

        data class NoInternet(
            override val primaryAction: ErrorPrimaryAction,
            override val secondaryAction: ErrorSecondaryAction
        ) : Error() {
            override val type = NO_INTERNET
            override val title: UiString = UiStringRes(R.string.qrcode_auth_flow_error_no_connection_title)
            override val subtitle: UiString = UiStringRes(R.string.qrcode_auth_flow_error_no_connection_subtitle)
            @DrawableRes override val image = R.drawable.img_illustration_cloud_off_152dp
        }
    }

    sealed class Content : QRCodeAuthUiState() {
        override val type = CONTENT
        override val contentVisibility: Boolean = true
        open val title: UiString? = null
        open val subtitle: UiString? = null
        @DrawableRes open val image: Int? = null
        open val isProgressShowing: Boolean = false
        open val alpha: Float = BASE_ALPHA
        open val primaryAction: Action? = null
        open val secondaryAction: Action? = null
        open val browser: String? = null
        open val location: String? = null

        data class Validated(
            override val browser: String? = null,
            override val location: String? = null,
            override val primaryAction: ValidatedPrimaryAction,
            override val secondaryAction: ValidatedSecondaryAction
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
            @DrawableRes override val image = R.drawable.img_illustration_qrcode_auth_validated_152dp
        }

        data class Authenticating(
            override val browser: String? = null,
            override val location: String? = null,
            override val primaryAction: AuthenticatingPrimaryAction,
            override val secondaryAction: AuthenticatingSecondaryAction
        ) : Content() {
            override val type = AUTHENTICATING
            override val title: UiString = if (browser == null) {
                UiStringResWithParams(R.string.qrcode_auth_flow_validated_title, listOf(UiStringText(location ?: " ")))
            } else {
                UiStringResWithParams(
                        R.string.qrcode_auth_flow_validated_title,
                        listOf(UiStringText(browser), UiStringText(location ?: " "))
                )
            }
            override val subtitle: UiString = UiStringRes(R.string.qrcode_auth_flow_validated_subtitle)
            @DrawableRes override val image = R.drawable.img_illustration_qrcode_auth_validated_152dp
            override val alpha: Float = BLURRED_ALPHA
            override val isProgressShowing: Boolean = true
        }

        data class Done(
            override val primaryAction: DonePrimaryAction,
            override val secondaryAction: DoneSecondaryAction
        ) : Content() {
            override val type = DONE
            override val title: UiString = UiStringRes(R.string.qrcode_auth_flow_done_title)
            override val subtitle: UiString = UiStringRes(R.string.qrcode_auth_flow_done_subtitle)
            @DrawableRes override val image = R.drawable.img_illustration_qrcode_auth_login_success_218dp
        }
    }

    sealed class Action {
        open val label: UiString? = null
        open val isEnabled: Boolean = true
        open val isVisible: Boolean = true
        open val clickAction: (() -> Unit)? = null

        data class ValidatedPrimaryAction(override val clickAction: (() -> Unit)) : Action() {
            override val label: UiString = UiStringRes(R.string.qrcode_auth_flow_validated_primary_action)
        }

        data class ValidatedSecondaryAction(override val clickAction: (() -> Unit)) : Action() {
            override val label: UiString = UiStringRes(R.string.cancel)
        }

        object AuthenticatingPrimaryAction : Action() {
            override val label: UiString = UiStringRes(R.string.qrcode_auth_flow_validated_primary_action)
            override val isEnabled = false
        }

        object AuthenticatingSecondaryAction : Action() {
            override val label: UiString = UiStringRes(R.string.cancel)
            override val isEnabled = false
        }

        data class DonePrimaryAction(override val clickAction: (() -> Unit)) : Action() {
            override val label: UiString = UiStringRes(R.string.qrcode_auth_flow_dismiss)
        }

        object DoneSecondaryAction : Action() {
            override val isVisible = false
        }

        data class ErrorPrimaryAction(override val clickAction: (() -> Unit)) : Action() {
            override val label: UiString = UiStringRes(R.string.qrcode_auth_flow_scan_again)
        }

        data class ErrorSecondaryAction(override val clickAction: (() -> Unit)) : Action() {
            override val label: UiString = UiStringRes(R.string.cancel)
        }
    }
}
