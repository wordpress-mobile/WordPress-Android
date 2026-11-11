package org.wordpress.android.ui.mysite.cards.applicationpassword

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.utils.AppLogWrapper
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.RequestExecutionErrorReason
import uniffi.wp_api.RequestMethod
import uniffi.wp_api.WpErrorCode

@ExperimentalCoroutinesApi
class ApplicationPasswordValidatorTest : BaseUnitTest() {
    @Mock
    lateinit var wpApiClientProvider: WpApiClientProvider

    @Mock
    lateinit var appLogWrapper: AppLogWrapper

    @Mock
    lateinit var wpApiClient: WpApiClient

    private lateinit var validator: ApplicationPasswordValidator
    private lateinit var site: SiteModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        validator = ApplicationPasswordValidator(wpApiClientProvider, appLogWrapper)
        site = SiteModel().apply {
            id = 1
            url = "https://example.com"
            apiRestUsernamePlain = "user"
            apiRestPasswordPlain = "pass"
        }
        whenever(wpApiClientProvider.getApplicationPasswordClient(site)).thenReturn(wpApiClient)
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun stubResponse(response: WpRequestResult<*>) {
        whenever(wpApiClient.request<Any>(any())).thenReturn(response as WpRequestResult<Any>)
    }

    // Default to a non-auth status so WpErrorCode-only tests isolate the code path from the
    // status path. Tests of the status path (401/403) pass an explicit value.
    private fun wpError(code: WpErrorCode, statusCode: Int = 500) = WpRequestResult.WpError<Any>(
        errorCode = code,
        errorMessage = "msg",
        statusCode = statusCode.toUInt(),
        response = "",
        requestUrl = "https://example.com",
        requestMethod = RequestMethod.GET,
    )

    private fun requestFailed(reason: RequestExecutionErrorReason) =
        WpRequestResult.RequestExecutionFailed<Any>(
            statusCode = null,
            redirects = null,
            reason = reason,
            requestUrl = "https://example.com",
            requestMethod = RequestMethod.GET,
        )

    // --- Success ---

    @Test
    fun `Success maps to Valid`() = runTest {
        stubResponse(WpRequestResult.Success<Any>(response = Any()))
        assertThat(validator.validate(site)).isEqualTo(ApplicationPasswordValidator.Outcome.Valid)
    }

    // --- WpError: auth-related codes map to Invalid ---

    @Test
    fun `WpError Unauthorized maps to Invalid`() = runTest {
        stubResponse(wpError(WpErrorCode.Unauthorized()))
        assertThat(validator.validate(site)).isEqualTo(ApplicationPasswordValidator.Outcome.Invalid)
    }

    @Test
    fun `WpError Forbidden maps to Invalid`() = runTest {
        stubResponse(wpError(WpErrorCode.Forbidden()))
        assertThat(validator.validate(site)).isEqualTo(ApplicationPasswordValidator.Outcome.Invalid)
    }

    @Test
    fun `WpError ApplicationPasswordNotFound maps to Invalid`() = runTest {
        stubResponse(wpError(WpErrorCode.ApplicationPasswordNotFound()))
        assertThat(validator.validate(site)).isEqualTo(ApplicationPasswordValidator.Outcome.Invalid)
    }

    @Test
    fun `WpError NoAuthenticatedAppPassword maps to Invalid`() = runTest {
        stubResponse(wpError(WpErrorCode.NoAuthenticatedAppPassword()))
        assertThat(validator.validate(site)).isEqualTo(ApplicationPasswordValidator.Outcome.Invalid)
    }

    // --- WpError: non-auth codes must NOT wipe creds ---

    @Test
    fun `WpError non-auth code with non-auth status maps to NetworkUnavailable`() = runTest {
        // Non-401/403 status with an unrelated WpErrorCode: ambiguous, don't wipe creds.
        stubResponse(wpError(WpErrorCode.InvalidParam(), statusCode = 400))
        assertThat(validator.validate(site))
            .isEqualTo(ApplicationPasswordValidator.Outcome.NetworkUnavailable)
    }

    @Test
    fun `WpError with 401 status maps to Invalid regardless of code`() = runTest {
        // WordPress emits a wide range of WpErrorCodes for credential rejections (e.g.
        // `incorrect_password`, `invalid_username`, plugin-defined codes). Many fall through
        // to wordpress-rs's untagged-string fallback and aren't recognized as auth codes by
        // name. The status code is the reliable signal — a parseable WpError with 401/403 is
        // always an auth rejection regardless of which WpErrorCode it carries.
        stubResponse(wpError(WpErrorCode.InvalidParam(), statusCode = 401))
        assertThat(validator.validate(site)).isEqualTo(ApplicationPasswordValidator.Outcome.Invalid)
    }

    @Test
    fun `WpError with 403 status maps to Invalid regardless of code`() = runTest {
        stubResponse(wpError(WpErrorCode.InvalidParam(), statusCode = 403))
        assertThat(validator.validate(site)).isEqualTo(ApplicationPasswordValidator.Outcome.Invalid)
    }

