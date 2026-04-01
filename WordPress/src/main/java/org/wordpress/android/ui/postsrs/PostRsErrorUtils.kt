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
        val reason =
            (api as? WpApiException.RequestExecutionFailed)
                ?.reason
        val errorCode =
            (api as? WpApiException.WpException)?.errorCode
        return reason is RequestExecutionErrorReason
            .HttpAuthenticationRejectedError ||
            reason is RequestExecutionErrorReason
                .HttpAuthenticationRequiredError ||
            errorCode is WpErrorCode.Unauthorized ||
            errorCode is WpErrorCode
                .ApplicationPasswordNotFound ||
            errorCode is WpErrorCode
                .NoAuthenticatedAppPassword
    }

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
    ): String {
        val api = unwrapException(e)
        val reason =
            (api as? WpApiException.RequestExecutionFailed)
                ?.reason

        val resId = when {
            reason is RequestExecutionErrorReason
                .DeviceIsOfflineError ||
                !networkUtilsWrapper.isNetworkAvailable() ->
                R.string.error_generic_network

            isAuthError(e) -> R.string.post_rs_error_auth

            defaultResId != null -> defaultResId

            else -> R.string.request_failed_message
        }
        return resourceProvider.getString(resId)
    }
}
