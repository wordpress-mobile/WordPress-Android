package org.wordpress.android.ui.domains.usecases

import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.RequestExecutionErrorReason

/**
 * The message WordPress.com sent with a refusal, for the screens that put it in
 * front of the user.
 */
internal fun WpRequestResult<*>.apiErrorMessage(): String? =
    (this as? WpRequestResult.WpError)?.errorMessage

internal fun WpRequestResult<*>.isDeviceOffline(): Boolean =
    this is WpRequestResult.RequestExecutionFailed &&
            reason is RequestExecutionErrorReason.DeviceIsOfflineError
