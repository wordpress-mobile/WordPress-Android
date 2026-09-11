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
 * The code WordPress.com sent with a refusal, for the endpoints that define
 * codes of their own. Codes the WordPress REST API also uses are modelled as
 * [WpErrorCode] variants and come back null here.
 */
internal fun WpRequestResult<*>.apiErrorCode(): String? =
    ((this as? WpRequestResult.WpError)?.errorCode as? WpErrorCode.CustomException)?.v1

internal fun WpRequestResult<*>.isDeviceOffline(): Boolean =
    this is WpRequestResult.RequestExecutionFailed &&
            reason is RequestExecutionErrorReason.DeviceIsOfflineError
