package org.wordpress.android.ui.domains

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.domains.DomainsDashboardItem.AddDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.PurchaseDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.PurchasePlan
import org.wordpress.android.ui.domains.DomainsDashboardItem.SiteDomains
import org.wordpress.android.ui.domains.DomainsDashboardItem.SiteDomainsHeader
import org.wordpress.android.ui.domains.management.testDomainItem
import org.wordpress.android.ui.domains.usecases.AllDomains
import org.wordpress.android.ui.domains.usecases.FetchAllDomainsUseCase
import org.wordpress.android.ui.domains.usecases.FetchPlansUseCase
import org.wordpress.android.ui.domains.usecases.FetchSiteDomainsUseCase
import org.wordpress.android.ui.domains.usecases.SiteDomainsResult
import org.wordpress.android.ui.domains.usecases.SitePlansResult
import org.wordpress.android.util.PlansConstants.FREE_PLAN_ID
import org.wordpress.android.util.PlansConstants.PREMIUM_PLAN_ID
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import uniffi.wp_api.SiteDomain
import uniffi.wp_api.SitePlan
import uniffi.wp_api.SitePlanCurrentPlanInfo

@ExperimentalCoroutinesApi
class DomainsDashboardViewModelTest : BaseUnitTest() {
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val htmlMessageUtils: HtmlMessageUtils = mock()
    private val fetchPlansUseCase: FetchPlansUseCase = mock()
    private val fetchSiteDomainsUseCase: FetchSiteDomainsUseCase = mock()
    private val fetchAllDomainsUseCase: FetchAllDomainsUseCase = mock()

    private lateinit var viewModel: DomainsDashboardViewModel

    private val uiModel = mutableListOf<DomainsDashboardItem>()

    @Before
    fun setUp() {
        viewModel = DomainsDashboardViewModel(
            analyticsTracker,
            htmlMessageUtils,
            fetchPlansUseCase,
            fetchSiteDomainsUseCase,
            fetchAllDomainsUseCase,
            testDispatcher()
        )

        viewModel.uiModel.observeForever { if (it != null) uiModel += it }
    }

    @Test
    fun `free plan with custom domains`() = test {
        setupWith(
            hasPaidPlan = false,
            hasCustomDomains = true,
            hasDomainCredits = false
        )

        val dashboardItems = uiModel

        assertThat(dashboardItems).hasSize(5)

        assertThat(dashboardItems[0]).isInstanceOf(SiteDomainsHeader::class.java)
        assertThat(dashboardItems[1]).isInstanceOf(SiteDomains::class.java)
        assertThat(dashboardItems[2]).isInstanceOf(SiteDomainsHeader::class.java)
        assertThat(dashboardItems[3]).isInstanceOf(SiteDomains::class.java)
        assertThat(dashboardItems[4]).isInstanceOf(AddDomain::class.java)
    }

    @Test
    fun `free plan with no custom domains`() = test {
        setupWith(
            hasPaidPlan = false,
            hasCustomDomains = false,
            hasDomainCredits = false
        )

        val dashboardItems = uiModel

        assertThat(dashboardItems).hasSize(3)

        assertThat(dashboardItems[0]).isInstanceOf(SiteDomainsHeader::class.java)
        assertThat(dashboardItems[1]).isInstanceOf(SiteDomains::class.java)
        assertThat(dashboardItems[2]).isInstanceOf(PurchasePlan::class.java)
        assertThat((dashboardItems[2] as PurchasePlan).title)
            .isEqualTo(UiStringRes(R.string.domains_free_plan_get_your_domain_title))
    }

    @Test
    fun `paid plan with custom domains and credits`() = test {
        setupWith(
            hasPaidPlan = true,
            hasCustomDomains = true,
            hasDomainCredits = true
        )

        val dashboardItems = uiModel

        assertThat(dashboardItems).hasSize(5)

        assertThat(dashboardItems[0]).isInstanceOf(SiteDomainsHeader::class.java)
        assertThat(dashboardItems[1]).isInstanceOf(SiteDomains::class.java)
        assertThat(dashboardItems[2]).isInstanceOf(SiteDomainsHeader::class.java)
        assertThat(dashboardItems[3]).isInstanceOf(SiteDomains::class.java)
        assertThat(dashboardItems[4]).isInstanceOf(PurchaseDomain::class.java)
        assertThat((dashboardItems[4] as PurchaseDomain).title)
            .isEqualTo(UiStringRes(R.string.domains_paid_plan_claim_your_domain_title))
    }

