package org.wordpress.android.ui.mysite

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.PlanModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore.OnPlansFetched
import org.wordpress.android.fluxc.store.SiteStore.PlansError
import org.wordpress.android.fluxc.store.SiteStore.PlansErrorType
import org.wordpress.android.fluxc.store.SiteStore.PlansErrorType.GENERIC_ERROR
import org.wordpress.android.ui.plans.PlansConstants.PREMIUM_PLAN_ID
import org.wordpress.android.util.SiteUtilsWrapper

class DomainRegistrationHandlerTest : BaseUnitTest() {
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var siteUtils: SiteUtilsWrapper
    @Mock lateinit var site: SiteModel
    private lateinit var isDomainCreditAvailableEvents: MutableList<Boolean>
    private lateinit var handler: DomainRegistrationHandler
    private val onSiteChange = MutableLiveData<SiteModel>()

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.selectedSiteChange).thenReturn(onSiteChange)
        whenever(selectedSiteRepository.getSelectedSite()).thenAnswer { onSiteChange.value }

        isDomainCreditAvailableEvents = mutableListOf()
        handler = DomainRegistrationHandler(dispatcher, selectedSiteRepository, siteUtils)
        handler.isDomainCreditAvailable.observeForever { isDomainCreditAvailableEvents.add(it) }
    }

    @Test
    fun `when site is null, don't emit value and don't fetch`() {
        onSiteChange.postValue(null)

        assertThat(isDomainCreditAvailableEvents).hasSize(0)

        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `when site is free, emit false and don't fetch`() {
        setupSite(site = site, isFree = true, hasCustomDomain = false)

        onSiteChange.postValue(site)

        assertThat(isDomainCreditAvailableEvents).hasSize(1)
        assertThat(isDomainCreditAvailableEvents.last()).isFalse

        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `when site has custom domain, emit false and don't fetch`() {
        setupSite(site = site, isFree = true, hasCustomDomain = false)

        onSiteChange.postValue(site)

        assertThat(isDomainCreditAvailableEvents).hasSize(1)
        assertThat(isDomainCreditAvailableEvents.last()).isFalse

        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `when site is not free and doesn't have custom domain, don't emit value and start fetch`() {
        setupSite(site = site, isFree = false, hasCustomDomain = false)

        onSiteChange.postValue(site)

        assertThat(isDomainCreditAvailableEvents).hasSize(0)

        verify(dispatcher, times(1)).dispatch(any())
    }

    @Test
    fun `when fetched site has a plan with credits, emit true`() {
        setupSite(site = site, currentPlan = buildPlan(hasDomainCredit = true))

        onSiteChange.postValue(site)

        assertThat(isDomainCreditAvailableEvents).hasSize(1)
        assertThat(isDomainCreditAvailableEvents.last()).isTrue
    }

    @Test
    fun `when fetched site doesn't have a plan with credits, emit false`() {
        setupSite(site = site, currentPlan = buildPlan(hasDomainCredit = false))

        onSiteChange.postValue(site)

        assertThat(isDomainCreditAvailableEvents).hasSize(1)
        assertThat(isDomainCreditAvailableEvents.last()).isFalse
    }

    @Test
    fun `when fetched site is different from currently selected site, don't emit value`() {
        val selectedSite = SiteModel().apply { id = 1 }

        setupSite(site = selectedSite, currentPlan = buildPlan(hasDomainCredit = false))

        onSiteChange.postValue(selectedSite)

        val fetchedSite = SiteModel().apply { id = 2 }

        buildOnPlansFetchedEvent(site = fetchedSite, currentPlan = buildPlan(hasDomainCredit = true))?.let { event ->
            handler.onPlansFetched(event)
        }

        assertThat(isDomainCreditAvailableEvents).hasSize(1)
        assertThat(isDomainCreditAvailableEvents.last()).isFalse
    }

    @Test
    fun `when fetch fails, don't emit value`() {
        setupSite(site = site, error = GENERIC_ERROR)

        onSiteChange.postValue(site)

        assertThat(isDomainCreditAvailableEvents).hasSize(0)
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
