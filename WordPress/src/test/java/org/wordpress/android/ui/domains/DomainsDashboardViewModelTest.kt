package org.wordpress.android.ui.domains

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R.string
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.Domain
import org.wordpress.android.fluxc.network.rest.wpcom.site.GoogleAppsSubscription
import org.wordpress.android.fluxc.network.rest.wpcom.site.TitanMailSubscription
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.FetchedDomainsPayload
import org.wordpress.android.test
import org.wordpress.android.ui.domains.DomainsDashboardItem.AddDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.PrimaryDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.PurchaseDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.SiteDomains
import org.wordpress.android.ui.domains.DomainsDashboardItem.SiteDomainsHeader
import org.wordpress.android.ui.mysite.cards.domainregistration.DomainRegistrationHandler
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

class DomainsDashboardViewModelTest : BaseUnitTest() {
    @Mock private lateinit var siteStore: SiteStore
    @Mock private lateinit var analyticsTracker: AnalyticsTrackerWrapper
    @Mock private lateinit var domainRegistrationHandler: DomainRegistrationHandler
    @Mock private lateinit var htmlMessageUtils: HtmlMessageUtils

    private lateinit var viewModel: DomainsDashboardViewModel
    private var site: SiteModel = SiteModel()
    private var siteUrl: String = ""
    private var domains: List<Domain> = listOf()
    private var hasDomainCredit: Boolean = false

    private val siteId = 1234L
    private val testDomainName = "testdomain.blog"
    private val navigationActions = mutableListOf<DomainsDashboardNavigationAction>()
    private val uiModel = mutableListOf<DomainsDashboardItem>()

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = DomainsDashboardViewModel(
                siteStore,
                analyticsTracker,
                domainRegistrationHandler,
                htmlMessageUtils,
                TEST_DISPATCHER
        )

        site.siteId = siteId
        site.url = testDomainName

        siteUrl = site.url

        viewModel.onNavigation.observeForever { it.applyIfNotHandled { navigationActions.add(this) } }

