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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
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

    private fun siteWithoutUrl(): SiteModel = SiteModel().apply {
        id = LOCAL_ID
        url = SITE_URL
        wpApiRestUrl = null
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
    fun `recoverAndPersist populates wpApiRestUrl in memory and writes the DB row`() = runTest {
        val site = siteWithoutUrl()
        val siteFromDB = siteWithoutUrl()
        stubDiscoverySuccess(DISCOVERED_API_ROOT)
        whenever(siteSqlUtils.getSitesWithLocalId(LOCAL_ID)).thenReturn(listOf(siteFromDB))

        recoverer.recoverAndPersistIfMissing(site)

        assertThat(site.wpApiRestUrl).isEqualTo(DISCOVERED_API_ROOT)
        assertThat(siteFromDB.wpApiRestUrl).isEqualTo(DISCOVERED_API_ROOT)
        verify(siteSqlUtils).insertOrUpdateSite(siteFromDB)
    }

    @Test
    fun `recoverAndPersist skips discovery when wpApiRestUrl is already populated`() = runTest {
        val site = siteWithoutUrl().apply { wpApiRestUrl = "https://example.test/wp-json/" }

        recoverer.recoverAndPersistIfMissing(site)

        verify(wpLoginClient, never()).apiDiscovery(any())
        verify(siteSqlUtils, never()).insertOrUpdateSite(any())
    }

    @Test
    fun `recoverAndPersist does nothing when discovery returns a blank apiRootUrl`() = runTest {
        val site = siteWithoutUrl()
        stubDiscoverySuccess(apiRootUrl = "")

        recoverer.recoverAndPersistIfMissing(site)

        assertThat(site.wpApiRestUrl).isNull()
        verify(siteSqlUtils, never()).insertOrUpdateSite(any())
    }

    @Test
    fun `recoverAndPersist does nothing when discovery returns a failure`() = runTest {
        val site = siteWithoutUrl()
        whenever(wpLoginClient.apiDiscovery(any())).thenReturn(
            ApiDiscoveryResult.FailureParseSiteUrl(ParseUrlException.Generic(""))
        )

        recoverer.recoverAndPersistIfMissing(site)

        assertThat(site.wpApiRestUrl).isNull()
        verify(siteSqlUtils, never()).insertOrUpdateSite(any())
    }

    @Test
    fun `recoverAndPersist swallows non-cancellation exceptions thrown by discovery`() = runTest {
        val site = siteWithoutUrl()
        whenever(wpLoginClient.apiDiscovery(any()))
            .doThrow(RuntimeException("network error"))

        recoverer.recoverAndPersistIfMissing(site)

        assertThat(site.wpApiRestUrl).isNull()
        verify(siteSqlUtils, never()).insertOrUpdateSite(any())
    }

    @Test
    fun `recoverAndPersist rethrows CancellationException to preserve structured concurrency`() = runTest {
        val site = siteWithoutUrl()
        whenever(wpLoginClient.apiDiscovery(any()))
            .doThrow(CancellationException("cancelled"))

        assertFailsWith<CancellationException> {
            recoverer.recoverAndPersistIfMissing(site)
        }
        verify(siteSqlUtils, never()).insertOrUpdateSite(any())
    }

    @Test
    fun `recoverAndPersist skips the DB write when the site is not in the DB`() = runTest {
        val site = siteWithoutUrl()
        stubDiscoverySuccess(DISCOVERED_API_ROOT)
        whenever(siteSqlUtils.getSitesWithLocalId(LOCAL_ID)).thenReturn(emptyList())

        recoverer.recoverAndPersistIfMissing(site)

        assertThat(site.wpApiRestUrl).isEqualTo(DISCOVERED_API_ROOT)
        verify(siteSqlUtils, never()).insertOrUpdateSite(any())
    }

    @Test
    fun `discoverInMemory populates wpApiRestUrl when missing but never touches the DB`() = runTest {
        val site = siteWithoutUrl()
        stubDiscoverySuccess(DISCOVERED_API_ROOT)

        recoverer.discoverInMemoryIfMissing(site)

        assertThat(site.wpApiRestUrl).isEqualTo(DISCOVERED_API_ROOT)
        verify(siteSqlUtils, never()).getSitesWithLocalId(any())
        verify(siteSqlUtils, never()).insertOrUpdateSite(any())
    }

    @Test
    fun `discoverInMemory skips discovery when wpApiRestUrl is already populated`() = runTest {
        val site = siteWithoutUrl().apply { wpApiRestUrl = "https://example.test/wp-json/" }

        recoverer.discoverInMemoryIfMissing(site)

        verify(wpLoginClient, never()).apiDiscovery(any())
    }

    @Test
    fun `discoverInMemory does nothing when discovery fails`() = runTest {
        val site = siteWithoutUrl()
        whenever(wpLoginClient.apiDiscovery(any())).thenReturn(
            ApiDiscoveryResult.FailureParseSiteUrl(ParseUrlException.Generic(""))
        )

        recoverer.discoverInMemoryIfMissing(site)

        assertThat(site.wpApiRestUrl).isNull()
    }

    @Test
    fun `discoverInMemory swallows non-cancellation exceptions thrown by discovery`() = runTest {
        val site = siteWithoutUrl()
        whenever(wpLoginClient.apiDiscovery(any()))
            .doThrow(RuntimeException("network error"))

        recoverer.discoverInMemoryIfMissing(site)

        assertThat(site.wpApiRestUrl).isNull()
    }

    @Test
    fun `discoverInMemory rethrows CancellationException to preserve structured concurrency`() = runTest {
        val site = siteWithoutUrl()
        whenever(wpLoginClient.apiDiscovery(any()))
            .doThrow(CancellationException("cancelled"))

        assertFailsWith<CancellationException> {
            recoverer.discoverInMemoryIfMissing(site)
        }
    }
}
