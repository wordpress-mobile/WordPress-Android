package org.wordpress.android.ui.domains.usecases

import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.RequestExecutionErrorReason
import uniffi.wp_api.WpErrorCode

/**
 * The message WordPress.com sent with a refusal, for the screens that put it in
 * front of the user.
 */
internal fun WpRequestResult<*>.apiErrorMessage(): String? =
    (this as? WpRequestResult.WpError)?.errorMessage

/**
 * The code WordPress.com sent with a refusal, when wordpress-rs did not match it
 * to a [WpErrorCode] variant. Null when it did, and null for a failure that
 * carries no code.
 */
internal fun WpRequestResult<*>.apiErrorCode(): String? =
    ((this as? WpRequestResult.WpError)?.errorCode as? WpErrorCode.CustomException)?.v1

internal fun WpRequestResult<*>.isDeviceOffline(): Boolean =
    this is WpRequestResult.RequestExecutionFailed &&
            reason is RequestExecutionErrorReason.DeviceIsOfflineError
