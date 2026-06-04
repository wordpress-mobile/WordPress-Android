package org.wordpress.android.ui.mysite.cards.applicationpassword

import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.repositories.SiteAuthState
import org.wordpress.android.repositories.SiteProvisioningSource
import org.wordpress.android.repositories.SiteReadiness
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItem

private const val TEST_URL = "https://www.test.com"
private const val TEST_SITE_ID = 1
private const val TEST_AUTH_URL = "https://www.test.com/auth"

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ApplicationPasswordViewModelSliceTest : BaseUnitTest() {
    @Mock lateinit var applicationPasswordLoginHelper: ApplicationPasswordLoginHelper
    @Mock lateinit var siteStore: SiteStore
    @Mock lateinit var appLogWrapper: AppLogWrapper
    @Mock lateinit var selfHostedEndpointFinder: SelfHostedEndpointFinder
    @Mock lateinit var siteXMLRPCClient: SiteXMLRPCClient
    @Mock lateinit var siteProvisioningSource: SiteProvisioningSource
    @Mock lateinit var dispatcher: Dispatcher

    private lateinit var siteTest: SiteModel
    private var card: MySiteCardAndItem? = null
    private lateinit var slice: ApplicationPasswordViewModelSlice

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        slice = ApplicationPasswordViewModelSlice(
            applicationPasswordLoginHelper,
            siteStore,
            appLogWrapper,
            selfHostedEndpointFinder,
            siteXMLRPCClient,
            siteProvisioningSource,
            dispatcher,
            testDispatcher(),
        ).apply { initialize(testScope()) }
        siteTest = SiteModel().apply {
            id = TEST_SITE_ID
            url = TEST_URL
            // A WP.com-REST site by default, so a provisioned site hides the card (no XML-RPC path).
            setIsWPCom(true)
        }
        card = null
        slice.uiModel.observeForever { card = it }
    }

    private fun stubReadiness(readiness: SiteReadiness): MutableStateFlow<SiteReadiness> {
        val flow = MutableStateFlow(readiness)
        whenever(siteProvisioningSource.stateFor(siteTest)).thenReturn(flow)
        return flow
    }

    private suspend fun stubAuthorized() =
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(eq(TEST_URL)))
            .thenReturn(ApplicationPasswordLoginHelper.DiscoveryResult.Authorized(TEST_AUTH_URL))

    @Test
    fun `given unprovisionable without prior creds, then show the create card`() = test {
        stubReadiness(SiteReadiness.NeedsAuth(SiteAuthState.Unprovisionable(hadCredentials = false)))
        stubAuthorized()

        slice.buildCard(siteTest)

        assertThat(card).isInstanceOf(MySiteCardAndItem.Card.QuickLinksItem::class.java)
    }

    @Test
    fun `given unprovisionable with prior creds, then show the reauthentication card`() = test {
        stubReadiness(SiteReadiness.NeedsAuth(SiteAuthState.Unprovisionable(hadCredentials = true)))
        stubAuthorized()

        slice.buildCard(siteTest)

        val banner = card as MySiteCardAndItem.Item.SingleActionCard
        assertThat(banner.textResource).isEqualTo(R.string.application_password_reauthentication_banner)
    }

    @Test
    fun `given unprovisionable but discovery fails, then no card`() = test {
        stubReadiness(SiteReadiness.NeedsAuth(SiteAuthState.Unprovisionable(hadCredentials = false)))
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(eq(TEST_URL)))
            .thenReturn(ApplicationPasswordLoginHelper.DiscoveryResult.Failed("bad discovery"))

        slice.buildCard(siteTest)

        assertNull(card)
    }

    @Test
    fun `given provisioning, then no card`() = test {
        stubReadiness(SiteReadiness.NeedsAuth(SiteAuthState.Provisioning))

        slice.buildCard(siteTest)

        assertNull(card)
    }

    @Test
    fun `given ready on a WPCom-REST site, then no card`() = test {
        stubReadiness(SiteReadiness.Ready)

        slice.buildCard(siteTest)

        assertNull(card)
        verify(applicationPasswordLoginHelper, never()).getAuthorizationUrlComplete(any())
    }

    @Test
    fun `given ready on a self-hosted site missing XML-RPC, then show the XML-RPC disabled card`() = test {
        siteTest = SiteModel().apply {
            id = TEST_SITE_ID
            url = TEST_URL
            // Not WP.com-REST and no XML-RPC endpoint — the one case the card still surfaces.
        }
        stubReadiness(SiteReadiness.Ready)
        // Let rediscovery fail so the card stays put for the assertion.
        whenever(selfHostedEndpointFinder.verifyOrDiscoverXMLRPCEndpoint(TEST_URL))
            .thenThrow(mock<SelfHostedEndpointFinder.DiscoveryException>())

        slice.buildCard(siteTest)

        val xmlRpcCard = card as MySiteCardAndItem.Item.SingleActionCard
        assertThat(xmlRpcCard.textResource).isEqualTo(R.string.xmlrpc_disabled_card_text)
    }
}
