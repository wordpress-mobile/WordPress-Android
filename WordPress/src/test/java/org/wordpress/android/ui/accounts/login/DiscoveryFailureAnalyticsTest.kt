package org.wordpress.android.ui.accounts.login

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper.DiscoveryResult.FailureReason
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

class DiscoveryFailureAnalyticsTest {
    @Test
    fun `unparseable site url`() {
        val failure = AutoDiscoveryAttemptFailure.ParseSiteUrl(ParseUrlException.EmptyHost())

        assertEquals(mapOf("reason" to "invalid_url", "error_code" to "EmptyHost"), failure.toDiscoveryFailure().props)
    }

    @Test
    fun `not a WordPress site`() {
        val failure = AutoDiscoveryAttemptFailure.FindApiRoot(mock(), FindApiRootFailure.ProbablyNotAWordPressSite)

        assertEquals(mapOf("reason" to "not_a_wordpress_site"), failure.toDiscoveryFailure().props)
    }

    @Test
    fun `network failure carries the reason and status code`() {
        val failure = fetchAndParse(
            FetchAndParseApiRootFailure.FetchApiRoot(
                requestFailed(RequestExecutionErrorReason.HttpTimeoutError, statusCode = 503u)
            )
        )

        assertEquals(
            mapOf("reason" to "network_error", "network_reason" to "http_timeout", "status_code" to "503"),
            failure.toDiscoveryFailure().props
        )
    }

    @Test
    fun `network failure without a status code omits it`() {
        val failure = AutoDiscoveryAttemptFailure.FindApiRoot(
            mock(),
            FindApiRootFailure.FetchHomepage(
                requestFailed(RequestExecutionErrorReason.DeviceIsOfflineError("offline"), statusCode = null)
            )
        )

        assertEquals(
            mapOf("reason" to "network_error", "network_reason" to "device_offline"),
            failure.toDiscoveryFailure().props
        )
    }

    @Test
    fun `Wordfence blocking the API root is a plugin block`() {
        val failure = fetchAndParse(parseApiRoot(ParseApiRootFailureReason.WORDFENCE_BLOCKING_ACCESS))

        assertEquals(
            mapOf("reason" to "blocked_by_plugin", "plugin" to "Wordfence"),
            failure.toDiscoveryFailure().props
        )
    }

    @Test
    fun `unrecognised API root response reports its body type, not its body`() {
        val failure = fetchAndParse(parseApiRoot(reason = null))

        assertEquals(
            mapOf("reason" to "invalid_api_root_response", "response_body_type" to "maybe_html"),
            failure.toDiscoveryFailure().props
        )
    }

    @Test
    fun `REST error envelope carries the raw custom code and status`() {
        val failure = fetchAndParse(wpError("rest_forbidden"))

        assertEquals(
            mapOf("reason" to "wp_error", "error_code" to "rest_forbidden", "status_code" to "403"),
            failure.toDiscoveryFailure().props
        )
    }

    @Test
    fun `REST error code that is not a slug is not shipped`() {
        val failure = fetchAndParse(wpError("blocked: client 203.0.113.7 <script>"))

        assertEquals(
            mapOf("reason" to "wp_error", "error_code" to "custom", "status_code" to "403"),
            failure.toDiscoveryFailure().props
        )
    }

    @Test
    fun `private_site is the one REST error named to the user`() {
        assertEquals(FailureReason.PrivateSite, fetchAndParse(wpError("private_site")).toDiscoveryFailure().userFacing)
        assertEquals(FailureReason.Unknown, fetchAndParse(wpError("rest_forbidden")).toDiscoveryFailure().userFacing)
        assertEquals(
            FailureReason.Unknown,
            AutoDiscoveryAttemptFailure.FindApiRoot(mock(), FindApiRootFailure.ProbablyNotAWordPressSite)
                .toDiscoveryFailure().userFacing
        )
    }

    @Test
    fun `single blocking plugin is named`() {
        val failure = fetchAndParse(
            notSupported(ApplicationPasswordsNotSupportedReason.ApplicationPasswordBlockedByPlugin(plugin("Wordfence")))
        )

        assertEquals(
            mapOf("reason" to "blocked_by_plugin", "plugin" to "Wordfence"),
            failure.toDiscoveryFailure().props
        )
    }

