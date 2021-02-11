package org.wordpress.android.ui.mysite

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.TEST_SCOPE
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.PlanModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore.OnPlansFetched
import org.wordpress.android.fluxc.store.SiteStore.PlansError
import org.wordpress.android.fluxc.store.SiteStore.PlansErrorType
import org.wordpress.android.fluxc.store.SiteStore.PlansErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.test
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.DomainCreditAvailable
import org.wordpress.android.ui.plans.PlansConstants.PREMIUM_PLAN_ID
import org.wordpress.android.util.SiteUtilsWrapper

class DomainRegistrationHandlerTest : BaseUnitTest() {
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var appLogWrapper: AppLogWrapper
    @Mock lateinit var siteUtils: SiteUtilsWrapper
    private val siteId = 1
    private val site = SiteModel()
    private lateinit var result: MutableList<DomainCreditAvailable>
    private lateinit var handler: DomainRegistrationHandler

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        handler = DomainRegistrationHandler(
                TEST_DISPATCHER,
                dispatcher,
                selectedSiteRepository,
                appLogWrapper,
                siteUtils
        )
        site.id = siteId
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        result = mutableListOf()
    }

    @Test
    fun `when site is free, emit false and don't fetch`() = test {
        setupSite(site = site, isFree = true, hasCustomDomain = false)

        assertThat(result.last().isDomainCreditAvailable).isFalse

        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `when site has custom domain, emit false and don't fetch`() = test {
        setupSite(site = site, isFree = false, hasCustomDomain = true)

        assertThat(result.last().isDomainCreditAvailable).isFalse

        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `when fetched site has a plan with credits, start to fetch and emit true`() = test {
        setupSite(site = site, currentPlan = buildPlan(hasDomainCredit = true))

        assertThat(result.last().isDomainCreditAvailable).isTrue

        verify(dispatcher, times(1)).dispatch(any())
    }

    @Test
    fun `when fetched site doesn't have a plan with credits, start to fetch and emit false`() = test {
        setupSite(site = site, currentPlan = buildPlan(hasDomainCredit = false))

        assertThat(result.last().isDomainCreditAvailable).isFalse
    }

    @Test
    fun `when fetched site is different from currently selected site, don't emit value`() = test {
        val selectedSite = SiteModel().apply { id = 1 }

        setupSite(site = selectedSite, currentPlan = buildPlan(hasDomainCredit = false))

        val fetchedSite = SiteModel().apply { id = 2 }

        buildOnPlansFetchedEvent(site = fetchedSite, currentPlan = buildPlan(hasDomainCredit = true))?.let { event ->
            handler.onPlansFetched(event)
        }

        assertThat(result.last().isDomainCreditAvailable).isFalse
    }

    @Test
    fun `when fetch fails, don't emit value`() = test {
        setupSite(site = site, error = GENERIC_ERROR)

        assertThat(result.count()).isEqualTo(0)
    }

    private fun setupSite(
        site: SiteModel,
        isFree: Boolean = false,
        hasCustomDomain: Boolean = false,
        currentPlan: PlanModel? = null,
        error: PlansErrorType? = null
    ) {
        whenever(siteUtils.onFreePlan(any())).thenReturn(isFree)
        whenever(siteUtils.hasCustomDomain(any())).thenReturn(hasCustomDomain)
        buildOnPlansFetchedEvent(site, currentPlan, error)?.let { event ->
            whenever(dispatcher.dispatch(any())).then { handler.onPlansFetched(event) }
        }
        handler.buildSource(TEST_SCOPE, siteId).observeForever { result.add(it) }
    }

    private fun buildOnPlansFetchedEvent(
        site: SiteModel,
        currentPlan: PlanModel? = null,
        error: PlansErrorType? = null
    ) = if (currentPlan != null || error != null) {
        OnPlansFetched(site, currentPlan?.let { listOf(it) }, error?.let { PlansError(it) })
    } else {
        null
    }

    private fun buildPlan(hasDomainCredit: Boolean) = PlanModel(
            productId = PREMIUM_PLAN_ID.toInt(),
            productSlug = null,
            productName = null,
            isCurrentPlan = true,
            hasDomainCredit = hasDomainCredit
    )
}
