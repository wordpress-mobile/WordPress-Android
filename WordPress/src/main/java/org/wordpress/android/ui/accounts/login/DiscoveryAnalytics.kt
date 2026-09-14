package org.wordpress.android.ui.accounts.login

import uniffi.wp_api.ApplicationPasswordsNotSupportedReason
import uniffi.wp_api.AutoDiscoveryAttemptFailure
import uniffi.wp_api.FetchAndParseApiRootFailure
import uniffi.wp_api.FindApiRootFailure
import uniffi.wp_api.KnownAuthenticationBlockingPlugin
import uniffi.wp_api.ParseApiRootFailureReason
import uniffi.wp_api.RequestExecutionErrorReason
import uniffi.wp_api.RequestExecutionException
import uniffi.wp_api.WpErrorCode
import java.util.Locale

internal const val URL_TAG = "url"
internal const val SUCCESS_TAG = "success"
internal const val REASON_TAG = "reason"
internal const val SOURCE_TAG = "source"
internal const val ERROR_TAG = "error"
internal const val IS_WPCOM_TAG = "is_wpcom"
internal const val NETWORK_REASON_TAG = "network_reason"
internal const val STATUS_CODE_TAG = "status_code"
internal const val ERROR_CODE_TAG = "error_code"
internal const val PLUGIN_TAG = "plugin"
internal const val RESPONSE_BODY_TYPE_TAG = "response_body_type"

/**
 * Which flow asked for API discovery. The `background_rest_autodiscovery_*` events are also fired
 * from the foreground login flows, so without this the background card probes and real login
 * attempts are one indistinguishable number.
 */
enum class DiscoverySource(val value: String) {
    MY_SITE_CARD("my_site_card"),
    LOGIN("login"),
    REAUTH_DIALOG("reauth_dialog"),
    AUTO_AUTH_FALLBACK("auto_auth_fallback"),
}

/**
 * A discovery failure reduced to something a query can group on. [reason] is the headline cause;
 * [details] carries whatever the underlying variant knew (status code, REST error code, blocking
 * plugin name).
 */
internal data class DiscoveryFailure(
    val reason: String,
    val details: Map<String, String> = emptyMap(),
) {
    val props: Map<String, String> get() = details + (REASON_TAG to reason)
}

/**
 * Classify a wordpress-rs discovery failure.
 *
 * The slugs follow the library's own variant names so Android and iOS — which read the same Rust
 * taxonomy — group the same way without a translation layer. Deliberately excluded from the props:
 * `ParseApiRoot.responseBody` (arbitrary site HTML) and `localizedDescription()` (a translated
 * sentence, so it would fragment by device locale).
 */
internal fun AutoDiscoveryAttemptFailure.toDiscoveryFailure(): DiscoveryFailure = when (this) {
    is AutoDiscoveryAttemptFailure.ParseSiteUrl -> DiscoveryFailure(
        reason = "parse_site_url",
        details = mapOf(ERROR_CODE_TAG to error.javaClass.simpleName),
    )
    is AutoDiscoveryAttemptFailure.FindApiRoot -> findApiRootFailure.toDiscoveryFailure()
    is AutoDiscoveryAttemptFailure.FetchAndParseApiRoot -> fetchAndParseApiRootFailure.toDiscoveryFailure()
}

/** Anything thrown out of discovery rather than reported by it — we only have the type. */
internal fun Throwable.toDiscoveryFailure() = DiscoveryFailure(
    reason = "exception",
    details = mapOf(ERROR_CODE_TAG to javaClass.simpleName),
)

/**
 * Discovery reached and parsed the API root, but the site advertised no application-passwords URL.
 * When its REST namespaces name plugins we know block application passwords, say so: that's a cause
 * someone can act on, and it's the only place we can spot it. The library's own
 * [ApplicationPasswordsNotSupportedReason] only fires when the API root itself fails to parse.
 */
