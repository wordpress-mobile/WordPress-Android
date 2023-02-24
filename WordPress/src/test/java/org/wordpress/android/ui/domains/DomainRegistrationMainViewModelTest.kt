package org.wordpress.android.ui.domains

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.AUTOMATED_TRANSFER
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.CTA_DOMAIN_CREDIT_REDEMPTION
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.DOMAIN_PURCHASE
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.FinishDomainRegistration
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.OpenDomainRegistrationDetails
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.OpenDomainRegistrationResult
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.OpenDomainSuggestions

@ExperimentalCoroutinesApi
class DomainRegistrationMainViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var tracker: DomainsRegistrationTracker

    @Mock
    lateinit var site: SiteModel

    private lateinit var viewModel: DomainRegistrationMainViewModel

    private val testDomainProductDetails = DomainProductDetails(76, "testdomain.blog")
    private val domainRegisteredEvent = DomainRegistrationCompletedEvent("testdomain.blog", "email@wordpress.org")

    private val navigationActions = mutableListOf<DomainRegistrationNavigationAction>()

    @Before
    fun setUp() {
        viewModel = DomainRegistrationMainViewModel(tracker)
        viewModel.onNavigation.observeForever { it.applyIfNotHandled { navigationActions.add(this) } }
    }

    @Test
    fun `domain suggestions are visible at start`() {
        viewModel.start(site, CTA_DOMAIN_CREDIT_REDEMPTION)
        assertThat(navigationActions).containsOnly(OpenDomainSuggestions)
    }

    @Test
    fun `selecting domain opens registration details`() {
        viewModel.start(site, CTA_DOMAIN_CREDIT_REDEMPTION)
        viewModel.selectDomain(testDomainProductDetails)

        assertThat(navigationActions.last()).isEqualTo(OpenDomainRegistrationDetails(testDomainProductDetails))
    }

    @Test
    fun `completing domain registration when registration purpose is credit redemption opens registration result`() {
        viewModel.start(site, CTA_DOMAIN_CREDIT_REDEMPTION)
        viewModel.completeDomainRegistration(domainRegisteredEvent)

        assertThat(navigationActions.last()).isEqualTo(OpenDomainRegistrationResult(domainRegisteredEvent))
    }

    @Test
    fun `completing domain registration when registration purpose is domain purchase opens registration result`() {
        viewModel.start(site, DOMAIN_PURCHASE)
        viewModel.completeDomainRegistration(domainRegisteredEvent)

        assertThat(navigationActions.last()).isEqualTo(OpenDomainRegistrationResult(domainRegisteredEvent))
    }

    @Test
    fun `completing domain registration when registration purpose is automated transfer finishes flow`() {
        viewModel.start(site, AUTOMATED_TRANSFER)
        viewModel.completeDomainRegistration(domainRegisteredEvent)

        assertThat(navigationActions.last()).isEqualTo(FinishDomainRegistration(domainRegisteredEvent))
    }
}
