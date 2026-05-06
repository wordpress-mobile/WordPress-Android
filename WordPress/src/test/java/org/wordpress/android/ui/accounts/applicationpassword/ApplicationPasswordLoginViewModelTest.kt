package org.wordpress.android.ui.accounts.applicationpassword

import app.cash.turbine.test
import com.automattic.android.tracks.crashlogging.CrashLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper.StoreCredentialsResult
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    lateinit var appLogWrapper: AppLogWrapper

    @Mock
    lateinit var crashLogging: CrashLogging

    private lateinit var viewModel: ApplicationPasswordLoginViewModel

    private val rawData = "url=callback?site_url=https://example.com&user_login=user&password=pass"
    private val urlLogin = ApplicationPasswordLoginHelper.UriLogin(
        "https://example.com",
        "user",
        "pass",
        "https://example.com/json"
    )
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
            appLogWrapper,
            crashLogging
        )
        whenever(applicationPasswordLoginHelper.getSiteUrlLoginFromRawData(rawData)).thenReturn(urlLogin)
    }

    @Test
    fun `given empty rawData, when setup site, then emit error`() = runTest {
        // Given
        val emptyRawData = ""
        val expectedResult = ApplicationPasswordLoginViewModel.NavigationActionData(
            showSiteSelector = false,
            siteUrl = "",
            oldSitesIDs = null,
            isError = true,
            errorMessage = "empty_raw_data"
        )

        // When
        viewModel.onFinishedEvent.test {
            viewModel.setupSite(emptyRawData)

            // Then
            val finishedEvent = awaitItem()
            assertEquals(expectedResult, finishedEvent)
            verify(applicationPasswordLoginHelper, times(0))
                .storeApplicationPasswordCredentialsFrom(eq(urlLogin), any())
            verify(selfHostedEndpointFinder, times(0)).verifyOrDiscoverXMLRPCEndpoint(any())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given user rejected authorization, when setup site, then emit user_rejected without crash report`() =
        runTest {
            // Given
            val rejectionRawData = "wordpress://app-pass-authorize?success=false"
            whenever(applicationPasswordLoginHelper.isUserRejectedAuthorization(rejectionRawData))
                .thenReturn(true)
            val expectedResult = ApplicationPasswordLoginViewModel.NavigationActionData(
                showSiteSelector = false,
                siteUrl = null,
                oldSitesIDs = null,
                isError = true,
                errorMessage = "user_rejected"
            )

            // When
            viewModel.onFinishedEvent.test {
                viewModel.setupSite(rejectionRawData)

                // Then
                val finishedEvent = awaitItem()
                assertEquals(expectedResult, finishedEvent)
                verify(applicationPasswordLoginHelper)
                    .trackStoringFailed(eq(""), eq("user_rejected"), any())
                verify(applicationPasswordLoginHelper, times(0))
                    .getSiteUrlLoginFromRawData(any())
                verify(applicationPasswordLoginHelper, times(0))
                    .storeApplicationPasswordCredentialsFrom(any(), any())
                verify(crashLogging, times(0))
                    .sendReport(any(), any(), any())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given malformed rawData, when setup site and bad data, then emit bad_data error`() =
        runTest {
            // Given
            val malformedRawData = "malformed ray data"
            val emptyUriLogin =
                ApplicationPasswordLoginHelper.UriLogin("", "", "", "")
            val expectedResult =
                ApplicationPasswordLoginViewModel.NavigationActionData(
                    showSiteSelector = false,
                    siteUrl = "",
                    oldSitesIDs = null,
                    isError = true,
                    errorMessage = "bad_data"
                )
            whenever(
                applicationPasswordLoginHelper
                    .getSiteUrlLoginFromRawData(malformedRawData)
            ).thenReturn(emptyUriLogin)
            whenever(
                applicationPasswordLoginHelper
                    .storeApplicationPasswordCredentialsFrom(
                        eq(emptyUriLogin), any()
                    )
            ).thenReturn(StoreCredentialsResult.BadData)

            // When
            viewModel.onFinishedEvent.test {
                viewModel.setupSite(malformedRawData)

                // Then
                val finishedEvent = awaitItem()
                assertEquals(expectedResult, finishedEvent)
                verify(selfHostedEndpointFinder, times(0))
                    .verifyOrDiscoverXMLRPCEndpoint(any())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given intent rawData, when setup site and not able to store credentials and throw error fetching, then fetch them and emit error`() =
        runTest {
            // Given
            val expectedResult = ApplicationPasswordLoginViewModel.NavigationActionData(
                showSiteSelector = false,
                siteUrl = urlLogin.siteUrl,
                oldSitesIDs = null,
                isError = true,
                errorMessage = null
            )
            whenever(applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(eq(urlLogin), any()))
                .thenReturn(StoreCredentialsResult.SiteNotFound)
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
            whenever(
                applicationPasswordLoginHelper
                    .storeApplicationPasswordCredentialsFrom(eq(urlLogin), any())
            ).thenReturn(StoreCredentialsResult.SiteNotFound)
            whenever(
                selfHostedEndpointFinder
                    .verifyOrDiscoverXMLRPCEndpoint(urlLogin.siteUrl!!)
            ).thenReturn(xmlRpcEndpoint)

            // When
            viewModel.onFinishedEvent.test {
                viewModel.setupSite(rawData)
                // Mock onSiteChanged event
                viewModel.onSiteChanged(
                    SiteStore.OnSiteChanged(rowsAffected = 1)
                )

                // Then
                val finishedEvent = awaitItem()
                assertTrue(finishedEvent.isError)
                assertEquals(
                    "site_not_found", finishedEvent.errorMessage
                )
                verify(selfHostedEndpointFinder, times(1))
                    .verifyOrDiscoverXMLRPCEndpoint(urlLogin.siteUrl)
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
                siteUrl = urlLogin.siteUrl,
                oldSitesIDs = null,
                isError = false,
                newSiteLocalId = testSite.id
            )
            whenever(siteStore.hasSite()).thenReturn(true)
            whenever(siteStore.sites).thenReturn(listOf(testSite))
            whenever(applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(eq(urlLogin), any()))
                .thenReturn(StoreCredentialsResult.SiteNotFound)
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
                verify(selfHostedEndpointFinder, times(1)).verifyOrDiscoverXMLRPCEndpoint(urlLogin.siteUrl)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given intent rawData, when setup site and not able to store credentials but store fetch and no sites, then emit ok without site selector`() =
        runTest {
            // Given
            val xmlRpcEndpoint = "https://example.com/xmlrpc.php"
            val expectedResult = ApplicationPasswordLoginViewModel.NavigationActionData(
                showSiteSelector = false,
                siteUrl = urlLogin.siteUrl,
                oldSitesIDs = null,
                isError = false,
                newSiteLocalId = testSite.id
            )
            whenever(siteStore.hasSite()).thenReturn(false)
            whenever(siteStore.sites).thenReturn(listOf(testSite))
            whenever(applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(eq(urlLogin), any()))
                .thenReturn(StoreCredentialsResult.SiteNotFound)
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
                verify(selfHostedEndpointFinder, times(1)).verifyOrDiscoverXMLRPCEndpoint(urlLogin.siteUrl)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given intent rawData, when setup site and not able to store credentials but store fetch with sites, then emit ok with site selector`() =
        runTest {
            // Given
            val xmlRpcEndpoint = "https://example.com/xmlrpc.php"
            val expectedResult = ApplicationPasswordLoginViewModel.NavigationActionData(
                showSiteSelector = true,
                siteUrl = urlLogin.siteUrl,
                oldSitesIDs = null,
                isError = false,
                newSiteLocalId = testSite.id
            )
            whenever(siteStore.hasSite()).thenReturn(true)
            whenever(siteStore.sites).thenReturn(listOf(testSite))
            whenever(
                applicationPasswordLoginHelper
                    .storeApplicationPasswordCredentialsFrom(eq(urlLogin), any())
            ).thenReturn(StoreCredentialsResult.SiteNotFound)
            whenever(
                selfHostedEndpointFinder
                    .verifyOrDiscoverXMLRPCEndpoint(urlLogin.siteUrl!!)
            ).thenReturn(xmlRpcEndpoint)

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
                verify(selfHostedEndpointFinder, times(1))
                    .verifyOrDiscoverXMLRPCEndpoint(urlLogin.siteUrl)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given onSiteChanged with error, then emit error with SiteStore details`() =
        runTest {
            // Given
            setupFetchSitesFlow()
            val siteError = SiteStore.SiteError(
                SiteStore.SiteErrorType.GENERIC_ERROR, "encryption failed"
            )
            val errorEvent = SiteStore.OnSiteChanged(0, siteError)

            // When
            viewModel.onFinishedEvent.test {
                viewModel.setupSite(rawData)
                viewModel.onSiteChanged(errorEvent)

                // Then
                val result = awaitItem()
                assertTrue(result.isError)
                assertEquals("site_store_error", result.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given onSiteChanged with no rows affected, then emit error`() =
        runTest {
            // Given
            setupFetchSitesFlow()

            // When
            viewModel.onFinishedEvent.test {
                viewModel.setupSite(rawData)
                viewModel.onSiteChanged(
                    SiteStore.OnSiteChanged(rowsAffected = 0)
                )

                // Then
                val result = awaitItem()
                assertTrue(result.isError)
                assertEquals("no_rows_affected", result.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given onSiteChanged with bad credentials, then emit error`() =
        runTest {
            // Given
            setupFetchSitesFlow()
            whenever(siteStore.sites).thenReturn(listOf(testSite))
            whenever(
                applicationPasswordLoginHelper.siteHasBadCredentials(any())
            ).thenReturn(true)

            // When
            viewModel.onFinishedEvent.test {
                viewModel.setupSite(rawData)
                viewModel.onSiteChanged(
                    SiteStore.OnSiteChanged(
                        rowsAffected = 1,
                        updatedSites = listOf(testSite)
                    )
                )

                // Then
                val result = awaitItem()
                assertTrue(result.isError)
                assertEquals("empty_credentials", result.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given onSiteChanged with DB exception, then emit error`() =
        runTest {
            // Given
            setupFetchSitesFlow()
            whenever(siteStore.sites)
                .thenThrow(RuntimeException("DB corrupted"))

            // When
            viewModel.onFinishedEvent.test {
                viewModel.setupSite(rawData)
                viewModel.onSiteChanged(
                    SiteStore.OnSiteChanged(rowsAffected = 1)
                )

                // Then
                val result = awaitItem()
                assertTrue(result.isError)
                assertEquals("db_read_exception", result.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given error emitted, then crash report is sent`() = runTest {
        // Given & When
        viewModel.onFinishedEvent.test {
            viewModel.setupSite("")

            // Then
            awaitItem()
            verify(crashLogging).sendReport(
                exception = any(),
                tags = eq(mapOf("tag" to "MAIN")),
                message = eq(null)
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given existing site, when store credentials succeeds, then emit success without site selector`() =
        runTest {
            // Given
            val expectedResult =
                ApplicationPasswordLoginViewModel.NavigationActionData(
                    showSiteSelector = false,
                    siteUrl = urlLogin.siteUrl,
                    oldSitesIDs = null,
                    isError = false,
                )
            whenever(
                applicationPasswordLoginHelper
                    .storeApplicationPasswordCredentialsFrom(
                        eq(urlLogin), any()
                    )
            ).thenReturn(StoreCredentialsResult.Success)

            // When
            viewModel.onFinishedEvent.test {
                viewModel.setupSite(rawData)

                // Then
                val finishedEvent = awaitItem()
                assertEquals(expectedResult, finishedEvent)
                verify(selfHostedEndpointFinder, times(0))
                    .verifyOrDiscoverXMLRPCEndpoint(any())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given existing site, when store credentials succeeds, then onSiteChanged is ignored`() =
        runTest {
            // Given
            whenever(
                applicationPasswordLoginHelper
                    .storeApplicationPasswordCredentialsFrom(
                        eq(urlLogin), any()
                    )
            ).thenReturn(StoreCredentialsResult.Success)

            // When
            viewModel.onFinishedEvent.test {
                viewModel.setupSite(rawData)
                val successEvent = awaitItem()
                assertFalse(successEvent.isError)

                // Simulate onSiteChanged from updateApplicationPassword
                viewModel.onSiteChanged(
                    SiteStore.OnSiteChanged(
                        rowsAffected = 1,
                        updatedSites = listOf(testSite)
                    )
                )

                // No second event should be emitted
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given DiscoveryException, when fetchSites, then dispatch WPAPI fallback`() =
        runTest {
            // Given
            whenever(
                applicationPasswordLoginHelper
                    .storeApplicationPasswordCredentialsFrom(
                        eq(urlLogin), any()
                    )
            ).thenReturn(StoreCredentialsResult.SiteNotFound)
            whenever(
                selfHostedEndpointFinder
                    .verifyOrDiscoverXMLRPCEndpoint(any())
            ).thenThrow(
                mock<SelfHostedEndpointFinder.DiscoveryException>()
            )

            // When
            viewModel.onFinishedEvent.test {
                viewModel.setupSite(rawData)

                // Then - no error emitted, WPAPI action dispatched
                expectNoEvents()
                verify(dispatcher, times(1)).dispatch(
                    argThat {
                        type == SiteAction.FETCH_SITE_WP_API_FROM_APPLICATION_PASSWORD
                    }
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    private suspend fun setupFetchSitesFlow() {
        val xmlRpcEndpoint = "https://example.com/xmlrpc.php"
        whenever(
            applicationPasswordLoginHelper
                .storeApplicationPasswordCredentialsFrom(eq(urlLogin), any())
        ).thenReturn(StoreCredentialsResult.SiteNotFound)
        whenever(
            selfHostedEndpointFinder
                .verifyOrDiscoverXMLRPCEndpoint(urlLogin.siteUrl!!)
        ).thenReturn(xmlRpcEndpoint)
    }
}
