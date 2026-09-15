package org.wordpress.android.ui.accounts.login

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uniffi.wp_api.ApplicationPasswordsNotSupportedReason
import uniffi.wp_api.AutoDiscoveryAttemptFailure
import uniffi.wp_api.FetchAndParseApiRootFailure
import uniffi.wp_api.FindApiRootFailure
import uniffi.wp_api.KnownAuthenticationBlockingPlugin
import uniffi.wp_api.ParseApiRootFailureReason
import uniffi.wp_api.ParseUrlException
import uniffi.wp_api.RequestExecutionErrorReason
import uniffi.wp_api.RequestExecutionException
import uniffi.wp_api.RequestMethod
import uniffi.wp_api.ResponseBodyType
import uniffi.wp_api.WpApiDetails
import uniffi.wp_api.WpErrorCode
import kotlin.test.assertEquals

class DiscoveryAnalyticsTest {
    @Test
    fun `a malformed site URL reports the parse error type`() {
        val failure = AutoDiscoveryAttemptFailure.ParseSiteUrl(ParseUrlException.EmptyHost())

        assertEquals(
            mapOf("reason" to "parse_site_url", "error_code" to "EmptyHost"),
            failure.toDiscoveryFailure().props
        )
    }

    @Test
    fun `a site with no WordPress markers reports not_a_wordpress_site`() {
        val failure = AutoDiscoveryAttemptFailure.FindApiRoot(
            mock(),
            FindApiRootFailure.ProbablyNotAWordPressSite
        )

        assertEquals(mapOf("reason" to "not_a_wordpress_site"), failure.toDiscoveryFailure().props)
    }

    @Test
    fun `a homepage fetch failure reports the network reason and status code`() {
        val failure = AutoDiscoveryAttemptFailure.FindApiRoot(
            mock(),
            FindApiRootFailure.FetchHomepage(
                RequestExecutionException.RequestExecutionFailed(
                    statusCode = 403u,
                    redirects = null,
                    reason = RequestExecutionErrorReason.HttpForbiddenError("example.com"),
                    requestUrl = "https://example.com",
                    requestMethod = RequestMethod.GET,
                )
            )
        )

        assertEquals(
            mapOf(
                "reason" to "fetch_homepage_failed",
                "network_reason" to "http_forbidden",
                "status_code" to "403",
            ),
            failure.toDiscoveryFailure().props
        )
    }

    @Test
    fun `a timeout with no response reports the reason without a status code`() {
        val failure = AutoDiscoveryAttemptFailure.FetchAndParseApiRoot(
            mock(),
            mock(),
            FetchAndParseApiRootFailure.FetchApiRoot(
                RequestExecutionException.RequestExecutionFailed(
                    statusCode = null,
                    redirects = null,
                    reason = RequestExecutionErrorReason.HttpTimeoutError,
                    requestUrl = "https://example.com/wp-json",
                    requestMethod = RequestMethod.GET,
                )
            )
        )

        assertEquals(
            mapOf("reason" to "fetch_api_root_failed", "network_reason" to "http_timeout"),
            failure.toDiscoveryFailure().props
        )
    }

    @Test
    fun `a Wordfence-generated body is reported as a block, not a parse failure`() {
        val failure = parseApiRootFailure(ParseApiRootFailureReason.WORDFENCE_BLOCKING_ACCESS)

        assertEquals(
            mapOf("reason" to "wordfence_blocking_access", "response_body_type" to "maybe_html"),
            failure.toDiscoveryFailure().props
        )
    }

    @Test
    fun `an unrecognised unparseable body reports the body type only`() {
        val failure = parseApiRootFailure(reason = null)

        assertEquals(
            mapOf("reason" to "parse_api_root_failed", "response_body_type" to "maybe_html"),
            failure.toDiscoveryFailure().props
        )
    }

    @Test
    fun `a REST error envelope reports its code and status`() {
        val failure = AutoDiscoveryAttemptFailure.FetchAndParseApiRoot(
            mock(),
            mock(),
            FetchAndParseApiRootFailure.WpError(
                errorCode = WpErrorCode.CustomException("private_site"),
                errorMessage = "This site is private.",
                statusCode = 403u,
            )
        )

        assertEquals(
            mapOf("reason" to "wp_error", "error_code" to "private_site", "status_code" to "403"),
            failure.toDiscoveryFailure().props
        )
    }

