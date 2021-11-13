package org.wordpress.android.ui.mysite.cards.domainregistration

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
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.PlanModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore.OnPlansFetched
import org.wordpress.android.fluxc.store.SiteStore.PlansError
import org.wordpress.android.fluxc.store.SiteStore.PlansErrorType
import org.wordpress.android.fluxc.store.SiteStore.PlansErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.test
import org.wordpress.android.testScope
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.DomainCreditAvailable
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.plans.PlansConstants.PREMIUM_PLAN_ID
import org.wordpress.android.util.SiteUtilsWrapper

class DomainRegistrationSourceTest : BaseUnitTest() {
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var appLogWrapper: AppLogWrapper
    @Mock lateinit var siteUtils: SiteUtilsWrapper
    private val siteLocalId = 1
    private val site = SiteModel()
    private lateinit var result: MutableList<DomainCreditAvailable>
    private lateinit var source: DomainRegistrationSource

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        source = DomainRegistrationSource(
                TEST_DISPATCHER,
                dispatcher,
                selectedSiteRepository,
                appLogWrapper,
                siteUtils
        )
        site.id = siteLocalId
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        result = mutableListOf()
    }

    @Test
    fun `when site is free, emit false and don't fetch`() = test {
        setupSite(site = site, isFree = true)

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
            source.onPlansFetched(event)
        }

        assertThat(result.last().isDomainCreditAvailable).isFalse
    }

    @Test
    fun `when fetch fails, emit false value`() = test {
        setupSite(site = site, error = GENERIC_ERROR)

        assertThat(result.last().isDomainCreditAvailable).isFalse
    }

    private fun setupSite(
        site: SiteModel,
        isFree: Boolean = false,
        currentPlan: PlanModel? = null,
        error: PlansErrorType? = null
    ) {
        whenever(siteUtils.onFreePlan(any())).thenReturn(isFree)
        buildOnPlansFetchedEvent(site, currentPlan, error)?.let { event ->
            whenever(dispatcher.dispatch(any())).then { source.onPlansFetched(event) }
        }
        source.buildSource(testScope(), siteLocalId).observeForever { result.add(it) }
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