internal fun noAuthenticationUrlFailure(
    blockingPlugins: List<KnownAuthenticationBlockingPlugin>
): DiscoveryFailure = when {
    blockingPlugins.isEmpty() -> DiscoveryFailure("no_app_passwords_url")
    blockingPlugins.size == 1 -> DiscoveryFailure(
        reason = BLOCKED_BY_PLUGIN_REASON,
        details = mapOf(PLUGIN_TAG to blockingPlugins.first().name),
    )
    else -> DiscoveryFailure(
        reason = BLOCKED_BY_MULTIPLE_PLUGINS_REASON,
        details = mapOf(PLUGIN_TAG to blockingPlugins.joinToString(separator = ",") { it.name }),
    )
}

private fun FindApiRootFailure.toDiscoveryFailure(): DiscoveryFailure = when (this) {
    is FindApiRootFailure.FetchHomepage -> DiscoveryFailure("fetch_homepage_failed", error.networkDetails())
    FindApiRootFailure.ProbablyNotAWordPressSite -> DiscoveryFailure("not_a_wordpress_site")
    FindApiRootFailure.RestApiDisabled -> DiscoveryFailure("rest_api_disabled")
}

private fun FetchAndParseApiRootFailure.toDiscoveryFailure(): DiscoveryFailure = when (this) {
    is FetchAndParseApiRootFailure.FetchApiRoot ->
        DiscoveryFailure("fetch_api_root_failed", error.networkDetails())
    is FetchAndParseApiRootFailure.ParseApiRoot -> DiscoveryFailure(
        reason = when (reason) {
            ParseApiRootFailureReason.WORDFENCE_BLOCKING_ACCESS -> "wordfence_blocking_access"
            ParseApiRootFailureReason.SERVER_FATAL_ERROR -> "server_fatal_error"
            null -> "parse_api_root_failed"
        },
        details = mapOf(RESPONSE_BODY_TYPE_TAG to responseBodyType.name.lowercase(Locale.US)),
    )
    is FetchAndParseApiRootFailure.WpError -> DiscoveryFailure(
        reason = "wp_error",
        details = mapOf(
            ERROR_CODE_TAG to errorCode.analyticsName(),
            STATUS_CODE_TAG to statusCode.toString(),
        ),
    )
    is FetchAndParseApiRootFailure.ApplicationPasswordsNotSupported -> reason.toDiscoveryFailure()
}

private fun ApplicationPasswordsNotSupportedReason?.toDiscoveryFailure(): DiscoveryFailure = when (this) {
    is ApplicationPasswordsNotSupportedReason.ApplicationPasswordBlockedByPlugin -> DiscoveryFailure(
        reason = BLOCKED_BY_PLUGIN_REASON,
        details = mapOf(PLUGIN_TAG to plugin.name),
    )
    ApplicationPasswordsNotSupportedReason.ApplicationPasswordBlockedByMultiplePlugins ->
        DiscoveryFailure(BLOCKED_BY_MULTIPLE_PLUGINS_REASON)
    ApplicationPasswordsNotSupportedReason.SiteIsLocalDevelopmentEnvironment ->
        DiscoveryFailure("local_dev_environment")
    ApplicationPasswordsNotSupportedReason.ApplicationPasswordsDisabledForHttpSite ->
        DiscoveryFailure("http_site")
    null -> DiscoveryFailure("app_passwords_not_supported")
}

private fun RequestExecutionException.networkDetails(): Map<String, String> = when (this) {
    is RequestExecutionException.RequestExecutionFailed -> buildMap {
        put(NETWORK_REASON_TAG, reason.analyticsName())
        statusCode?.let { put(STATUS_CODE_TAG, it.toString()) }
    }
    is RequestExecutionException.MediaFileNotFound -> mapOf(NETWORK_REASON_TAG to "media_file_not_found")
    is RequestExecutionException.MediaFileUnreadable -> mapOf(NETWORK_REASON_TAG to "media_file_unreadable")
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
 * The library models ~170 REST error codes as one object per code, so the generated class name is
 * exactly the code we want to group on. Codes without a dedicated variant arrive as
 * [WpErrorCode.CustomException] carrying the raw string — that's where `private_site` shows up.
 */
private fun WpErrorCode.analyticsName(): String = when (this) {
    is WpErrorCode.CustomException -> v1
    else -> javaClass.simpleName
}

private const val BLOCKED_BY_PLUGIN_REASON = "app_passwords_blocked_by_plugin"
private const val BLOCKED_BY_MULTIPLE_PLUGINS_REASON = "app_passwords_blocked_by_multiple_plugins"