    @Test
    fun `a known blocking plugin is named`() {
        val failure = applicationPasswordsNotSupported(
            ApplicationPasswordsNotSupportedReason.ApplicationPasswordBlockedByPlugin(
                KnownAuthenticationBlockingPlugin("Wordfence", "wordfence/v1", "https://example.com")
            )
        )

        assertEquals(
            mapOf("reason" to "app_passwords_blocked_by_plugin", "plugin" to "Wordfence"),
            failure.toDiscoveryFailure().props
        )
    }

    @Test
    fun `multiple blocking plugins are named from the site's api details`() {
        val apiDetails = mock<WpApiDetails>()
        whenever(apiDetails.applicationPasswordBlockingPlugins())
            .thenReturn(listOf(plugin("Wordfence"), plugin("Hostinger Tools")))
        val failure = AutoDiscoveryAttemptFailure.FetchAndParseApiRoot(
            mock(),
            mock(),
            FetchAndParseApiRootFailure.ApplicationPasswordsNotSupported(
                apiDetails,
                ApplicationPasswordsNotSupportedReason.ApplicationPasswordBlockedByMultiplePlugins,
            )
        )

        assertEquals(
            mapOf(
                "reason" to "app_passwords_blocked_by_multiple_plugins",
                "plugin" to "Wordfence,Hostinger Tools",
            ),
            failure.toDiscoveryFailure().props
        )
    }

    @Test
    fun `an http-only site is reported distinctly from an unsupported one`() {
        val httpSite = applicationPasswordsNotSupported(
            ApplicationPasswordsNotSupportedReason.ApplicationPasswordsDisabledForHttpSite
        )
        val unknown = applicationPasswordsNotSupported(reason = null)

        assertEquals(mapOf("reason" to "http_site"), httpSite.toDiscoveryFailure().props)
        assertEquals(mapOf("reason" to "app_passwords_not_supported"), unknown.toDiscoveryFailure().props)
    }

    @Test
    fun `a throwable out of discovery reports its type`() {
        assertEquals(
            mapOf("reason" to "exception", "error_code" to "IllegalStateException"),
            IllegalStateException("boom").toDiscoveryFailure().props
        )
    }

    @Test
    fun `no advertised auth URL is attributed to blocking plugins when the namespaces name them`() {
        assertEquals(
            mapOf("reason" to "no_app_passwords_url"),
            noAuthenticationUrlFailure(emptyList()).props
        )
        assertEquals(
            mapOf("reason" to "app_passwords_blocked_by_plugin", "plugin" to "Wordfence"),
            noAuthenticationUrlFailure(listOf(plugin("Wordfence"))).props
        )
        assertEquals(
            mapOf(
                "reason" to "app_passwords_blocked_by_multiple_plugins",
                "plugin" to "Wordfence,Hostinger Tools",
            ),
            noAuthenticationUrlFailure(listOf(plugin("Wordfence"), plugin("Hostinger Tools"))).props
        )
    }

    private fun parseApiRootFailure(reason: ParseApiRootFailureReason?) =
        AutoDiscoveryAttemptFailure.FetchAndParseApiRoot(
            mock(),
            mock(),
            FetchAndParseApiRootFailure.ParseApiRoot(
                parsingErrorMessage = "expected value",
                // The body itself must never reach the event — it's arbitrary site HTML.
                responseBody = "<p>Generated by Wordfence</p>",
                responseBodyType = ResponseBodyType.MAYBE_HTML,
                reason = reason,
            )
        )

    private fun applicationPasswordsNotSupported(reason: ApplicationPasswordsNotSupportedReason?) =
        AutoDiscoveryAttemptFailure.FetchAndParseApiRoot(
            mock(),
            mock(),
            FetchAndParseApiRootFailure.ApplicationPasswordsNotSupported(mock(), reason)
        )

    private fun plugin(name: String) = KnownAuthenticationBlockingPlugin(name, "$name/v1", "")
}
