package org.wordpress.android.fluxc.network.rest.wpapi.rs

import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.DEFAULT_REQUEST_ERROR_LOG_POLICY
import rs.wordpress.api.kotlin.RequestErrorLogger
import rs.wordpress.api.kotlin.WpRequestErrorLogger
import uniffi.wp_api.WpRequestErrorLogPolicy
import uniffi.wp_api.WpRequestUrlLogDetail
import uniffi.wp_api.WpResponseBodyLogDetail

/**
 * Keeps every value a request or a response carried out of the log line: query parameter values
 * are replaced with `REDACTED`, and the response reaches the line only as the error code it was
 * parsed into. The one value that survives is `rest_route`, which on a site using plain permalinks
 * carries the REST route itself and so names the endpoint that failed.
 *
 * The pairing for a client that reaches a site the app has no relationship with yet, where the
 * address is what a user typed and everything that comes back is written by a stranger. The
 * failure is still named — an endpoint, a status, an error code — but nothing that names a person
 * survives.
 */
val NO_LOGGED_VALUES_POLICY = WpRequestErrorLogPolicy(
    requestUrl = WpRequestUrlLogDetail.QUERY_KEYS_ONLY,
    responseBody = WpResponseBodyLogDetail.OMITTED
)

/**
 * Writes each failed wordpress-rs request to [AppLog] under [AppLog.T.API], at [policy].
 *
 * [DEFAULT_REQUEST_ERROR_LOG_POLICY] describes a failure as fully as it can without recording a
 * credential: query parameter values and the server's own account of what went wrong are kept,
 * the response body is not. Pass [NO_LOGGED_VALUES_POLICY] for a client that should record
 * neither.
 *
 * A user filing a support ticket attaches the [AppLog] buffer to it, so what a policy admits here
 * can leave the device.
 */
fun wpRsErrorLogger(
    policy: WpRequestErrorLogPolicy = DEFAULT_REQUEST_ERROR_LOG_POLICY
): RequestErrorLogger = WpRequestErrorLogger(policy) { message ->
    AppLog.e(AppLog.T.API, message)
}