    @Test
    fun `paid plan with custom domains and no credits`() = test {
        setupWith(
            hasPaidPlan = true,
            hasCustomDomains = true,
            hasDomainCredits = false
        )

        val dashboardItems = uiModel

        assertThat(dashboardItems).hasSize(5)

        assertThat(dashboardItems[0]).isInstanceOf(SiteDomainsHeader::class.java)
        assertThat(dashboardItems[1]).isInstanceOf(SiteDomains::class.java)
        assertThat(dashboardItems[2]).isInstanceOf(SiteDomainsHeader::class.java)
        assertThat(dashboardItems[3]).isInstanceOf(SiteDomains::class.java)
        assertThat(dashboardItems[4]).isInstanceOf(AddDomain::class.java)
    }

    @Test
    fun `paid plan with no custom domains and credits`() = test {
        setupWith(
            hasPaidPlan = true,
            hasCustomDomains = false,
            hasDomainCredits = true
        )

        val dashboardItems = uiModel

        assertThat(dashboardItems).hasSize(3)

        assertThat(dashboardItems[0]).isInstanceOf(SiteDomainsHeader::class.java)
        assertThat(dashboardItems[1]).isInstanceOf(SiteDomains::class.java)
        assertThat(dashboardItems[2]).isInstanceOf(PurchaseDomain::class.java)
    }

    @Test
    fun `paid plan with no custom domains and no credits`() = test {
        setupWith(
            hasPaidPlan = true,
            hasCustomDomains = false,
            hasDomainCredits = false
        )

        val dashboardItems = uiModel

        assertThat(dashboardItems).hasSize(3)

        assertThat(dashboardItems[0]).isInstanceOf(SiteDomainsHeader::class.java)
        assertThat(dashboardItems[1]).isInstanceOf(SiteDomains::class.java)
        assertThat(dashboardItems[2]).isInstanceOf(PurchaseDomain::class.java)
        assertThat((dashboardItems[2] as PurchaseDomain).title)
            .isEqualTo(UiStringRes(R.string.domains_paid_plan_add_your_domain_title))
    }

    @Test
    fun `custom domain row carries the values the API reported`() = test {
        setupWith(
            hasPaidPlan = true,
            hasCustomDomains = true,
            hasDomainCredits = false
        )

        val customDomainRow = uiModel[3] as SiteDomains

        assertThat(customDomainRow.domain).isEqualTo(UiStringText("henna.tattoo"))
        assertThat(customDomainRow.isPrimary).isFalse()
        assertThat(customDomainRow.domainStatus).isEqualTo(UiStringText("Active"))
        assertThat(customDomainRow.domainStatusColor).isEqualTo(R.color.jetpack_green_50)
        assertThat(customDomainRow.expiry).isEqualTo(
            UiStringResWithParams(
                R.string.domains_site_domain_expires,
                listOf(UiStringText("June 8, 2022"))
            )
        )
    }

    @Test
    fun `free domain row falls back to the site address when the fetch fails`() = test {
        val site = siteWithPaidPlan
        whenever(fetchSiteDomainsUseCase.execute(site)).thenReturn(SiteDomainsResult.Error)
        whenever(fetchPlansUseCase.execute(site)).thenReturn(SitePlansResult.Error)
        whenever(fetchAllDomainsUseCase.execute()).thenReturn(AllDomains.Error)

        viewModel.start(site)

        val dashboardItems = uiModel

        assertThat(dashboardItems).hasSize(3)
        assertThat(dashboardItems[1]).isInstanceOf(SiteDomains::class.java)
        assertThat((dashboardItems[1] as SiteDomains).domain).isEqualTo(UiStringText(TEST_DOMAIN_NAME))
        assertThat(dashboardItems[2]).isInstanceOf(PurchaseDomain::class.java)
        assertThat((dashboardItems[2] as PurchaseDomain).title)
            .isEqualTo(UiStringRes(R.string.domains_paid_plan_add_your_domain_title))
    }

