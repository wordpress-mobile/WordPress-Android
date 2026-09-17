package org.wordpress.android.ui.accounts.login

import org.wordpress.android.ui.accounts.applicationpassword.ApplicationPasswordCreationTracker
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper.DiscoveryResult.FailureReason
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

private const val ERROR_CODE_TAG = "error_code"
private const val STATUS_CODE_TAG = "status_code"
private const val NETWORK_REASON_TAG = "network_reason"
private const val PLUGIN_TAG = "plugin"
private const val RESPONSE_BODY_TYPE_TAG = "response_body_type"

private const val REASON_NETWORK_ERROR = "network_error"
private const val REASON_BLOCKED_BY_PLUGIN = "blocked_by_plugin"
private const val REASON_NOT_SUPPORTED = "app_passwords_not_supported"
private const val REASON_NO_AUTH_URL = "no_auth_url_advertised"
private const val REASON_EXCEPTION = "exception"
private const val WORDFENCE = "Wordfence"

// WordPress.com returns this error code from the REST root of a site whose Privacy setting is
// Private (or Coming Soon). The gate sits in front of WordPress, so discovery never reaches the
// API — the site's Application Password support is irrelevant to the failure.
private const val PRIVATE_SITE_ERROR_CODE = "private_site"

// A REST error `code` is whatever the site's error envelope says, so a plugin or WAF could put
// anything there. Only a slug-shaped code is worth a bucket of its own.
private val ERROR_CODE_SLUG = Regex("[A-Za-z0-9_.-]{1,64}")
private val CAMEL_BOUNDARY = Regex("(?<=[a-z0-9])(?=[A-Z])")

/**
 * Which flow asked for API discovery. The `background_rest_autodiscovery_*` events fire from the
 * foreground login flows too, so without this a card probe and a real login attempt are one number.
 * Values match the `source` the same flows put on the `application_password_created` that follows.
 *
 * @property isLoginAttempt whether a failed discovery from this flow is a failed login. A card
 * probe isn't one: it re-runs on every My Site build, so counting it would inflate the
 * `*_application_password_login` denominator with every re-probe of a broken site. Nor is the
 * re-discovery run to recover a missing API root before credentials are stored: the login it
 * belongs to reports its own outcome.
 */
enum class DiscoverySource(val value: String, val isLoginAttempt: Boolean) {
    MY_SITE_CARD("my_site_card", isLoginAttempt = false),
    API_ROOT_RECOVERY("api_root_recovery", isLoginAttempt = false),
    LOGIN(ApplicationPasswordCreationTracker.SOURCE_LOGIN, isLoginAttempt = true),
    REAUTH_DIALOG(ApplicationPasswordCreationTracker.SOURCE_REAUTH, isLoginAttempt = true),
    AUTO_AUTH_FALLBACK(ApplicationPasswordCreationTracker.SOURCE_MIGRATION, isLoginAttempt = true),
}

/**
 * A discovery failure reduced to what the events and the UI need. [reason] is the slug
 * `background_rest_autodiscovery_failed` groups on, [details] is whatever the variant knew (status
 * code, REST error code, blocking plugin), and [userFacing] is the cause worth naming to the user,
 * when there is one.
 */
internal data class DiscoveryFailure(
    val reason: String,
    val details: Map<String, String> = emptyMap(),
    val userFacing: FailureReason = FailureReason.Unknown,
) {
    val props: Map<String, String> get() = details + (REASON_TAG to reason)
}

/** Discovery succeeded but the site advertises no application-passwords URL. */
internal fun notSupportedFailure() =
    DiscoveryFailure(REASON_NO_AUTH_URL, userFacing = FailureReason.NotSupported)

/** Anything thrown out of discovery rather than reported by it. */
internal fun unexpectedDiscoveryFailure(throwable: Throwable) =
    DiscoveryFailure(REASON_EXCEPTION, mapOf(ERROR_CODE_TAG to throwable.analyticsSlug()))

/**
 * Classify a wordpress-rs discovery failure. Deliberately excluded from the details:
 * `responseBody` (arbitrary site HTML), `localizedDescription()` (translated, would fragment by
 * locale), request URLs and redirect chains.
 */
internal fun AutoDiscoveryAttemptFailure.toDiscoveryFailure(): DiscoveryFailure = when (this) {
    is AutoDiscoveryAttemptFailure.ParseSiteUrl ->
        DiscoveryFailure("invalid_url", mapOf(ERROR_CODE_TAG to error.analyticsSlug()))
    is AutoDiscoveryAttemptFailure.FindApiRoot -> findApiRootFailure.toDiscoveryFailure()
    is AutoDiscoveryAttemptFailure.FetchAndParseApiRoot -> fetchAndParseApiRootFailure.toDiscoveryFailure()
}

