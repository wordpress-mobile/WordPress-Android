package org.wordpress.android.ui.postsrs

import org.wordpress.android.R
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.RequestExecutionErrorReason
import uniffi.wp_api.WpApiException
import uniffi.wp_api.WpErrorCode
import uniffi.wp_mobile.FetchException

/**
 * Shared error-handling helpers for the RS post screens.
 */
internal object PostRsErrorUtils {
    /**
     * Extracts the underlying [WpApiException] from a
     * [FetchException.Api] wrapper so callers can inspect
     * API-level error details without knowing the wrapper.
     */
    fun unwrapException(e: Exception?): Exception? =
        (e as? FetchException.Api)?.v1 ?: e

    /**
     * Returns true when the exception represents an
     * authentication failure (rejected credentials,
     * missing app-password, etc.).
     */
    fun isAuthError(e: Exception?): Boolean =
        isAuthError(failureReason(e), errorCode(e))

    /**
     * The same check for callers holding a
     * [uniffi.wp_api.WpRequestResult] rather than a thrown
     * exception: the rs client reports these failures as
     * result variants, so there is nothing to unwrap. A
     * rejected credential arrives as a [reason] when the
     * request never completed and as an [errorCode] when the
     * server answered with a WP error envelope, so both have
     * to be checked.
     */
    fun isAuthError(
        reason: RequestExecutionErrorReason?,
        errorCode: WpErrorCode?
    ): Boolean =
        reason is RequestExecutionErrorReason
            .HttpAuthenticationRejectedError ||
            reason is RequestExecutionErrorReason
                .HttpAuthenticationRequiredError ||
            errorCode is WpErrorCode.Unauthorized ||
            errorCode is WpErrorCode
                .ApplicationPasswordNotFound ||
            errorCode is WpErrorCode
                .NoAuthenticatedAppPassword

    /**
     * Returns true when the server refused a page number past
     * the end of a list. A caller paging without a known total
     * reads this as "no more pages", not as a failure.
     */
    fun isPastLastPage(e: Exception?): Boolean =
        errorCode(e) is WpErrorCode.PostInvalidPageNumber

    /**
     * A short description of a failed [WpRequestResult], for logging.
     *
     * Only the `WpError` variant carries an `errorMessage`, so logging that alone reports "null"
     * for the six other ways a request can fail - which is exactly when you most need to know
     * which one it was. Deliberately reports codes and reasons only: the AppLog buffer is attached
     * to support tickets, so response bodies and request URLs stay out of it.
     */
    fun describeFailure(result: WpRequestResult<*>): String = when (result) {
        is WpRequestResult.WpError ->
            "WpError ${result.errorCode} (HTTP ${result.statusCode}): ${result.errorMessage}"
        is WpRequestResult.RequestExecutionFailed ->
            "RequestExecutionFailed ${result.reason} (HTTP ${result.statusCode})"
        is WpRequestResult.InvalidHttpStatusCode ->
            "InvalidHttpStatusCode ${result.statusCode}"
        is WpRequestResult.ResponseParsingError ->
            "ResponseParsingError ${result.reason}"
        is WpRequestResult.SiteUrlParsingError -> "SiteUrlParsingError ${result.reason}"
        is WpRequestResult.UnknownError -> "UnknownError (HTTP ${result.statusCode})"
        // Anything else - including variants a later wordpress-rs adds - at least names itself.
        else -> result::class.simpleName.orEmpty()
    }

    private fun failureReason(e: Exception?): RequestExecutionErrorReason? =
        (unwrapException(e) as? WpApiException.RequestExecutionFailed)
            ?.reason

    private fun errorCode(e: Exception?): WpErrorCode? =
        (unwrapException(e) as? WpApiException.WpException)?.errorCode

    /**
     * Returns a user-friendly error string based on the
     * exception type. Detects offline, auth, and generic
     * errors.
     */
    fun friendlyErrorMessage(
        e: Exception? = null,
        defaultResId: Int? = null,
        resourceProvider: ResourceProvider,
        networkUtilsWrapper: NetworkUtilsWrapper,
        reason: RequestExecutionErrorReason? = null,
        errorCode: WpErrorCode? = null,
    ): String {
        val failureReason = reason ?: failureReason(e)

        val resId = when {
            failureReason is RequestExecutionErrorReason
                .DeviceIsOfflineError ||
                !networkUtilsWrapper.isNetworkAvailable() ->
                R.string.error_generic_network

            isAuthError(failureReason, errorCode ?: errorCode(e)) ->
                R.string.post_rs_error_auth

            defaultResId != null -> defaultResId

            else -> R.string.request_failed_message
        }
        return resourceProvider.getString(resId)
    }
}