    @Test
    fun `WpError with 500 status maps to NetworkUnavailable`() = runTest {
        // A server returning a structured WpError on 5xx is unusual but possible. Without an
        // auth-status signal and without an auth code, treat it as transient.
        stubResponse(wpError(WpErrorCode.InvalidParam(), statusCode = 500))
        assertThat(validator.validate(site))
            .isEqualTo(ApplicationPasswordValidator.Outcome.NetworkUnavailable)
    }

    // --- RequestExecutionFailed: auth-related reasons map to Invalid ---

    @Test
    fun `RequestExecutionFailed HttpAuthenticationRejectedError maps to Invalid`() = runTest {
        stubResponse(
            requestFailed(
                RequestExecutionErrorReason.HttpAuthenticationRejectedError(
                    hostname = "example.com",
                    method = null,
                )
            )
        )
        assertThat(validator.validate(site)).isEqualTo(ApplicationPasswordValidator.Outcome.Invalid)
    }

    @Test
    fun `RequestExecutionFailed HttpAuthenticationRequiredError maps to Invalid`() = runTest {
        stubResponse(
            requestFailed(
                RequestExecutionErrorReason.HttpAuthenticationRequiredError(
                    hostname = "example.com",
                    method = null,
                )
            )
        )
        assertThat(validator.validate(site)).isEqualTo(ApplicationPasswordValidator.Outcome.Invalid)
    }

    @Test
    fun `RequestExecutionFailed HttpForbiddenError maps to Invalid`() = runTest {
        stubResponse(
            requestFailed(RequestExecutionErrorReason.HttpForbiddenError(hostname = "example.com"))
        )
        assertThat(validator.validate(site)).isEqualTo(ApplicationPasswordValidator.Outcome.Invalid)
    }

    // --- RequestExecutionFailed: non-auth reasons must NOT wipe creds ---

    @Test
    fun `RequestExecutionFailed HttpTimeoutError maps to NetworkUnavailable`() = runTest {
        stubResponse(requestFailed(RequestExecutionErrorReason.HttpTimeoutError))
        assertThat(validator.validate(site))
            .isEqualTo(ApplicationPasswordValidator.Outcome.NetworkUnavailable)
    }

    @Test
    fun `RequestExecutionFailed DeviceIsOfflineError maps to NetworkUnavailable`() = runTest {
        stubResponse(
            requestFailed(RequestExecutionErrorReason.DeviceIsOfflineError(errorMessage = "off"))
        )
        assertThat(validator.validate(site))
            .isEqualTo(ApplicationPasswordValidator.Outcome.NetworkUnavailable)
    }

    @Test
    fun `RequestExecutionFailed NonExistentSiteError maps to NetworkUnavailable`() = runTest {
        stubResponse(
            requestFailed(
                RequestExecutionErrorReason.NonExistentSiteError(
                    errorMessage = null,
                    suggestedAction = null,
                )
            )
        )
        assertThat(validator.validate(site))
            .isEqualTo(ApplicationPasswordValidator.Outcome.NetworkUnavailable)
    }

    @Test
    fun `RequestExecutionFailed HttpError maps to NetworkUnavailable`() = runTest {
        stubResponse(requestFailed(RequestExecutionErrorReason.HttpError(reason = "boom")))
        assertThat(validator.validate(site))
            .isEqualTo(ApplicationPasswordValidator.Outcome.NetworkUnavailable)
    }

    // --- Other variants: all default to NetworkUnavailable ---

    @Test
    fun `UnknownError (5xx without parseable JSON) maps to NetworkUnavailable`() = runTest {
        stubResponse(
            WpRequestResult.UnknownError<Any>(
                statusCode = 503.toUInt(),
                response = "<html>Service Unavailable</html>",
                requestUrl = "https://example.com",
                requestMethod = RequestMethod.GET,
            )
        )
        assertThat(validator.validate(site))
            .isEqualTo(ApplicationPasswordValidator.Outcome.NetworkUnavailable)
    }

    @Test
    fun `InvalidHttpStatusCode maps to NetworkUnavailable`() = runTest {
        stubResponse(
            WpRequestResult.InvalidHttpStatusCode<Any>(
                statusCode = 999.toUInt(),
                requestUrl = "https://example.com",
                requestMethod = RequestMethod.GET,
            )
        )
        assertThat(validator.validate(site))
            .isEqualTo(ApplicationPasswordValidator.Outcome.NetworkUnavailable)
    }

    @Test
    fun `ResponseParsingError maps to NetworkUnavailable`() = runTest {
        stubResponse(
            WpRequestResult.ResponseParsingError<Any>(
                reason = "bad json",
                response = "not json",
                requestUrl = "https://example.com",
                requestMethod = RequestMethod.GET,
            )
        )
        assertThat(validator.validate(site))
            .isEqualTo(ApplicationPasswordValidator.Outcome.NetworkUnavailable)
    }

    // --- Exception handling ---

    @Test
    fun `generic Exception maps to NetworkUnavailable`() = runTest {
        whenever(wpApiClient.request<Any>(any())).thenThrow(RuntimeException("boom"))
        assertThat(validator.validate(site))
            .isEqualTo(ApplicationPasswordValidator.Outcome.NetworkUnavailable)
    }

    @Test(expected = CancellationException::class)
    fun `CancellationException is rethrown, not swallowed`() = runTest {
        whenever(wpApiClient.request<Any>(any())).thenThrow(CancellationException("cancelled"))
        validator.validate(site)
    }
}
