package org.wordpress.android.fluxc.network.rest.wpapi.rs

import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.DEFAULT_REQUEST_ERROR_LOG_POLICY
import rs.wordpress.api.kotlin.RequestErrorLogger
import rs.wordpress.api.kotlin.WpRequestErrorLogger
import uniffi.wp_api.WpRequestErrorLogPolicy

/**
 * A user filing a support ticket attaches the [AppLog] buffer to it, so what [policy] admits here
 * can leave the device.
 */
fun wpRsErrorLogger(
    policy: WpRequestErrorLogPolicy = DEFAULT_REQUEST_ERROR_LOG_POLICY
): RequestErrorLogger = WpRequestErrorLogger(policy) { message ->
    AppLog.e(AppLog.T.API, message)
}
