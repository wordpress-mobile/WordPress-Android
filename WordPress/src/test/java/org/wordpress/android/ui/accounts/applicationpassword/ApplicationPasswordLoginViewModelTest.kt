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
import org.mockito.kotlin.eq
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@Suppress("MaxLineLength")
class ApplicationPasswordLoginViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var dispatcher: Dispatcher

    @Mock
    lateinit var applicationPasswordLoginHelper: ApplicationPasswordLoginHelper

    @Mock
    lateinit var selfHostedEndpointFinder: SelfHostedEndpointFinder

    @Mock
    lateinit var siteStore: SiteStore

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var appLogWrapper: AppLogWrapper

    private lateinit var viewModel: ApplicationPasswordLoginViewModel

    private val rawData = "url=callback?site_url=https://example.com&user_login=user&password=pass"
    private val urlLogin = ApplicationPasswordLoginHelper.UriLogin("https://example.com", "user", "pass", "https://example.com/json")
    private val testSite = SiteModel().apply {
        apiRestUsernamePlain = urlLogin.user
        apiRestPasswordPlain = urlLogin.password
        url = urlLogin.siteUrl
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = ApplicationPasswordLoginViewModel(
            testDispatcher(),
            dispatcher,
            applicationPasswordLoginHelper,
            selfHostedEndpointFinder,
            siteStore,
            appPrefsWrapper,
            appLogWrapper
        )
        whenever(applicationPasswordLoginHelper.getSiteUrlLoginFromRawData(rawData)).thenReturn(urlLogin)
    }

    @Test
    fun `given empty rawData, when setup site, then emit error`() = runTest {
        // Given
        val emptyRawData = ""
        val expectedResult = ApplicationPasswordLoginViewModel.NavigationActionData(
            showSiteSelector = false,
            showPostSignupInterstitial = false,
            siteUrl = "",
            oldSitesIDs = null,
            isError = true
        )

        // When
        viewModel.onFinishedEvent.test {
            viewModel.setupSite(emptyRawData)

            // Then
            val finishedEvent = awaitItem()
            assertEquals(expectedResult, finishedEvent)
            verify(applicationPasswordLoginHelper, times(0))
                .storeApplicationPasswordCredentialsFrom(eq(urlLogin))
            verify(selfHostedEndpointFinder, times(0)).verifyOrDiscoverXMLRPCEndpoint(any())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given intent rawData, when setup site and not able to store credentials and data is empty, then fetch them and emit error`() =
        runTest {
            // Given
            val malformedRawData = "malformed ray data"
            val expectedResult = ApplicationPasswordLoginViewModel.NavigationActionData(
                showSiteSelector = false,
                showPostSignupInterstitial = false,
                siteUrl = "",
                oldSitesIDs = null,
                isError = true
            )
            whenever(applicationPasswordLoginHelper.getSiteUrlLoginFromRawData(malformedRawData))
                .thenReturn(
                    ApplicationPasswordLoginHelper.UriLogin("", "", "", "")
                )

            // When
            viewModel.onFinishedEvent.test {
                viewModel.setupSite(malformedRawData)

                // Then
                val finishedEvent = awaitItem()
                assertEquals(expectedResult, finishedEvent)
                verify(selfHostedEndpointFinder, times(0)).verifyOrDiscoverXMLRPCEndpoint(any())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given intent rawData, when setup site and not able to store credentials and throw error fetching, then fetch them and emit error`() =
        runTest {
            // Given
            val expectedResult = ApplicationPasswordLoginViewModel.NavigationActionData(
                showSiteSelector = false,
                showPostSignupInterstitial = false,
                siteUrl = urlLogin.siteUrl,
                oldSitesIDs = null,
                isError = true
            )
            whenever(applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(eq(urlLogin))).thenReturn(false)
            whenever(selfHostedEndpointFinder.verifyOrDiscoverXMLRPCEndpoint(any())).thenThrow(RuntimeException())

            // When
            viewModel.onFinishedEvent.test {
                viewModel.setupSite(rawData)

                // Then
                val finishedEvent = awaitItem()
                assertEquals(expectedResult, finishedEvent)
                verify(selfHostedEndpointFinder, times(1)).verifyOrDiscoverXMLRPCEndpoint(eq(urlLogin.siteUrl!!))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given intent rawData, when setup site and not able to store credentials nor store fetch, then emit error`() =
        runTest {
            // Given
            val xmlRpcEndpoint = "https://example.com/xmlrpc.php"
            val expectedResult = ApplicationPasswordLoginViewModel.NavigationActionData(
                showSiteSelector = false,
                showPostSignupInterstitial = false,
                siteUrl = urlLogin.siteUrl,
                oldSitesIDs = null,
                isError = true
            )
            whenever(applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(eq(urlLogin))).thenReturn(false)
            whenever(selfHostedEndpointFinder.verifyOrDiscoverXMLRPCEndpoint(urlLogin.siteUrl!!))
                .thenReturn(xmlRpcEndpoint)

            // When
            viewModel.onFinishedEvent.test {
                viewModel.setupSite(rawData)
                // Mock onSiteChanged event
                viewModel.onSiteChanged(
                    SiteStore.OnSiteChanged(
                        rowsAffected = 1,
                    )
                )

                // Then
                val finishedEvent = awaitItem()
                assertEquals(expectedResult, finishedEvent)
                verify(selfHostedEndpointFinder, times(1)).verifyOrDiscoverXMLRPCEndpoint(urlLogin.siteUrl!!)
                verify(siteStore, times(1)).sites
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given intent rawData, when setup site and not able to store credentials but store fetch, then emit ok with site selector`() =
        runTest {
            // Given
            val xmlRpcEndpoint = "https://example.com/xmlrpc.php"
            val expectedResult = ApplicationPasswordLoginViewModel.NavigationActionData(
                showSiteSelector = true,
                showPostSignupInterstitial = false,
                siteUrl = urlLogin.siteUrl,
                oldSitesIDs = null,
                isError = false,
                newSiteLocalId = testSite.id
            )
            whenever(siteStore.hasSite()).thenReturn(true)
            whenever(siteStore.sites).thenReturn(listOf(testSite))
            whenever(applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(eq(urlLogin))).thenReturn(false)
            whenever(selfHostedEndpointFinder.verifyOrDiscoverXMLRPCEndpoint(urlLogin.siteUrl!!))
                .thenReturn(xmlRpcEndpoint)

            // When
            viewModel.onFinishedEvent.test {
                viewModel.setupSite(rawData)
                // Mock onSiteChanged event
                viewModel.onSiteChanged(
                    SiteStore.OnSiteChanged(
                        rowsAffected = 1,
                        updatedSites = listOf(SiteModel())
                    )
                )

                // Then
                val finishedEvent = awaitItem()
                assertEquals(expectedResult, finishedEvent)
                verify(selfHostedEndpointFinder, times(1)).verifyOrDiscoverXMLRPCEndpoint(urlLogin.siteUrl!!)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given intent rawData, when setup site and not able to store credentials but store fetch, then emit ok with no site selector nor interstitial`() =
        runTest {
            // Given
            val xmlRpcEndpoint = "https://example.com/xmlrpc.php"
            val expectedResult = ApplicationPasswordLoginViewModel.NavigationActionData(
                showSiteSelector = false,
                showPostSignupInterstitial = false,
                siteUrl = urlLogin.siteUrl,
                oldSitesIDs = null,
                isError = false,
                newSiteLocalId = testSite.id
            )
            whenever(siteStore.hasSite()).thenReturn(false)
            whenever(siteStore.sites).thenReturn(listOf(testSite))
            whenever(applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(eq(urlLogin))).thenReturn(false)
            whenever(selfHostedEndpointFinder.verifyOrDiscoverXMLRPCEndpoint(urlLogin.siteUrl!!))
                .thenReturn(xmlRpcEndpoint)

            // When
            viewModel.onFinishedEvent.test {
                viewModel.setupSite(rawData)
                // Mock onSiteChanged event
                viewModel.onSiteChanged(
                    SiteStore.OnSiteChanged(
                        rowsAffected = 1,
                        updatedSites = listOf(testSite)
                    )
                )

                // Then
                val finishedEvent = awaitItem()
                assertEquals(expectedResult, finishedEvent)
                verify(selfHostedEndpointFinder, times(1)).verifyOrDiscoverXMLRPCEndpoint(urlLogin.siteUrl!!)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given intent rawData, when setup site and not able to store credentials but store fetch, then emit ok with no interstitial by sites`() =
        runTest {
            // Given
            val xmlRpcEndpoint = "https://example.com/xmlrpc.php"
            val expectedResult = ApplicationPasswordLoginViewModel.NavigationActionData(
                showSiteSelector = true,
                showPostSignupInterstitial = false,
                siteUrl = urlLogin.siteUrl,
                oldSitesIDs = null,
                isError = false,
                newSiteLocalId = testSite.id
            )
            whenever(siteStore.hasSite()).thenReturn(true)
            whenever(siteStore.sites).thenReturn(listOf(testSite))
            whenever(applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(eq(urlLogin))).thenReturn(false)
            whenever(selfHostedEndpointFinder.verifyOrDiscoverXMLRPCEndpoint(urlLogin.siteUrl!!))
                .thenReturn(xmlRpcEndpoint)

            // When
            viewModel.onFinishedEvent.test {
                viewModel.setupSite(rawData)
                // Mock onSiteChanged event
                viewModel.onSiteChanged(
                    SiteStore.OnSiteChanged(
                        rowsAffected = 1,
                        updatedSites = listOf(SiteModel())
                    )
                )

                // Then
                val finishedEvent = awaitItem()
                assertEquals(expectedResult, finishedEvent)
                verify(selfHostedEndpointFinder, times(1)).verifyOrDiscoverXMLRPCEndpoint(urlLogin.siteUrl!!)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given intent rawData, when setup site and not able to store credentials but store fetch, then emit ok with no interstitial by preferences`() =
        runTest {
            // Given
            val xmlRpcEndpoint = "https://example.com/xmlrpc.php"
            val expectedResult = ApplicationPasswordLoginViewModel.NavigationActionData(
                showSiteSelector = false,
                showPostSignupInterstitial = false,
                siteUrl = urlLogin.siteUrl,
                oldSitesIDs = null,
                isError = false,
                newSiteLocalId = testSite.id
            )
            whenever(siteStore.sites).thenReturn(listOf(testSite))
            whenever(appPrefsWrapper.shouldShowPostSignupInterstitial).thenReturn(false)
            whenever(applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(eq(urlLogin))).thenReturn(false)
            whenever(selfHostedEndpointFinder.verifyOrDiscoverXMLRPCEndpoint(urlLogin.siteUrl!!))
                .thenReturn(xmlRpcEndpoint)

            // When
            viewModel.onFinishedEvent.test {
                viewModel.setupSite(rawData)
                // Mock onSiteChanged event
                viewModel.onSiteChanged(
                    SiteStore.OnSiteChanged(
                        rowsAffected = 1,
                        updatedSites = listOf(testSite)
                    )
                )

                // Then
                val finishedEvent = awaitItem()
                assertEquals(expectedResult, finishedEvent)
                verify(selfHostedEndpointFinder, times(1)).verifyOrDiscoverXMLRPCEndpoint(urlLogin.siteUrl!!)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