    @Test
    fun `a domain that omits the wpcom flag counts as a custom domain`() = test {
        setupWith(
            site = siteWithPaidPlan,
            domains = listOf(testSiteDomain(domain = "henna.tattoo", wpcomDomain = null))
        )

        val dashboardItems = uiModel

        assertThat(dashboardItems).hasSize(5)
        assertThat(dashboardItems[2]).isInstanceOf(SiteDomainsHeader::class.java)
        assertThat((dashboardItems[3] as SiteDomains).domain).isEqualTo(UiStringText("henna.tattoo"))
    }

    @Test
    fun `a domain that omits the registration flag has no expiry line`() = test {
        setupWith(
            site = siteWithPaidPlan,
            domains = listOf(
                testSiteDomain(
                    domain = "henna.tattoo",
                    hasRegistration = null,
                    expiry = "June 8, 2022"
                )
            )
        )

        assertThat((uiModel[3] as SiteDomains).expiry).isNull()
    }

    private suspend fun setupWith(site: SiteModel, domains: List<SiteDomain>) {
        val plan = planWithCredit(false)
        whenever(fetchSiteDomainsUseCase.execute(site)).thenReturn(SiteDomainsResult.Success(domains))
        whenever(fetchPlansUseCase.execute(site))
            .thenReturn(SitePlansResult.Success(mapOf(TEST_PRODUCT_ID to plan)))
        whenever(fetchAllDomainsUseCase.execute()).thenReturn(AllDomains.Success(listOf(allDomainsDomain)))

        viewModel.start(site)
    }

    private suspend fun setupWith(hasPaidPlan: Boolean, hasCustomDomains: Boolean, hasDomainCredits: Boolean) {
        val site = if (hasPaidPlan) siteWithPaidPlan else siteWithFreePlan
        val domains = if (hasCustomDomains) listOf(customDomain) else emptyList()
        whenever(fetchSiteDomainsUseCase.execute(site)).thenReturn(SiteDomainsResult.Success(domains))

        val plan = planWithCredit(hasDomainCredits)
        whenever(fetchPlansUseCase.execute(site))
            .thenReturn(SitePlansResult.Success(mapOf(TEST_PRODUCT_ID to plan)))
        val allDomains = if (hasCustomDomains) listOf(allDomainsDomain) else emptyList()
        whenever(fetchAllDomainsUseCase.execute()).thenReturn(AllDomains.Success(allDomains))

        viewModel.start(site)
    }

    private fun planWithCredit(hasDomainCredit: Boolean): SitePlan = mock {
        on { currentPlan } doReturn SitePlanCurrentPlanInfo(
            purchaseId = null,
            userIsOwner = null,
            hasDomainCredit = hasDomainCredit
        )
    }

    companion object {
        private const val TEST_SITE_ID = 1234L
        private const val TEST_DOMAIN_NAME = "testdomain.blog"
        private const val TEST_SITE_NAME = "Test Site"
        private const val TEST_PRODUCT_ID = 1uL

        private val customDomain = testSiteDomain(
            domain = "henna.tattoo",
            hasRegistration = true,
            expiry = "June 8, 2022",
            expirySoon = false,
            primaryDomain = false,
            wpcomDomain = false
        )

        private val allDomainsDomain = testDomainItem(domain = "henna.tattoo")

        private val siteWithFreePlan = SiteModel().apply {
            siteId = TEST_SITE_ID
            url = TEST_DOMAIN_NAME
            name = TEST_SITE_NAME
            unmappedUrl = TEST_DOMAIN_NAME
            planId = FREE_PLAN_ID
        }

        private val siteWithPaidPlan = SiteModel().apply {
            siteId = TEST_SITE_ID
            url = TEST_DOMAIN_NAME
            name = TEST_SITE_NAME
            unmappedUrl = TEST_DOMAIN_NAME
            planId = PREMIUM_PLAN_ID
        }
    }
}
