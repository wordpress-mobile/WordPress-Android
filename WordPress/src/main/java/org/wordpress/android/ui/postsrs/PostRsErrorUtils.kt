package org.wordpress.android.ui.postsrs

import org.wordpress.android.R
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
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
    fun isAuthError(e: Exception?): Boolean {
        val api = unwrapException(e)
        val errorCode =
            (api as? WpApiException.WpException)?.errorCode
        return isAuthErrorReason(failureReason(e)) ||
            errorCode is WpErrorCode.Unauthorized ||
            errorCode is WpErrorCode
                .ApplicationPasswordNotFound ||
            errorCode is WpErrorCode
                .NoAuthenticatedAppPassword
    }

    /**
     * The auth half of [isAuthError] for callers holding a
     * [uniffi.wp_api.WpRequestResult.RequestExecutionFailed]
     * rather than a thrown exception: the rs client reports
     * the failure as a result variant, so there is nothing
     * to unwrap.
     */
    fun isAuthErrorReason(reason: RequestExecutionErrorReason?): Boolean =
        reason is RequestExecutionErrorReason
            .HttpAuthenticationRejectedError ||
            reason is RequestExecutionErrorReason
                .HttpAuthenticationRequiredError

    private fun failureReason(e: Exception?): RequestExecutionErrorReason? =
        (unwrapException(e) as? WpApiException.RequestExecutionFailed)
            ?.reason

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
    ): String {
        val failureReason = reason ?: failureReason(e)

        val resId = when {
            failureReason is RequestExecutionErrorReason
                .DeviceIsOfflineError ||
                !networkUtilsWrapper.isNetworkAvailable() ->
                R.string.error_generic_network

            isAuthError(e) || isAuthErrorReason(reason) ->
                R.string.post_rs_error_auth

            defaultResId != null -> defaultResId

            else -> R.string.request_failed_message
        }
        return resourceProvider.getString(resId)
    }
}