        uiModel.clear()
        viewModel.uiModel.observeForever { if (it != null) uiModel += it }
    }

    @Test
    fun `free plan with custom domain shows manage your domains`() = test {
        addCustomDomain()
        whenever(siteStore.fetchSiteDomains(site)).thenReturn(FetchedDomainsPayload(site, domains))

        viewModel.start(site)

        val dashboardItems = uiModel

        assertThat(dashboardItems[0]).isInstanceOf(PrimaryDomain::class.java)
        assertThat(dashboardItems[1]).isInstanceOf(PrimaryDomain::class.java)
        assertThat(dashboardItems[2]).isInstanceOf(SiteDomainsHeader::class.java)
        assertThat(dashboardItems[3]).isInstanceOf(SiteDomains::class.java)
    }

    @Test
    fun `free plan with no custom domain shows get your domain card`() = test {
        clearCustomDomain()
        whenever(siteStore.fetchSiteDomains(site)).thenReturn(FetchedDomainsPayload(site, domains))
        whenever(htmlMessageUtils.getHtmlMessageFromStringFormatResId(any(), any())).thenReturn("")

        viewModel.start(site)

        val dashboardItems = uiModel

        assertThat(dashboardItems[0]).isInstanceOf(PrimaryDomain::class.java)
        assertThat(dashboardItems[1]).isInstanceOf(PrimaryDomain::class.java)
        assertThat(dashboardItems[2]).isInstanceOf(PurchaseDomain::class.java)
        assertThat((dashboardItems[2] as PurchaseDomain).title)
                .isEqualTo(UiStringRes(string.domains_free_plan_get_your_domain_title))
    }

    @Test
    fun `paid plan with primary custom domain and credits shows manage your domains`() = test {
        addCustomDomain()
        hasDomainCredit = true
        whenever(siteStore.fetchSiteDomains(site)).thenReturn(FetchedDomainsPayload(site, domains))

        viewModel.start(site)

        val dashboardItems = uiModel

        assertThat(dashboardItems).isNotEmpty
        assertThat(dashboardItems[0]).isInstanceOf(PrimaryDomain::class.java)
        assertThat(dashboardItems[1]).isInstanceOf(PrimaryDomain::class.java)
        assertThat(dashboardItems[2]).isInstanceOf(SiteDomainsHeader::class.java)
        assertThat(dashboardItems[3]).isInstanceOf(SiteDomains::class.java)
        assertThat(dashboardItems[4]).isInstanceOf(AddDomain::class.java)
    }

    @Test
    fun `paid plan with primary custom domain and no credits shows manage domains`() = test {
        addCustomDomain()
        hasDomainCredit = false
        whenever(siteStore.fetchSiteDomains(site)).thenReturn(FetchedDomainsPayload(site, domains))

        viewModel.start(site)

        val dashboardItems = uiModel

        assertThat(dashboardItems).isNotEmpty
        assertThat(dashboardItems[0]).isInstanceOf(PrimaryDomain::class.java)
        assertThat(dashboardItems[1]).isInstanceOf(PrimaryDomain::class.java)
        assertThat(dashboardItems[2]).isInstanceOf(SiteDomainsHeader::class.java)
        assertThat(dashboardItems[3]).isInstanceOf(SiteDomains::class.java)
        assertThat(dashboardItems[4]).isInstanceOf(AddDomain::class.java)
    }

    @Test
    fun `paid plan with secondary custom domain and credits shows manage your domains`() = test {
        addCustomDomain()
        hasDomainCredit = true
        whenever(siteStore.fetchSiteDomains(site)).thenReturn(FetchedDomainsPayload(site, domains))

        viewModel.start(site)

        val dashboardItems = uiModel

        assertThat(dashboardItems).isNotEmpty
        assertThat(dashboardItems[0]).isInstanceOf(PrimaryDomain::class.java)
        assertThat(dashboardItems[1]).isInstanceOf(PrimaryDomain::class.java)
        assertThat(dashboardItems[2]).isInstanceOf(SiteDomainsHeader::class.java)
        assertThat(dashboardItems[3]).isInstanceOf(SiteDomains::class.java)
        assertThat(dashboardItems[4]).isInstanceOf(AddDomain::class.java)
    }

    @Test
    fun `paid plan with secondary custom domain and no credits shows manage domain items`() = test {
        addCustomDomain()
        hasDomainCredit = false
        whenever(siteStore.fetchSiteDomains(site)).thenReturn(FetchedDomainsPayload(site, domains))

        viewModel.start(site)

        val dashboardItems = uiModel

        assertThat(dashboardItems[0]).isInstanceOf(PrimaryDomain::class.java)
        assertThat(dashboardItems[1]).isInstanceOf(PrimaryDomain::class.java)
        assertThat(dashboardItems[2]).isInstanceOf(SiteDomainsHeader::class.java)
        assertThat(dashboardItems[3]).isInstanceOf(SiteDomains::class.java)
        assertThat(dashboardItems[4]).isInstanceOf(AddDomain::class.java)
    }

    @Test
    fun `paid plan with no custom domain and credits shows claim your domain card`() = test {
        clearCustomDomain()
        hasDomainCredit = true
        whenever(siteStore.fetchSiteDomains(site)).thenReturn(FetchedDomainsPayload(site, domains))
        whenever(htmlMessageUtils.getHtmlMessageFromStringFormatResId(any(), any())).thenReturn("")

        viewModel.start(site)

        val dashboardItems = uiModel

        assertThat(dashboardItems[0]).isInstanceOf(PrimaryDomain::class.java)
        assertThat(dashboardItems[1]).isInstanceOf(PrimaryDomain::class.java)
        assertThat(dashboardItems[2]).isInstanceOf(PurchaseDomain::class.java)
    }

    @Test
    fun `paid plan with no custom domain and no credits shows get your domain card`() = test {
        clearCustomDomain()
        hasDomainCredit = false
        whenever(siteStore.fetchSiteDomains(site)).thenReturn(FetchedDomainsPayload(site, domains))
        whenever(htmlMessageUtils.getHtmlMessageFromStringFormatResId(any(), any())).thenReturn("")

        viewModel.start(site)

        val dashboardItems = uiModel

        assertThat(dashboardItems[0]).isInstanceOf(PrimaryDomain::class.java)
        assertThat(dashboardItems[1]).isInstanceOf(PrimaryDomain::class.java)
        assertThat(dashboardItems[2]).isInstanceOf(PurchaseDomain::class.java)
        assertThat((dashboardItems[2] as PurchaseDomain).title)
                .isEqualTo(UiStringRes(string.domains_free_plan_get_your_domain_title))
    }

    private fun clearCustomDomain() {
        domains = emptyList()
    }

    private fun addCustomDomain() {
        clearCustomDomain()
        domains = listOf(Domain(
                aRecordsRequiredForMapping = listOf("192.0.78.138, 192.0.78.237"),
                autoRenewalDate = "May 9, 2022",
                autoRenewing = true,
                blogId = 189860817,
                bundledPlanSubscriptionId = "17182973",
                canSetAsPrimary = true,
                connectionMode = null,
                contactInfoDisclosed = false,
                contactInfoDisclosureAvailable = false,
                currentUserCanAddEmail = true,
                currentUserCanCreateSiteFromDomainOnly = false,
                currentUserCanManage = true,
                currentUserCannotAddEmailReason = null,
                domain = "henna.tattoo",
                domainLockingAvailable = false,
                domainRegistrationAgreementUrl = "",
                emailForwardsCount = 0,
                expired = false,
                expiry = "June 8, 2022",
                expirySoon = false,
                googleAppsSubscription = GoogleAppsSubscription(status = "no_subscription"),
                hasPrivateRegistration = false,
                hasRegistration = false,
                hasWpcomNameservers = false,
                hasZone = true,
                isEligibleForInboundTransfer = false,
                isLocked = false,
                isPendingIcannVerification = false,
                isPremium = false,
                isRedeemable = false,
                isRenewable = false,
                isSubdomain = false,
                isWhoisEditable = false,
                isWpcomStagingDomain = false,
                manualTransferRequired = false,
                newRegistration = false,
                owner = "Test (tester)",
                partnerDomain = false,
                pendingRegistration = false,
                pendingRegistrationTime = "",
                pendingTransfer = false,
                pendingWhoisUpdate = false,
                pointsToWpcom = false,
                primaryDomain = false,
                privacyAvailable = false,
                privateDomain = false,
                productSlug = "domain_map",
                redeemableUntil = null,
                registrar = null,
                registrationDate = "July 27, 2021",
                renewableUntil = null,
                sslStatus = "pending",
                subdomainPart = null,
                subscriptionId = "17447242",
                supportsDomainConnect = false,
                supportsGdprConsentManagement = false,
                supportsTransferApproval = false,
                titanMailSubscription = TitanMailSubscription(
                        isEligibleForIntroductoryOffer = true, status = "no_subscription"),
                tldMaintenanceEndTime = 0,
                transferAwayEligibleAt = null,
                transferLockOnWhoisUpdateOptional = false,
                type = "mapping",
                whoisUpdateUnmodifiableFields = null,
                wpcomDomain = false))
    }
}
