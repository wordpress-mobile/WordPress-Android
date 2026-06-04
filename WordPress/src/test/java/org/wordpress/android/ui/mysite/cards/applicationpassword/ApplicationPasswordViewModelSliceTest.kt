package org.wordpress.android.ui.mysite.cards.applicationpassword

import junit.framework.TestCase.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.SitesModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordCredentials
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnApplicationPasswordCreated
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.accounts.login.CredentialsChangedNotifier
import org.wordpress.android.ui.accounts.login.SiteApiRestUrlRecoverer
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import kotlin.test.assertNotNull

private const val TEST_URL = "https://www.test.com"
private const val TEST_SITE_NAME = "My Site"
private const val TEST_SITE_ID = 1
private const val TEST_SITE_ICON = "http://site.com/icon.jpg"
private const val TEST_URL_AUTH = "https://www.test.com/auth"
private const val TEST_URL_AUTH_SUFFIX = "?app_name=android-jetpack-client&success_url=callback://callback"

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ApplicationPasswordViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var applicationPasswordLoginHelper: ApplicationPasswordLoginHelper

    @Mock
    lateinit var siteStore: SiteStore

    @Mock
    lateinit var appLogWrapper: AppLogWrapper

    @Mock
    lateinit var wpApiClientProvider: WpApiClientProvider

    @Mock
    lateinit var applicationPasswordValidator: ApplicationPasswordValidator

    @Mock
    lateinit var selfHostedEndpointFinder: SelfHostedEndpointFinder

    @Mock
    lateinit var siteXMLRPCClient: SiteXMLRPCClient

    @Mock
    lateinit var siteApiRestUrlRecoverer: SiteApiRestUrlRecoverer

    @Mock
    lateinit var dispatcher: Dispatcher

    @Mock
    lateinit var credentialsChangedNotifier: CredentialsChangedNotifier

    private lateinit var siteTest: SiteModel

    private var applicationPasswordCard: MySiteCardAndItem? = null

    private lateinit var applicationPasswordViewModelSlice: ApplicationPasswordViewModelSlice

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        applicationPasswordViewModelSlice = ApplicationPasswordViewModelSlice(
            applicationPasswordLoginHelper,
            siteStore,
            appLogWrapper,
            wpApiClientProvider,
            applicationPasswordValidator,
            selfHostedEndpointFinder,
            siteXMLRPCClient,
            siteApiRestUrlRecoverer,
            dispatcher,
            credentialsChangedNotifier,
            testDispatcher()
        ).apply {
            initialize(testScope())
        }
        siteTest = SiteModel().apply {
            id = TEST_SITE_ID
            url = TEST_URL
            name = TEST_SITE_NAME
            iconUrl = TEST_SITE_ICON
            siteId = TEST_SITE_ID.toLong()
            apiRestUsernamePlain = "testuser"
            apiRestPasswordPlain = "testpass"
            // Mark xmlRpcUrl so handleValidAuth's XML-RPC-disabled fallback doesn't fire — that
            // path is exercised by the dedicated xmlRpcRediscovery tests below.
            xmlRpcUrl = "https://www.test.com/xmlrpc.php"
        }

        applicationPasswordCard = null
        applicationPasswordViewModelSlice.uiModel.observeForever { card ->
            applicationPasswordCard = card
        }

        // By default, treat the site as having no stored credentials. Tests that exercise the
        // validate-stored-creds path override this.
        whenever(applicationPasswordLoginHelper.siteHasBadCredentials(any())).thenReturn(true)
    }

    private suspend fun stubMintFailure(notSupported: Boolean = false) {
        whenever(siteStore.createApplicationPassword(any())).thenReturn(
            OnApplicationPasswordCreated(
                siteTest,
                BaseNetworkError(GenericErrorType.UNKNOWN, "fail"),
                notSupported = notSupported,
            )
        )
    }

    private suspend fun stubMintSuccess() {
        whenever(siteStore.createApplicationPassword(any())).thenReturn(
            OnApplicationPasswordCreated(
                siteTest,
                ApplicationPasswordCredentials("user", "pass", uuid = "u")
            )
        )
    }

    @Test
    fun `given proper site, when api discovery is success, then add the application password card`() = runTest {
        stubMintFailure()
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(eq(TEST_URL)))
            .thenReturn(
                ApplicationPasswordLoginHelper.DiscoveryResult.Authorized("$TEST_URL_AUTH$TEST_URL_AUTH_SUFFIX")
            )

        applicationPasswordViewModelSlice.buildCard(siteTest)

        assertNotNull(applicationPasswordCard)
        verify(applicationPasswordLoginHelper).getAuthorizationUrlComplete(eq(TEST_URL))
    }

    @Test
    fun `given login scenario, when api discovery is empty, then show no card`() = runTest {
        stubMintFailure()
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(eq(TEST_URL)))
            .thenReturn(ApplicationPasswordLoginHelper.DiscoveryResult.Failed("test discovery failure"))

        applicationPasswordViewModelSlice.buildCard(siteTest)

        assertNull(applicationPasswordCard)
        verify(applicationPasswordLoginHelper).getAuthorizationUrlComplete(eq(TEST_URL))
    }

    @Test
    fun `given headless mint succeeds, then hide card and skip discovery`() = runTest {
        stubMintSuccess()

        applicationPasswordViewModelSlice.buildCard(siteTest)

        assertNull(applicationPasswordCard)
        verify(siteStore).createApplicationPassword(any())
        verify(applicationPasswordLoginHelper, never()).getAuthorizationUrlComplete(any())
    }

    @Test
    fun `given headless mint succeeds, then notify credentials changed`() = runTest {
        stubMintSuccess()

        applicationPasswordViewModelSlice.buildCard(siteTest)

        verify(credentialsChangedNotifier).notifyChanged(TEST_SITE_ID)
    }

    @Test
    fun `given headless mint succeeds, card hides without waiting for the recoverer`() = runTest {
        stubMintSuccess()
        val recoverGate = CompletableDeferred<Unit>()
        whenever(siteApiRestUrlRecoverer.discoverApiRootUrl(any()))
            .doSuspendableAnswer { recoverGate.await(); null }

        applicationPasswordViewModelSlice.buildCard(siteTest)

        // Card has been hidden even though the recoverer is still suspended on the gate.
        assertNull(applicationPasswordCard)
        verify(siteApiRestUrlRecoverer).discoverApiRootUrl(siteTest.url)

        // Release the recoverer so the test scope doesn't carry a dangling coroutine.
        recoverGate.complete(Unit)
    }

    @Test
    fun `given valid stored creds, card hides without waiting for the recoverer`() = runTest {
        whenever(applicationPasswordLoginHelper.siteHasBadCredentials(any())).thenReturn(false)
        whenever(siteStore.sites).thenReturn(
            listOf(
                SiteModel().apply {
                    id = siteTest.id
                    url = TEST_URL
                    apiRestUsernamePlain = "user"
                    apiRestPasswordPlain = "password"
                    xmlRpcUrl = siteTest.xmlRpcUrl
                }
            )
        )
        whenever(applicationPasswordValidator.validate(any()))
            .thenReturn(ApplicationPasswordValidator.Outcome.Valid)
        val recoverGate = CompletableDeferred<Unit>()
        whenever(siteApiRestUrlRecoverer.discoverApiRootUrl(any()))
            .doSuspendableAnswer { recoverGate.await(); null }

        applicationPasswordViewModelSlice.buildCard(siteTest)

        assertNull(applicationPasswordCard)
        verify(siteApiRestUrlRecoverer).discoverApiRootUrl(TEST_URL)

        recoverGate.complete(Unit)
    }

    @Test
    fun `given headless mint returns NotSupported, then fall back to discovery`() = runTest {
        stubMintFailure(notSupported = true)
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(eq(TEST_URL)))
            .thenReturn(
                ApplicationPasswordLoginHelper.DiscoveryResult.Authorized("$TEST_URL_AUTH$TEST_URL_AUTH_SUFFIX")
            )

        applicationPasswordViewModelSlice.buildCard(siteTest)

        assertNotNull(applicationPasswordCard)
        verify(siteStore).createApplicationPassword(any())
        verify(applicationPasswordLoginHelper).getAuthorizationUrlComplete(eq(TEST_URL))
        verify(credentialsChangedNotifier, never()).notifyChanged(any())
    }

    @Test
    fun `given site already authenticated and validation succeeds, then show no card`() = runTest {
        whenever(applicationPasswordLoginHelper.siteHasBadCredentials(any())).thenReturn(false)
        whenever(siteStore.sites).thenReturn(
            listOf(
                SiteModel().apply {
                    id = siteTest.id
                    url = TEST_URL
                    apiRestUsernamePlain = "user"
                    apiRestPasswordPlain = "password"
                    xmlRpcUrl = siteTest.xmlRpcUrl
                }
            )
        )
        whenever(applicationPasswordValidator.validate(any()))
            .thenReturn(ApplicationPasswordValidator.Outcome.Valid)

        applicationPasswordViewModelSlice.buildCard(siteTest)

        assertNull(applicationPasswordCard)
        verify(applicationPasswordValidator).validate(any())
        verify(siteStore, never()).createApplicationPassword(any())
        verify(applicationPasswordLoginHelper, times(0)).getAuthorizationUrlComplete(any())
    }

    @Test
    fun `given stored creds invalid, clear them and try headless mint`() = runTest {
        whenever(applicationPasswordLoginHelper.siteHasBadCredentials(any())).thenReturn(false)
        whenever(siteStore.sites).thenReturn(
            listOf(
                SiteModel().apply {
                    id = siteTest.id
                    url = TEST_URL
                    apiRestUsernamePlain = "stale-user"
                    apiRestPasswordPlain = "stale-pass"
                    xmlRpcUrl = siteTest.xmlRpcUrl
                }
            )
        )
        whenever(applicationPasswordValidator.validate(any()))
            .thenReturn(ApplicationPasswordValidator.Outcome.Invalid)
        stubMintSuccess()

        applicationPasswordViewModelSlice.buildCard(siteTest)

        assertNull(applicationPasswordCard)
        verify(siteStore).deleteStoredApplicationPasswordCredentials(any())
        // clearSelfHostedClient is invoked twice — once on invalidation, once after the fresh mint
        verify(wpApiClientProvider, times(2)).clearSelfHostedClient(siteTest.id)
        verify(siteStore).createApplicationPassword(any())
    }

    @Test
    fun `given stored creds invalid and mint fails, show reauthentication banner`() = runTest {
        whenever(applicationPasswordLoginHelper.siteHasBadCredentials(any())).thenReturn(false)
        whenever(siteStore.sites).thenReturn(
            listOf(
                SiteModel().apply {
                    id = siteTest.id
                    url = TEST_URL
                    apiRestUsernamePlain = "stale-user"
                    apiRestPasswordPlain = "stale-pass"
                    xmlRpcUrl = siteTest.xmlRpcUrl
                }
            )
        )
        whenever(applicationPasswordValidator.validate(any()))
            .thenReturn(ApplicationPasswordValidator.Outcome.Invalid)
        stubMintFailure(notSupported = true)
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(eq(TEST_URL)))
            .thenReturn(
                ApplicationPasswordLoginHelper.DiscoveryResult.Authorized("$TEST_URL_AUTH$TEST_URL_AUTH_SUFFIX")
            )

        applicationPasswordViewModelSlice.buildCard(siteTest)

        assertNotNull(applicationPasswordCard)
        // Reauth banner uses SingleActionCard (not the QuickLinksItem create card)
        assert(applicationPasswordCard is MySiteCardAndItem.Item.SingleActionCard)
    }

    @Test
    fun `given validation hits a network error, leave the card hidden without re-minting`() = runTest {
        whenever(applicationPasswordLoginHelper.siteHasBadCredentials(any())).thenReturn(false)
        whenever(siteStore.sites).thenReturn(
            listOf(
                SiteModel().apply {
                    id = siteTest.id
                    url = TEST_URL
                    apiRestUsernamePlain = "user"
                    apiRestPasswordPlain = "pass"
                    xmlRpcUrl = siteTest.xmlRpcUrl
                }
            )
        )
        whenever(applicationPasswordValidator.validate(any()))
            .thenReturn(ApplicationPasswordValidator.Outcome.NetworkUnavailable)

        applicationPasswordViewModelSlice.buildCard(siteTest)

        assertNull(applicationPasswordCard)
        verify(siteStore, never()).createApplicationPassword(any())
        verify(siteStore, never()).deleteStoredApplicationPasswordCredentials(any())
    }

    @Test
    fun `concurrent buildCard calls coalesce to a single mint`() = runTest {
        // Gate the mint so the first buildCard suspends mid-call and the second arrives while it's
        // still in flight. Without the single-flight guard, both calls would issue separate
        // server-side mints and race the 409 conflict handler in ApplicationPasswordsManager.
        val mintGate = CompletableDeferred<Unit>()
        whenever(siteStore.createApplicationPassword(any())).doSuspendableAnswer {
            mintGate.await()
            OnApplicationPasswordCreated(
                siteTest,
                ApplicationPasswordCredentials("user", "pass", uuid = "u")
            )
        }

        applicationPasswordViewModelSlice.buildCard(siteTest)
        applicationPasswordViewModelSlice.buildCard(siteTest)

        mintGate.complete(Unit)
        advanceUntilIdle()

        verify(siteStore, times(1)).createApplicationPassword(any())
    }

    @Test
    fun `given xmlRpc rediscovery and auth check succeed, then update site and dispatch`() =
        runTest {
            val xmlRpcUrl = "https://www.test.com/xmlrpc.php"
            whenever(
                selfHostedEndpointFinder
                    .verifyOrDiscoverXMLRPCEndpoint(TEST_URL)
            ).thenReturn(xmlRpcUrl)
            whenever(
                siteXMLRPCClient.fetchSites(
                    eq(xmlRpcUrl), any(), any()
                )
            ).thenReturn(SitesModel(listOf(SiteModel())))

            applicationPasswordViewModelSlice
                .attemptXmlRpcRediscovery(siteTest)

            verify(dispatcher).dispatch(any())
            assert(siteTest.xmlRpcUrl == xmlRpcUrl)
        }

    @Test
    fun `given xmlRpc rediscovery succeeds but auth check fails, then do not dispatch`() =
        runTest {
            siteTest.xmlRpcUrl = null
            val xmlRpcUrl = "https://www.test.com/xmlrpc.php"
            whenever(
                selfHostedEndpointFinder
                    .verifyOrDiscoverXMLRPCEndpoint(TEST_URL)
            ).thenReturn(xmlRpcUrl)
            val errorResult = SitesModel().apply {
                error = mock()
            }
            whenever(
                siteXMLRPCClient.fetchSites(
                    eq(xmlRpcUrl), any(), any()
                )
            ).thenReturn(errorResult)

            applicationPasswordViewModelSlice
                .attemptXmlRpcRediscovery(siteTest)

            verify(dispatcher, never()).dispatch(any())
            assert(siteTest.xmlRpcUrl.isNullOrEmpty())
        }

    @Test
    fun `given xmlRpc rediscovery fails, then do not dispatch`() =
        runTest {
            siteTest.xmlRpcUrl = null
            whenever(
                selfHostedEndpointFinder
                    .verifyOrDiscoverXMLRPCEndpoint(TEST_URL)
            ).thenThrow(
                mock<SelfHostedEndpointFinder.DiscoveryException>()
            )

            applicationPasswordViewModelSlice
                .attemptXmlRpcRediscovery(siteTest)

            verify(selfHostedEndpointFinder)
                .verifyOrDiscoverXMLRPCEndpoint(TEST_URL)
            verify(dispatcher, never()).dispatch(any())
            assert(siteTest.xmlRpcUrl.isNullOrEmpty())
        }
}
