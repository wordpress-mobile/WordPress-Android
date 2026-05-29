package org.wordpress.android.ui.mysite.cards.applicationpassword

import kotlinx.coroutines.CancellationException
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.RequestExecutionErrorReason
import uniffi.wp_api.WpErrorCode
import java.net.HttpURLConnection
import javax.inject.Inject

/**
 * Validates that the SiteModel's application-password credentials still work against the site's
 * direct host. Uses [WpApiClientProvider.getApplicationPasswordClient] so the call exercises the
 * application password specifically — `getWpApiClient` would route WPCom-flagged sites through the
 * bearer-token path and would not catch a revoked password.
 *
 * Classification is intentionally asymmetric: a return of [Outcome.Invalid] cascades into a
 * credential wipe + re-mint in the caller, so we only classify as Invalid when we have positive
 * evidence the server rejected the credential — an auth-specific [WpErrorCode], an auth-specific
 * [RequestExecutionErrorReason], or a [WpRequestResult.WpError] with a 401/403 status. Everything
 * ambiguous — 5xx, parse errors, offline, DNS — falls to [Outcome.NetworkUnavailable], which
 * hides the card and lets the next foreground retry.
 */
class ApplicationPasswordValidator @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider,
    private val appLogWrapper: AppLogWrapper,
) {
    suspend fun validate(site: SiteModel): Outcome {
        appLogWrapper.d(
            AppLog.T.MAIN,
            "A_P: Validating application password for ${site.url} as user='${site.apiRestUsernamePlain}'"
        )
        return try {
            val client = wpApiClientProvider.getApplicationPasswordClient(site)
            val response = client.request { it.users().retrieveMeWithViewContext() }
            appLogWrapper.d(AppLog.T.MAIN, "A_P: Validation response: ${response::class.simpleName}")
            classify(response)
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            appLogWrapper.e(
                AppLog.T.MAIN,
                "A_P: Validation exception for ${site.url}: ${e::class.simpleName}: ${e.message}"
            )
            Outcome.NetworkUnavailable
        }
    }

    private fun <T> classify(response: WpRequestResult<T>): Outcome = when (response) {
        is WpRequestResult.Success -> Outcome.Valid

        // A WpError is the server returning a parseable error envelope. Treat any 401/403 as an
        // auth rejection regardless of the WpErrorCode value — WordPress emits a wide variety of
        // codes for credential failures (`incorrect_password`, `invalid_username`,
        // `application_passwords_disabled_for_user`, plugin-defined codes, etc.) and many of them
        // get parsed as `WpErrorCode.CustomError` via the library's untagged fallback. Status
        // code is the reliable signal. Additionally, accept a small allowlist of WpErrorCode
        // names (e.g. `ApplicationPasswordNotFound`, which comes back with 404) that signal an
        // unusable credential outside the 401/403 status range.
        is WpRequestResult.WpError -> if (
            isAuthErrorCode(response.errorCode) || isAuthStatusCode(response.statusCode)
        ) {
            Outcome.Invalid
        } else {
            // Parseable error envelope without an auth-rejection signal (e.g. a 5xx returned as
            // a structured WpError). Ambiguous — don't wipe creds.
            Outcome.NetworkUnavailable
        }

        is WpRequestResult.RequestExecutionFailed -> if (isAuthErrorReason(response.reason)) {
            Outcome.Invalid
        } else {
            // Timeouts, offline, DNS, SSL, generic transport errors — all non-destructive.
            Outcome.NetworkUnavailable
        }

        // UnknownError (4xx/5xx without parseable WP error JSON), parse errors, and any other
        // variants are all ambiguous. Default to non-destructive.
        else -> Outcome.NetworkUnavailable
    }

    private fun isAuthErrorCode(code: WpErrorCode): Boolean =
        code is WpErrorCode.Unauthorized ||
            code is WpErrorCode.Forbidden ||
            code is WpErrorCode.ApplicationPasswordNotFound ||
            code is WpErrorCode.NoAuthenticatedAppPassword

    private fun isAuthStatusCode(statusCode: UInt): Boolean =
        statusCode.toInt() == HttpURLConnection.HTTP_UNAUTHORIZED ||
            statusCode.toInt() == HttpURLConnection.HTTP_FORBIDDEN

    private fun isAuthErrorReason(reason: RequestExecutionErrorReason): Boolean =
        reason is RequestExecutionErrorReason.HttpAuthenticationRejectedError ||
            reason is RequestExecutionErrorReason.HttpAuthenticationRequiredError ||
            reason is RequestExecutionErrorReason.HttpForbiddenError

    enum class Outcome { Valid, Invalid, NetworkUnavailable }
}
