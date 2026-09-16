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

class DiscoveryFailureAnalyticsTest {
    @Test
    fun `unparseable site url`() {
        val failure = AutoDiscoveryAttemptFailure.ParseSiteUrl(ParseUrlException.EmptyHost())

        assertEquals(mapOf("reason" to "invalid_url", "error_code" to "EmptyHost"), failure.toAnalyticsProps())
    }

    @Test
    fun `not a WordPress site`() {
        val failure = AutoDiscoveryAttemptFailure.FindApiRoot(mock(), FindApiRootFailure.ProbablyNotAWordPressSite)

        assertEquals(mapOf("reason" to "not_a_wordpress_site"), failure.toAnalyticsProps())
    }

    @Test
    fun `network failure carries the reason and status code`() {
        val failure = fetchAndParse(
            FetchAndParseApiRootFailure.FetchApiRoot(
                RequestExecutionException.RequestExecutionFailed(
                    statusCode = 503u,
                    redirects = null,
                    reason = RequestExecutionErrorReason.HttpTimeoutError,
                    requestUrl = "https://example.com/wp-json",
                    requestMethod = RequestMethod.GET,
                )
            )
        )

        assertEquals(
            mapOf("reason" to "network_error", "network_reason" to "http_timeout", "status_code" to "503"),
            failure.toAnalyticsProps()
        )
    }

    @Test
    fun `network failure without a status code omits it`() {
        val failure = AutoDiscoveryAttemptFailure.FindApiRoot(
            mock(),
            FindApiRootFailure.FetchHomepage(
                RequestExecutionException.RequestExecutionFailed(
                    statusCode = null,
                    redirects = null,
                    reason = RequestExecutionErrorReason.DeviceIsOfflineError("offline"),
                    requestUrl = "https://example.com",
                    requestMethod = RequestMethod.GET,
                )
            )
        )

        assertEquals(
            mapOf("reason" to "network_error", "network_reason" to "device_offline"),
            failure.toAnalyticsProps()
        )
    }

    @Test
    fun `Wordfence blocking the API root is a plugin block`() {
        val failure = fetchAndParse(
            FetchAndParseApiRootFailure.ParseApiRoot(
                parsingErrorMessage = "",
                responseBody = "<html>blocked</html>",
                responseBodyType = ResponseBodyType.MAYBE_HTML,
                reason = ParseApiRootFailureReason.WORDFENCE_BLOCKING_ACCESS,
            )
        )

        assertEquals(mapOf("reason" to "blocked_by_plugin", "plugin" to "Wordfence"), failure.toAnalyticsProps())
    }

    @Test
    fun `unrecognised API root response reports its body type, not its body`() {
        val failure = fetchAndParse(
            FetchAndParseApiRootFailure.ParseApiRoot(
                parsingErrorMessage = "",
                responseBody = "<html>secret</html>",
                responseBodyType = ResponseBodyType.MAYBE_HTML,
                reason = null,
            )
        )

        assertEquals(
            mapOf("reason" to "invalid_api_root_response", "response_body_type" to "maybe_html"),
            failure.toAnalyticsProps()
        )
    }

    @Test
    fun `REST error envelope carries the raw custom code and status`() {
        val failure = fetchAndParse(
            FetchAndParseApiRootFailure.WpError(
                errorCode = WpErrorCode.CustomException("private_site"),
                errorMessage = "This site is private",
                statusCode = 403u,
            )
        )

        assertEquals(
            mapOf("reason" to "wp_error", "error_code" to "private_site", "status_code" to "403"),
            failure.toAnalyticsProps()
        )
    }

    @Test
    fun `single blocking plugin is named`() {
        val failure = fetchAndParse(
            FetchAndParseApiRootFailure.ApplicationPasswordsNotSupported(
                apiDetails = mock(),
                reason = ApplicationPasswordsNotSupportedReason.ApplicationPasswordBlockedByPlugin(plugin("Wordfence")),
            )
        )

        assertEquals(mapOf("reason" to "blocked_by_plugin", "plugin" to "Wordfence"), failure.toAnalyticsProps())
    }

    @Test
    fun `http-only site`() {
        val failure = fetchAndParse(
            FetchAndParseApiRootFailure.ApplicationPasswordsNotSupported(
                apiDetails = mock(),
                reason = ApplicationPasswordsNotSupportedReason.ApplicationPasswordsDisabledForHttpSite,
            )
        )

        assertEquals(mapOf("reason" to "http_site"), failure.toAnalyticsProps())
    }

    @Test
    fun `unsupported with no reason`() {
        val failure = fetchAndParse(
            FetchAndParseApiRootFailure.ApplicationPasswordsNotSupported(apiDetails = mock(), reason = null)
        )

        assertEquals(mapOf("reason" to "app_passwords_not_supported"), failure.toAnalyticsProps())
    }

    @Test
    fun `REST error code that is not a slug is not shipped`() {
        val failure = fetchAndParse(
            FetchAndParseApiRootFailure.WpError(
                errorCode = WpErrorCode.CustomException("blocked: client 203.0.113.7 <script>"),
                errorMessage = "",
                statusCode = 403u,
            )
        )

        assertEquals(
            mapOf("reason" to "wp_error", "error_code" to "custom", "status_code" to "403"),
            failure.toAnalyticsProps()
        )
    }

    @Test
    fun `multiple blocking plugins are sorted so order does not split the bucket`() {
        val apiDetails = mock<WpApiDetails>()
        whenever(apiDetails.applicationPasswordBlockingPlugins())
            .thenReturn(listOf(plugin("Wordfence"), plugin("Hostinger Tools")))
        val failure = fetchAndParse(
            FetchAndParseApiRootFailure.ApplicationPasswordsNotSupported(
                apiDetails = apiDetails,
                reason = ApplicationPasswordsNotSupportedReason.ApplicationPasswordBlockedByMultiplePlugins,
            )
        )

        assertEquals("Hostinger Tools,Wordfence", failure.toAnalyticsProps()["plugin"])
    }

    @Test
    fun `throwable reports its class only`() {
        assertEquals(
            mapOf("reason" to "exception", "error_code" to "IllegalStateException"),
            IllegalStateException("contains a url").toAnalyticsProps()
        )
    }

    private fun fetchAndParse(failure: FetchAndParseApiRootFailure) =
        AutoDiscoveryAttemptFailure.FetchAndParseApiRoot(mock(), mock(), failure)

    private fun plugin(name: String) =
        KnownAuthenticationBlockingPlugin(name = name, namespace = "ns/v1", supportUrl = "https://example.com")
}
