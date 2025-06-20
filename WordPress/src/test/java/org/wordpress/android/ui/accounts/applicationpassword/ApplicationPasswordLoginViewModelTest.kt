package org.wordpress.android.ui.accounts.applicationpassword

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder
import org.wordpress.android.fluxc.store.SiteStore
import org.mockito.kotlin.any
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class ApplicationPasswordLoginViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var applicationPasswordLoginHelper: ApplicationPasswordLoginHelper
    @Mock
    lateinit var selfHostedEndpointFinder: SelfHostedEndpointFinder
    @Mock
    lateinit var siteStore: SiteStore

    private lateinit var viewModel: ApplicationPasswordLoginViewModel

    private val rawData = "url=callback?site_url=https://example.com&user_login=user&password=pass"
    private val uriLogin = ApplicationPasswordLoginHelper.UriLogin("https://example.com", "user", "pass")

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = ApplicationPasswordLoginViewModel(
            testDispatcher(),
            applicationPasswordLoginHelper,
            selfHostedEndpointFinder,
            siteStore
        )
        whenever(applicationPasswordLoginHelper.getSiteUrlLoginFromRawData(rawData)).thenReturn(uriLogin)
    }

    @Test
    fun `valid rawData stores credentials and emits siteUrl`() = runTest {
        // Given
        whenever(applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(rawData)).thenReturn(true)

        // When
        viewModel.onFinishedEvent.test {
            viewModel.setupSite(rawData)

            // Then
            val finishedEvent = awaitItem()
            assertEquals(uriLogin.siteUrl, finishedEvent, "onFinishedEvent should emit the siteUrl")
            verify(applicationPasswordLoginHelper, times(1))
                .storeApplicationPasswordCredentialsFrom(rawData)
            verify(selfHostedEndpointFinder, times(0)).verifyOrDiscoverXMLRPCEndpoint(any())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty rawData does not store credentials nor site and emits null`()= runTest {
        // Given
        val emptyRawData = ""

        // When
        viewModel.onFinishedEvent.test {
            viewModel.setupSite(emptyRawData)

            // Then
            val finishedEvent = awaitItem()
            assertEquals(finishedEvent, null, "onFinishedEvent should emit null")
            verify(applicationPasswordLoginHelper, times(0))
                .storeApplicationPasswordCredentialsFrom(rawData)
            verify(selfHostedEndpointFinder, times(0)).verifyOrDiscoverXMLRPCEndpoint(any())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when credentials not stored, fetchSites succeeds, emits siteUrl`() = runTest {
        // Given
        whenever(applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(rawData)).thenReturn(false)
        whenever(selfHostedEndpointFinder.verifyOrDiscoverXMLRPCEndpoint(uriLogin.siteUrl!!))
            .thenReturn("https://example.com/xmlrpc.php")

        // When
        viewModel.onFinishedEvent.test {
            viewModel.setupSite(rawData)

            // Then
            val finishedEvent = awaitItem()
            assertEquals(uriLogin.siteUrl, finishedEvent, "onFinishedEvent should emit the siteUrl")
            verify(selfHostedEndpointFinder, times(1)).verifyOrDiscoverXMLRPCEndpoint(uriLogin.siteUrl!!)
            verify(siteStore, times(1)).onAction(any())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when credentials not stored due to exception, fetchSites succeeds, emits siteUrl`() = runTest {
        // Given
        whenever(applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(rawData))
            .thenThrow(RuntimeException())
        whenever(selfHostedEndpointFinder.verifyOrDiscoverXMLRPCEndpoint(uriLogin.siteUrl!!))
            .thenReturn("https://example.com/xmlrpc.php")

        // When
        viewModel.onFinishedEvent.test {
            viewModel.setupSite(rawData)

            // Then
            val finishedEvent = awaitItem()
            assertEquals(uriLogin.siteUrl, finishedEvent, "onFinishedEvent should emit the siteUrl")
            verify(selfHostedEndpointFinder, times(1)).verifyOrDiscoverXMLRPCEndpoint(uriLogin.siteUrl!!)
            verify(siteStore, times(1)).onAction(any())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when credentials not stored, fetchSites fails, emits null`() = runTest {
        // Given
        whenever(applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(rawData))
            .thenReturn(false)
        whenever(selfHostedEndpointFinder.verifyOrDiscoverXMLRPCEndpoint(uriLogin.siteUrl!!))
            .thenThrow(RuntimeException())

        // When
        viewModel.onFinishedEvent.test {
            viewModel.setupSite(rawData)

            // Then
            val finishedEvent = awaitItem()
            assertEquals(null, finishedEvent, "onFinishedEvent should emit null on fetchSites failure")
            verify(selfHostedEndpointFinder, times(1)).verifyOrDiscoverXMLRPCEndpoint(uriLogin.siteUrl!!)
            verify(siteStore, times(0)).onAction(any())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when credentials not stored, fetchSites with missing user or password emits null`() = runTest {
        // Given
        val malformedRawData = "malformed ray data"

        // When
        viewModel.onFinishedEvent.test {
            viewModel.setupSite(malformedRawData)

            // Then
            val finishedEvent = awaitItem()
            assertEquals(null, finishedEvent, "onFinishedEvent should emit null if user or password is missing")
            verify(selfHostedEndpointFinder, times(0)).verifyOrDiscoverXMLRPCEndpoint(any())
            verify(siteStore, times(0)).onAction(any())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
