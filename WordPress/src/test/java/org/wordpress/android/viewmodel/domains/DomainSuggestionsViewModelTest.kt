package org.wordpress.android.viewmodel.domains

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.CTA_DOMAIN_CREDIT_REDEMPTION
import org.wordpress.android.ui.plans.PlansConstants
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.SiteDomainsFeatureConfig
import org.wordpress.android.util.helpers.Debouncer

class DomainSuggestionsViewModelTest : BaseUnitTest() {
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var debouncer: Debouncer
    @Mock lateinit var tracker: AnalyticsTrackerWrapper
    @Mock lateinit var siteDomainsFeatureConfig: SiteDomainsFeatureConfig

    private lateinit var site: SiteModel
    private lateinit var domainRegistrationPurpose: DomainRegistrationPurpose
    private lateinit var viewModel: DomainSuggestionsViewModel

    @Before
    fun setUp() {
        site = SiteModel().also { it.name = "Test Site" }
        domainRegistrationPurpose = CTA_DOMAIN_CREDIT_REDEMPTION
        viewModel = DomainSuggestionsViewModel(tracker, dispatcher, debouncer, siteDomainsFeatureConfig)

        whenever(debouncer.debounce(any(), any(), any(), any())).thenAnswer { invocation ->
            val delayedRunnable = invocation.arguments[1] as Runnable
            delayedRunnable.run()
        }
    }

    @Test
    fun `intro is visible at start`() {
        viewModel.start(site, domainRegistrationPurpose)
        assertNotNull(viewModel.isIntroVisible.value)
        viewModel.isIntroVisible.value?.let { isIntroVisible ->
            assert(isIntroVisible)
        }
    }

    @Test
    fun `intro is hidden when search query is not empty`() {
        viewModel.start(site, domainRegistrationPurpose)
        viewModel.updateSearchQuery("Hello World")

        assertNotNull(viewModel.isIntroVisible.value)
        viewModel.isIntroVisible.value?.let { isIntroVisible ->
            assertFalse(isIntroVisible)
        }
    }

    @Test
    fun `intro is visible when search query is empty`() {
        viewModel.start(site, domainRegistrationPurpose)
        viewModel.updateSearchQuery("Hello World")
        viewModel.updateSearchQuery("")

        assertNotNull(viewModel.isIntroVisible.value)
        viewModel.isIntroVisible.value?.let { isIntroVisible ->
            assert(isIntroVisible)
        }
    }

    @Test
    fun `site on blogger plan is requesting only dot blog domain suggestions`() {
        site.planId = PlansConstants.BLOGGER_PLAN_ONE_YEAR_ID
        viewModel.start(site, domainRegistrationPurpose)
        viewModel.updateSearchQuery("test")

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(2)).dispatch(captor.capture())

        val lastAction = captor.value

        assertThat(lastAction.type).isEqualTo(SiteAction.SUGGEST_DOMAINS)
        assertThat(lastAction.payload).isNotNull
        assertThat(lastAction.payload).isInstanceOf(SuggestDomainsPayload::class.java)

        val payload = lastAction.payload as SuggestDomainsPayload
        assertThat(payload.tlds).isNotNull()
        assertThat(payload.tlds).isEqualTo("blog")
        assertThat(payload.onlyWordpressCom).isNull()
        assertThat(payload.includeWordpressCom).isNull()
        assertThat(payload.includeDotBlogSubdomain).isNull()
        assertThat(payload.includeVendorDot).isFalse()
    }

    @Test
    fun `site on non blogger plan is requesting all possible domain suggestions`() {
        site.planId = PlansConstants.PREMIUM_PLAN_ID
        viewModel.start(site, domainRegistrationPurpose)
        viewModel.updateSearchQuery("test")

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(2)).dispatch(captor.capture())

        val lastAction = captor.value

        assertThat(lastAction.type).isEqualTo(SiteAction.SUGGEST_DOMAINS)
        assertThat(lastAction.payload).isNotNull()
        assertThat(lastAction.payload).isInstanceOf(SuggestDomainsPayload::class.java)

        val payload = lastAction.payload as SuggestDomainsPayload
        assertThat(payload.onlyWordpressCom).isFalse()
        assertThat(payload.includeWordpressCom).isFalse()
        assertThat(payload.includeDotBlogSubdomain).isTrue()
        assertThat(payload.includeVendorDot).isFalse()
        assertThat(payload.tlds).isNull()
    }
}