private fun FindApiRootFailure.toDiscoveryFailure(): DiscoveryFailure = when (this) {
    is FindApiRootFailure.FetchHomepage -> error.toDiscoveryFailure()
    FindApiRootFailure.ProbablyNotAWordPressSite -> DiscoveryFailure("not_a_wordpress_site")
    FindApiRootFailure.RestApiDisabled -> DiscoveryFailure("rest_api_disabled")
}

private fun FetchAndParseApiRootFailure.toDiscoveryFailure(): DiscoveryFailure = when (this) {
    is FetchAndParseApiRootFailure.FetchApiRoot -> error.toDiscoveryFailure()
    is FetchAndParseApiRootFailure.ParseApiRoot -> when (reason) {
        ParseApiRootFailureReason.WORDFENCE_BLOCKING_ACCESS ->
            DiscoveryFailure(REASON_BLOCKED_BY_PLUGIN, mapOf(PLUGIN_TAG to WORDFENCE))
        ParseApiRootFailureReason.SERVER_FATAL_ERROR -> DiscoveryFailure("server_fatal_error")
        null -> DiscoveryFailure(
            "invalid_api_root_response",
            mapOf(RESPONSE_BODY_TYPE_TAG to responseBodyType.name.lowercase(Locale.ROOT)),
        )
    }
    // A REST error envelope means we reached the site, so its `code` is a reliable signal.
    // `private_site` has no dedicated WpErrorCode and arrives as a CustomException with the raw
    // code; it is the one cause here worth naming to the user.
    is FetchAndParseApiRootFailure.WpError -> DiscoveryFailure(
        "wp_error",
        mapOf(ERROR_CODE_TAG to errorCode.analyticsName(), STATUS_CODE_TAG to statusCode.toString()),
        userFacing = if (errorCode.rawCode() == PRIVATE_SITE_ERROR_CODE) {
            FailureReason.PrivateSite
        } else {
            FailureReason.Unknown
        },
    )
    is FetchAndParseApiRootFailure.ApplicationPasswordsNotSupported -> when (val why = reason) {
        is ApplicationPasswordsNotSupportedReason.ApplicationPasswordBlockedByPlugin ->
            DiscoveryFailure(REASON_BLOCKED_BY_PLUGIN, mapOf(PLUGIN_TAG to why.plugin.name))
        // Only the single-plugin variant names its plugin; the site's API details know the rest.
        ApplicationPasswordsNotSupportedReason.ApplicationPasswordBlockedByMultiplePlugins -> {
            val names = apiDetails.applicationPasswordBlockingPlugins().map { it.name }.sorted()
            DiscoveryFailure(REASON_BLOCKED_BY_PLUGIN, mapOf(PLUGIN_TAG to names.joinToString(",")))
        }
        ApplicationPasswordsNotSupportedReason.ApplicationPasswordsDisabledForHttpSite ->
            DiscoveryFailure("http_site")
        ApplicationPasswordsNotSupportedReason.SiteIsLocalDevelopmentEnvironment ->
            DiscoveryFailure("local_dev_environment")
        null -> DiscoveryFailure(REASON_NOT_SUPPORTED)
    }
}

private fun RequestExecutionException.toDiscoveryFailure(): DiscoveryFailure = when (this) {
    is RequestExecutionException.RequestExecutionFailed -> DiscoveryFailure(
        REASON_NETWORK_ERROR,
        mapOf(NETWORK_REASON_TAG to reason.analyticsName()) +
            listOfNotNull(statusCode?.let { STATUS_CODE_TAG to it.toString() }),
    )
    // Upload-only variants that discovery's GETs can't produce; named by class like other surprises.
    else -> DiscoveryFailure(REASON_NETWORK_ERROR, mapOf(NETWORK_REASON_TAG to analyticsSlug()))
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

/** The raw `code` string for codes without a dedicated class; null for the generated ones. */
private fun WpErrorCode.rawCode(): String? = (this as? WpErrorCode.CustomException)?.v1

/**
 * The library generates one class per REST error code, named after it in PascalCase; snake-casing
 * the class name gives the code back (`RestForbidden` is `rest_forbidden`). Codes without a
 * dedicated class arrive as [WpErrorCode.CustomException] carrying the raw string.
 */
private fun WpErrorCode.analyticsName(): String {
    val raw = rawCode() ?: return analyticsSlug()
    return if (ERROR_CODE_SLUG.matches(raw)) raw else "custom"
}

/** The runtime class name as a slug, so every `error_code` value shares one casing. */
private fun Any.analyticsSlug(): String =
    javaClass.simpleName.replace(CAMEL_BOUNDARY, "_").lowercase(Locale.ROOT)
