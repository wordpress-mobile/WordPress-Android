package org.wordpress.android.ui.accounts.login

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper.DiscoverSuccessWrapper
import rs.wordpress.api.kotlin.ApiDiscoveryResult
import rs.wordpress.api.kotlin.WpLoginClient
import uniffi.wp_api.AutoDiscoveryAttemptSuccess
import uniffi.wp_api.DiscoveredAuthenticationMechanism
import uniffi.wp_api.ParseUrlException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertFailsWith

private const val SITE_URL = "https://example.test"
private const val DISCOVERED_API_ROOT = "https://example.test/custom-api/"
private const val LOCAL_ID = 1

@ExperimentalCoroutinesApi
class SiteApiRestUrlRecovererTest : BaseUnitTest() {
    @Mock lateinit var wpLoginClient: WpLoginClient
    @Mock lateinit var discoverSuccessWrapper: DiscoverSuccessWrapper
    @Mock lateinit var siteSqlUtils: SiteSqlUtils
    @Mock lateinit var appLogWrapper: AppLogWrapper

    private lateinit var recoverer: SiteApiRestUrlRecoverer

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        recoverer = SiteApiRestUrlRecoverer(
            wpLoginClient = wpLoginClient,
            discoverSuccessWrapper = discoverSuccessWrapper,
            siteSqlUtils = siteSqlUtils,
            appLogWrapper = appLogWrapper,
            bgDispatcher = testDispatcher(),
        )
    }

    private suspend fun stubDiscoverySuccess(apiRootUrl: String) {
        val result = ApiDiscoveryResult.Success(
            AutoDiscoveryAttemptSuccess(
                mock(), mock(), mock(),
                DiscoveredAuthenticationMechanism.ApplicationPasswords(mock())
            )
        )
        whenever(wpLoginClient.apiDiscovery(any())).thenReturn(result)
        whenever(discoverSuccessWrapper.getApiRootUrl(eq(result)))
            .thenReturn(apiRootUrl)
    }

    @Test
    fun `discoverApiRootUrl returns the discovered URL on success`() = runTest {
        stubDiscoverySuccess(DISCOVERED_API_ROOT)

        val result = recoverer.discoverApiRootUrl(SITE_URL)

        assertThat(result).isEqualTo(DISCOVERED_API_ROOT)
    }

    @Test
    fun `discoverApiRootUrl returns null when the discovered URL is blank`() = runTest {
        stubDiscoverySuccess(apiRootUrl = "")

        val result = recoverer.discoverApiRootUrl(SITE_URL)

        assertThat(result).isNull()
    }

    @Test
    fun `discoverApiRootUrl returns null when discovery returns a failure`() = runTest {
        whenever(wpLoginClient.apiDiscovery(any())).thenReturn(
            ApiDiscoveryResult.FailureParseSiteUrl(ParseUrlException.Generic(""))
        )

        val result = recoverer.discoverApiRootUrl(SITE_URL)

        assertThat(result).isNull()
    }

    @Test
    fun `discoverApiRootUrl swallows non-cancellation exceptions and returns null`() = runTest {
        whenever(wpLoginClient.apiDiscovery(any()))
            .doThrow(RuntimeException("network error"))

        val result = recoverer.discoverApiRootUrl(SITE_URL)

        assertThat(result).isNull()
    }

    @Test
    fun `discoverApiRootUrl rethrows CancellationException to preserve structured concurrency`() = runTest {
        whenever(wpLoginClient.apiDiscovery(any()))
            .doThrow(CancellationException("cancelled"))

        assertFailsWith<CancellationException> {
            recoverer.discoverApiRootUrl(SITE_URL)
        }
    }

    @Test
    fun `persistApiRootUrl returns true and writes the column when a row matches`() = runTest {
        whenever(siteSqlUtils.updateWpApiRestUrl(LOCAL_ID, DISCOVERED_API_ROOT)).thenReturn(1)

        val updated = recoverer.persistApiRootUrl(LOCAL_ID, DISCOVERED_API_ROOT)

        assertThat(updated).isTrue()
    }

    @Test
    fun `persistApiRootUrl returns false when no row matches the local id`() = runTest {
        whenever(siteSqlUtils.updateWpApiRestUrl(LOCAL_ID, DISCOVERED_API_ROOT)).thenReturn(0)

        val updated = recoverer.persistApiRootUrl(LOCAL_ID, DISCOVERED_API_ROOT)

        assertThat(updated).isFalse()
    }
}
