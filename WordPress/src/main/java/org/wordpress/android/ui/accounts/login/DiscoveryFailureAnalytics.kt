package org.wordpress.android.ui.accounts.login

import uniffi.wp_api.ApplicationPasswordsNotSupportedReason
import uniffi.wp_api.AutoDiscoveryAttemptFailure
import uniffi.wp_api.FetchAndParseApiRootFailure
import uniffi.wp_api.FindApiRootFailure
import uniffi.wp_api.ParseApiRootFailureReason
import uniffi.wp_api.RequestExecutionErrorReason
import uniffi.wp_api.RequestExecutionException
import uniffi.wp_api.WpErrorCode
import java.util.Locale

internal const val REASON_TAG = "reason"
internal const val ERROR_CODE_TAG = "error_code"
internal const val STATUS_CODE_TAG = "status_code"
internal const val NETWORK_REASON_TAG = "network_reason"
internal const val PLUGIN_TAG = "plugin"
internal const val RESPONSE_BODY_TYPE_TAG = "response_body_type"

internal const val REASON_BLOCKED_BY_PLUGIN = "blocked_by_plugin"
internal const val REASON_NOT_SUPPORTED = "app_passwords_not_supported"
internal const val REASON_EXCEPTION = "exception"

private const val WORDFENCE = "Wordfence"

/**
 * Which flow asked for API discovery. The `background_rest_autodiscovery_*` events fire from the
 * foreground login flows too, so without this a card probe and a real login attempt are one number.
 * Only non-card sources count towards the `*_application_password_login` failure denominator.
 */
enum class DiscoverySource(val value: String) {
    MY_SITE_CARD("my_site_card"),
    LOGIN("login"),
    REAUTH_DIALOG("reauth_dialog"),
    AUTO_AUTH_FALLBACK("auto_auth_fallback"),
}

/**
 * Props for `background_rest_autodiscovery_failed`: always a `reason`, plus whatever the variant
 * knows. Deliberately excluded: `responseBody` (arbitrary site HTML), `localizedDescription()`
 * (translated, would fragment by locale), request URLs and redirect chains.
 */
internal fun AutoDiscoveryAttemptFailure.toAnalyticsProps(): Map<String, String> = when (this) {
    is AutoDiscoveryAttemptFailure.ParseSiteUrl -> props("invalid_url", ERROR_CODE_TAG to error.simpleName())
    is AutoDiscoveryAttemptFailure.FindApiRoot -> findApiRootFailure.toAnalyticsProps()
    is AutoDiscoveryAttemptFailure.FetchAndParseApiRoot -> fetchAndParseApiRootFailure.toAnalyticsProps()
}

/** Anything thrown out of discovery rather than reported by it. */
internal fun Throwable.toAnalyticsProps(): Map<String, String> =
    props(REASON_EXCEPTION, ERROR_CODE_TAG to simpleName())

private fun FindApiRootFailure.toAnalyticsProps(): Map<String, String> = when (this) {
    is FindApiRootFailure.FetchHomepage -> error.toNetworkProps()
    FindApiRootFailure.ProbablyNotAWordPressSite -> props("not_a_wordpress_site")
    FindApiRootFailure.RestApiDisabled -> props("rest_api_disabled")
}

private fun FetchAndParseApiRootFailure.toAnalyticsProps(): Map<String, String> = when (this) {
    is FetchAndParseApiRootFailure.FetchApiRoot -> error.toNetworkProps()
    is FetchAndParseApiRootFailure.ParseApiRoot -> when (reason) {
        ParseApiRootFailureReason.WORDFENCE_BLOCKING_ACCESS -> props(REASON_BLOCKED_BY_PLUGIN, PLUGIN_TAG to WORDFENCE)
        ParseApiRootFailureReason.SERVER_FATAL_ERROR -> props("server_fatal_error")
        null -> props(
            "invalid_api_root_response",
            RESPONSE_BODY_TYPE_TAG to responseBodyType.name.lowercase(Locale.ROOT),
        )
    }
    is FetchAndParseApiRootFailure.WpError -> props(
        "wp_error",
        ERROR_CODE_TAG to errorCode.analyticsName(),
        STATUS_CODE_TAG to statusCode.toString(),
    )
    is FetchAndParseApiRootFailure.ApplicationPasswordsNotSupported -> when (val reason = reason) {
        is ApplicationPasswordsNotSupportedReason.ApplicationPasswordBlockedByPlugin ->
            props(REASON_BLOCKED_BY_PLUGIN, PLUGIN_TAG to reason.plugin.name)
        // Only the single-plugin variant names its plugin; the site's API details know the rest.
        ApplicationPasswordsNotSupportedReason.ApplicationPasswordBlockedByMultiplePlugins -> props(
            REASON_BLOCKED_BY_PLUGIN,
            PLUGIN_TAG to apiDetails.applicationPasswordBlockingPlugins().joinToString(",") { it.name },
        )
        ApplicationPasswordsNotSupportedReason.ApplicationPasswordsDisabledForHttpSite -> props("http_site")
        ApplicationPasswordsNotSupportedReason.SiteIsLocalDevelopmentEnvironment -> props("local_dev_environment")
        null -> props(REASON_NOT_SUPPORTED)
    }
}

private fun RequestExecutionException.toNetworkProps(): Map<String, String> = when (this) {
    is RequestExecutionException.RequestExecutionFailed -> props(
        "network_error",
        NETWORK_REASON_TAG to reason.analyticsName(),
        STATUS_CODE_TAG to statusCode?.toString(),
    )
    is RequestExecutionException.MediaFileNotFound ->
        props("network_error", NETWORK_REASON_TAG to "media_file_not_found")
    is RequestExecutionException.MediaFileUnreadable ->
        props("network_error", NETWORK_REASON_TAG to "media_file_unreadable")
}

private fun RequestExecutionErrorReason.analyticsName(): String = when (this) {
    is RequestExecutionErrorReason.InvalidSslError -> "invalid_ssl"
    is RequestExecutionErrorReason.NonExistentSiteError -> "non_existent_site"
    is RequestExecutionErrorReason.HttpAuthenticationRequiredError -> "http_auth_required"
    is RequestExecutionErrorReason.HttpAuthenticationRejectedError -> "http_auth_rejected"
    is RequestExecutionErrorReason.HttpForbiddenError -> "http_forbidden"
    RequestExecutionErrorReason.HttpTimeoutError -> "http_timeout"
    is RequestExecutionErrorReason.MisconfiguredHttpAuthenticationError -> "misconfigured_http_auth"
    RequestExecutionErrorReason.MisconfiguredRateLimitError -> "misconfigured_rate_limit"
    is RequestExecutionErrorReason.DeviceIsOfflineError -> "device_offline"
    RequestExecutionErrorReason.CancellationError -> "cancelled"
    is RequestExecutionErrorReason.ConnectionError -> "connection_error"
    is RequestExecutionErrorReason.HttpError -> "http_error"
    is RequestExecutionErrorReason.GenericError -> "generic_error"
}

/**
 * The library generates one class per REST error code, so the class name is the code. Codes
 * without a dedicated class arrive as [WpErrorCode.CustomException] carrying the raw string.
 */
private fun WpErrorCode.analyticsName(): String = when (this) {
    is WpErrorCode.CustomException -> v1
    else -> simpleName()
}

private fun Any.simpleName(): String = javaClass.simpleName

private fun props(reason: String, vararg details: Pair<String, String?>): Map<String, String> =
    buildMap {
        put(REASON_TAG, reason)
        details.forEach { (key, value) -> if (value != null) put(key, value) }
    }