    @Test
    fun `multiple blocking plugins are sorted so order does not split the bucket`() {
        val apiDetails = mock<WpApiDetails>()
        whenever(apiDetails.applicationPasswordBlockingPlugins())
            .thenReturn(listOf(plugin("Wordfence"), plugin("Hostinger Tools")))
        val failure = fetchAndParse(
            notSupported(
                ApplicationPasswordsNotSupportedReason.ApplicationPasswordBlockedByMultiplePlugins,
                apiDetails,
            )
        )

        assertEquals("Hostinger Tools,Wordfence", failure.toDiscoveryFailure().props["plugin"])
    }

    @Test
    fun `http-only site`() {
        val failure = fetchAndParse(
            notSupported(ApplicationPasswordsNotSupportedReason.ApplicationPasswordsDisabledForHttpSite)
        )

        assertEquals(mapOf("reason" to "http_site"), failure.toDiscoveryFailure().props)
    }

    @Test
    fun `unsupported with no reason keeps the library message for the user`() {
        val failure = fetchAndParse(notSupported(reason = null)).toDiscoveryFailure()

        assertEquals(mapOf("reason" to "app_passwords_not_supported"), failure.props)
        assertEquals(FailureReason.Unknown, failure.userFacing)
    }

    @Test
    fun `no advertised authentication URL is the not-supported case named to the user`() {
        val failure = notSupportedFailure()

        assertEquals(mapOf("reason" to "no_auth_url_advertised"), failure.props)
        assertEquals(FailureReason.NotSupported, failure.userFacing)
    }

    @Test
    fun `a request the library reports as cancelled is not a failed login`() {
        val cancelled = fetchAndParse(
            FetchAndParseApiRootFailure.FetchApiRoot(
                requestFailed(RequestExecutionErrorReason.CancellationError, statusCode = null)
            )
        ).toDiscoveryFailure()
        val timedOut = fetchAndParse(
            FetchAndParseApiRootFailure.FetchApiRoot(
                requestFailed(RequestExecutionErrorReason.HttpTimeoutError, statusCode = null)
            )
        ).toDiscoveryFailure()

        assertEquals(true, cancelled.isCancellation)
        assertEquals("cancelled", cancelled.props["network_reason"])
        assertEquals(false, timedOut.isCancellation)
    }

    @Test
    fun `throwable reports its class only`() {
        assertEquals(
            mapOf("reason" to "exception", "error_code" to "IllegalStateException"),
            unexpectedDiscoveryFailure(IllegalStateException("contains a url")).props
        )
    }

    private fun fetchAndParse(failure: FetchAndParseApiRootFailure) =
        AutoDiscoveryAttemptFailure.FetchAndParseApiRoot(mock(), mock(), failure)

    private fun requestFailed(reason: RequestExecutionErrorReason, statusCode: UInt?) =
        RequestExecutionException.RequestExecutionFailed(
            statusCode = statusCode,
            redirects = null,
            reason = reason,
            requestUrl = "https://example.com",
            requestMethod = RequestMethod.GET,
        )

    private fun parseApiRoot(reason: ParseApiRootFailureReason?) =
        FetchAndParseApiRootFailure.ParseApiRoot(
            parsingErrorMessage = "",
            responseBody = "<html>secret</html>",
            responseBodyType = ResponseBodyType.MAYBE_HTML,
            reason = reason,
        )

    private fun wpError(code: String) =
        FetchAndParseApiRootFailure.WpError(WpErrorCode.CustomException(code), errorMessage = "", statusCode = 403u)

    private fun notSupported(
        reason: ApplicationPasswordsNotSupportedReason?,
        apiDetails: WpApiDetails = mock(),
    ) = FetchAndParseApiRootFailure.ApplicationPasswordsNotSupported(apiDetails, reason)

    private fun plugin(name: String) =
        KnownAuthenticationBlockingPlugin(name = name, namespace = "ns/v1", supportUrl = "https://example.com")
}
