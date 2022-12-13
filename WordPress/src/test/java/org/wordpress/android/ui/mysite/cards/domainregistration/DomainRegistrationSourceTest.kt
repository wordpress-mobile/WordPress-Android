package org.wordpress.android.ui.mysite.cards.domainregistration

import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
    private lateinit var isRefreshing: MutableList<Boolean>

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        site.id = siteLocalId
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        source = DomainRegistrationSource(
                TEST_DISPATCHER,
                dispatcher,
                selectedSiteRepository,
                appLogWrapper,
                siteUtils
        )
        result = mutableListOf()
        isRefreshing = mutableListOf()
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

    @Test
    fun `when build is invoked, then refresh is true`() = test {
        source.refresh.observeForever { isRefreshing.add(it) }

        source.build(testScope(), siteLocalId)

        assertThat(isRefreshing.last()).isTrue
    }

    @Test
    fun `when refresh is invoked, then refresh is true`() = test {
        source.refresh.observeForever { isRefreshing.add(it) }

        source.refresh()

        assertThat(isRefreshing.last()).isTrue
    }

    @Test
    fun `when data has been refreshed, then refresh is set to false`() = test {
        setupSite(site = site, currentPlan = buildPlan(hasDomainCredit = true))
        source.refresh.observeForever { isRefreshing.add(it) }

        source.build(testScope(), siteLocalId).observeForever { }
        source.refresh()

        assertThat(isRefreshing.last()).isFalse
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
        source.build(testScope(), siteLocalId).observeForever { result.add(it) }
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
