package org.wordpress.android.ui.accounts.login

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.SitesModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.utils.AppLogWrapper

private const val ENDPOINT = "https://selfhosted.example.com/xmlrpc.php"
private const val SITE_LOCAL_ID = 5

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteXmlRpcUrlRecovererTest : BaseUnitTest() {
    @Mock lateinit var selfHostedEndpointFinder: SelfHostedEndpointFinder
    @Mock lateinit var siteXMLRPCClient: SiteXMLRPCClient
    @Mock lateinit var siteSqlUtils: SiteSqlUtils
    @Mock lateinit var appLogWrapper: AppLogWrapper

    private lateinit var site: SiteModel
    private lateinit var recoverer: SiteXmlRpcUrlRecoverer

    @Before
    fun setUp() {
        site = SiteModel().apply {
            id = SITE_LOCAL_ID
            url = "https://selfhosted.example.com"
            apiRestUsernamePlain = "user"
            apiRestPasswordPlain = "pass"
        }
        recoverer = SiteXmlRpcUrlRecoverer(
            selfHostedEndpointFinder,
            siteXMLRPCClient,
            siteSqlUtils,
            appLogWrapper,
            testDispatcher(),
        )
    }

    @Test
    fun `given discovery and authenticated verify succeed, then returns the endpoint`() = test {
        whenever(selfHostedEndpointFinder.verifyOrDiscoverXMLRPCEndpoint(site.url)).thenReturn(ENDPOINT)
        // The site's stored credentials are forwarded to the authenticated verify call.
        whenever(siteXMLRPCClient.fetchSites(eq(ENDPOINT), eq("user"), eq("pass")))
            .thenReturn(SitesModel(listOf(SiteModel())))

        assertThat(recoverer.discoverAndVerifyXmlRpcUrl(site)).isEqualTo(ENDPOINT)
    }

    @Test
    fun `given discovery throws, then returns null`() = test {
        whenever(selfHostedEndpointFinder.verifyOrDiscoverXMLRPCEndpoint(site.url))
            .thenThrow(mock<SelfHostedEndpointFinder.DiscoveryException>())

        assertThat(recoverer.discoverAndVerifyXmlRpcUrl(site)).isNull()
    }

    @Test
    fun `given discovery throws an unexpected exception, then returns null`() = test {
        // A non-DiscoveryException (e.g. a RuntimeException from the network/parse path) must be
        // contained, not propagated — otherwise it cancels the whole provisioning pipeline.
        whenever(selfHostedEndpointFinder.verifyOrDiscoverXMLRPCEndpoint(site.url))
            .thenThrow(RuntimeException("unexpected"))

        assertThat(recoverer.discoverAndVerifyXmlRpcUrl(site)).isNull()
    }

    @Test
    fun `given the authenticated verify errors, then returns null`() = test {
        whenever(selfHostedEndpointFinder.verifyOrDiscoverXMLRPCEndpoint(site.url)).thenReturn(ENDPOINT)
        whenever(siteXMLRPCClient.fetchSites(eq(ENDPOINT), any(), any()))
            .thenReturn(SitesModel().apply { error = BaseNetworkError(GenericErrorType.UNKNOWN, "x") })

        assertThat(recoverer.discoverAndVerifyXmlRpcUrl(site)).isNull()
    }

    @Test
    fun `given persist updates a row, then returns true`() = test {
        whenever(siteSqlUtils.updateXmlRpcUrl(eq(SITE_LOCAL_ID), eq(ENDPOINT))).thenReturn(1)

        assertThat(recoverer.persistXmlRpcUrl(SITE_LOCAL_ID, ENDPOINT)).isTrue
        verify(siteSqlUtils).updateXmlRpcUrl(eq(SITE_LOCAL_ID), eq(ENDPOINT))
    }

    @Test
    fun `given persist matches no row, then returns false`() = test {
        whenever(siteSqlUtils.updateXmlRpcUrl(eq(SITE_LOCAL_ID), eq(ENDPOINT))).thenReturn(0)

        assertThat(recoverer.persistXmlRpcUrl(SITE_LOCAL_ID, ENDPOINT)).isFalse
    }
}
