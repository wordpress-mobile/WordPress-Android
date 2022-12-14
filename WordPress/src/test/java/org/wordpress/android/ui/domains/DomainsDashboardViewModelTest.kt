package org.wordpress.android.ui.domains

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PlanModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.Domain
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.FetchedDomainsPayload
import org.wordpress.android.fluxc.store.SiteStore.OnPlansFetched
import org.wordpress.android.ui.domains.DomainsDashboardItem.AddDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.DomainBlurb
import org.wordpress.android.ui.domains.DomainsDashboardItem.FreeDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.PurchaseDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.SiteDomains
import org.wordpress.android.ui.domains.DomainsDashboardItem.SiteDomainsHeader
import org.wordpress.android.ui.domains.usecases.FetchPlansUseCase
import org.wordpress.android.ui.plans.PlansConstants.FREE_PLAN_ID
import org.wordpress.android.ui.plans.PlansConstants.PREMIUM_PLAN_ID
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@ExperimentalCoroutinesApi
class DomainsDashboardViewModelTest : BaseUnitTest() {
    private val siteStore: SiteStore = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val htmlMessageUtils: HtmlMessageUtils = mock {
        on { getHtmlMessageFromStringFormatResId(any(), any()) } doReturn ""
    }
    private val fetchPlansUseCase: FetchPlansUseCase = mock()

    private lateinit var viewModel: DomainsDashboardViewModel

    private val uiModel = mutableListOf<DomainsDashboardItem>()

    @Before
    fun setUp() {
        viewModel = DomainsDashboardViewModel(
                siteStore,
                analyticsTracker,
                htmlMessageUtils,
                fetchPlansUseCase,
                coroutinesTestRule.testDispatcher
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

        assertThat(dashboardItems[0]).isInstanceOf(FreeDomain::class.java)
        assertThat(dashboardItems[1]).isInstanceOf(SiteDomainsHeader::class.java)
        assertThat(dashboardItems[2]).isInstanceOf(SiteDomains::class.java)
        assertThat(dashboardItems[3]).isInstanceOf(AddDomain::class.java)
        assertThat(dashboardItems[4]).isInstanceOf(DomainBlurb::class.java)
    }

    @Test
    fun `free plan with no custom domains`() = test {
        setupWith(
                hasPaidPlan = false,
                hasCustomDomains = false,
                hasDomainCredits = false
        )

        val dashboardItems = uiModel

        assertThat(dashboardItems).hasSize(2)

        assertThat(dashboardItems[0]).isInstanceOf(FreeDomain::class.java)
        assertThat(dashboardItems[1]).isInstanceOf(PurchaseDomain::class.java)
        assertThat((dashboardItems[1] as PurchaseDomain).title)
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

        assertThat(dashboardItems).hasSize(4)

        assertThat(dashboardItems[0]).isInstanceOf(FreeDomain::class.java)
        assertThat(dashboardItems[1]).isInstanceOf(SiteDomainsHeader::class.java)
        assertThat(dashboardItems[2]).isInstanceOf(SiteDomains::class.java)
        assertThat(dashboardItems[3]).isInstanceOf(PurchaseDomain::class.java)
        assertThat((dashboardItems[3] as PurchaseDomain).title)
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

        assertThat(dashboardItems).hasSize(4)

        assertThat(dashboardItems[0]).isInstanceOf(FreeDomain::class.java)
        assertThat(dashboardItems[1]).isInstanceOf(SiteDomainsHeader::class.java)
        assertThat(dashboardItems[2]).isInstanceOf(SiteDomains::class.java)
        assertThat(dashboardItems[3]).isInstanceOf(AddDomain::class.java)
    }

    @Test
    fun `paid plan with no custom domains and credits`() = test {
        setupWith(
                hasPaidPlan = true,
                hasCustomDomains = false,
                hasDomainCredits = true
        )

        val dashboardItems = uiModel

        assertThat(dashboardItems).hasSize(2)

        assertThat(dashboardItems[0]).isInstanceOf(FreeDomain::class.java)
        assertThat(dashboardItems[1]).isInstanceOf(PurchaseDomain::class.java)
    }

    @Test
    fun `paid plan with no custom domains and no credits`() = test {
        setupWith(
                hasPaidPlan = true,
                hasCustomDomains = false,
                hasDomainCredits = false
        )

        val dashboardItems = uiModel

        assertThat(dashboardItems).hasSize(2)

        assertThat(dashboardItems[0]).isInstanceOf(FreeDomain::class.java)
        assertThat(dashboardItems[1]).isInstanceOf(PurchaseDomain::class.java)
        assertThat((dashboardItems[1] as PurchaseDomain).title)
                .isEqualTo(UiStringRes(R.string.domains_paid_plan_add_your_domain_title))
    }

    private suspend fun setupWith(hasPaidPlan: Boolean, hasCustomDomains: Boolean, hasDomainCredits: Boolean) {
        val site = if (hasPaidPlan) siteWithPaidPlan else siteWithFreePlan
        val domains = if (hasCustomDomains) listOf(customDomain) else emptyList()
        whenever(siteStore.fetchSiteDomains(site)).thenReturn(FetchedDomainsPayload(site, domains))

        val plan = if (hasDomainCredits) planWithCredits else planWithNoCredits
        whenever(fetchPlansUseCase.execute(site)).thenReturn(OnPlansFetched(site, listOf(plan)))

        viewModel.start(site)
    }

    companion object {
        private const val TEST_SITE_ID = 1234L
        private const val TEST_DOMAIN_NAME = "testdomain.blog"

        private val customDomain = Domain(
                domain = "henna.tattoo",
                expired = false,
                expiry = "June 8, 2022",
                expirySoon = false,
                primaryDomain = false,
                wpcomDomain = false
        )

        private val siteWithFreePlan = SiteModel().apply {
            siteId = TEST_SITE_ID
            url = TEST_DOMAIN_NAME
            unmappedUrl = TEST_DOMAIN_NAME
            planId = FREE_PLAN_ID
        }

        private val siteWithPaidPlan = SiteModel().apply {
            siteId = TEST_SITE_ID
            url = TEST_DOMAIN_NAME
            unmappedUrl = TEST_DOMAIN_NAME
            planId = PREMIUM_PLAN_ID
        }

        private val planWithNoCredits = PlanModel(
                productId = 1,
                productSlug = "plan-1",
                productName = "Plan 1",
                isCurrentPlan = true,
                hasDomainCredit = false
        )
        private val planWithCredits = PlanModel(
                productId = 2,
                productSlug = "plan-2",
                productName = "Plan 2",
                isCurrentPlan = true,
                hasDomainCredit = true
        )
    }
}
