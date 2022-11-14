package org.wordpress.android.ui.domains

import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient.CreateShoppingCartResponse
import org.wordpress.android.fluxc.store.ProductsStore
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload
import org.wordpress.android.fluxc.store.TransactionsStore.OnShoppingCartCreated
import org.wordpress.android.test
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.CTA_DOMAIN_CREDIT_REDEMPTION
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.DOMAIN_PURCHASE
import org.wordpress.android.ui.domains.usecases.CreateCartUseCase
import org.wordpress.android.ui.plans.PlansConstants
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.SiteDomainsFeatureConfig
import org.wordpress.android.util.helpers.Debouncer

class DomainSuggestionsViewModelTest : BaseUnitTest() {
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var debouncer: Debouncer
    @Mock lateinit var tracker: AnalyticsTrackerWrapper
    @Mock lateinit var siteDomainsFeatureConfig: SiteDomainsFeatureConfig
    @Mock lateinit var createCartUseCase: CreateCartUseCase
    @Mock lateinit var productsStore: ProductsStore

    private lateinit var site: SiteModel
    private lateinit var domainRegistrationPurpose: DomainRegistrationPurpose
    private lateinit var viewModel: DomainSuggestionsViewModel
    private lateinit var onDomainSelectedEvents: MutableList<DomainProductDetails>

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        site = SiteModel().also { it.name = "Test Site" }
        domainRegistrationPurpose = CTA_DOMAIN_CREDIT_REDEMPTION
        viewModel = DomainSuggestionsViewModel(
                productsStore,
                tracker,
                dispatcher,
                debouncer,
                siteDomainsFeatureConfig,
                createCartUseCase,
                TEST_DISPATCHER
        )

        whenever(debouncer.debounce(any(), any(), any(), any())).thenAnswer { invocation ->
            val delayedRunnable = invocation.arguments[1] as Runnable
            delayedRunnable.run()
        }

        onDomainSelectedEvents = mutableListOf()
        viewModel.onDomainSelected.observeForever { onDomainSelectedEvents.add(it.peekContent()) }
    }

    @Test
    fun `redirect message is visible when purchasing a domain at start`() {
        domainRegistrationPurpose = DOMAIN_PURCHASE
        viewModel.start(site, domainRegistrationPurpose)
        assertNotNull(viewModel.showRedirectMessage.value)
        viewModel.showRedirectMessage.value?.let { siteUrl ->
            assertNotNull(siteUrl)
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
    fun `site on blogger plan is requesting only dot blog domain suggestions`() = test {
        site.planId = PlansConstants.BLOGGER_PLAN_ONE_YEAR_ID

        viewModel.start(site, domainRegistrationPurpose)
        viewModel.updateSearchQuery("test")

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(1)).dispatch(captor.capture())

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
    fun `site on non blogger plan is requesting all possible domain suggestions`() = test {
        site.planId = PlansConstants.PREMIUM_PLAN_ID
        viewModel.start(site, domainRegistrationPurpose)
        viewModel.updateSearchQuery("test")

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(1)).dispatch(captor.capture())

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

    @Test
    fun `clicking select domain button for credit redemption emits selected domain`() = test {
        viewModel.start(site, CTA_DOMAIN_CREDIT_REDEMPTION)
        viewModel.onDomainSuggestionSelected(dummySelectedDomainSuggestionItem)
        viewModel.onSelectDomainButtonClicked()

        verifyNoInteractions(createCartUseCase)

        assertThat(onDomainSelectedEvents.last()).isEqualTo(DomainProductDetails(DUMMY_PRODUCT_ID, DUMMY_DOMAIN_NAME))
    }

    @Test
    fun `clicking select domain button for purchase calls cart creation use case and emits selected domain`() = test {
        whenever(createCartUseCase.execute(site, DUMMY_PRODUCT_ID, DUMMY_DOMAIN_NAME, true, false))
                .thenReturn(dummySuccessfulOnShoppingCartCreated)

        viewModel.start(site, DOMAIN_PURCHASE)
        viewModel.onDomainSuggestionSelected(dummySelectedDomainSuggestionItem)
        viewModel.onSelectDomainButtonClicked()

        assertThat(onDomainSelectedEvents.last()).isEqualTo(DomainProductDetails(DUMMY_PRODUCT_ID, DUMMY_DOMAIN_NAME))
    }

    companion object {
        const val DUMMY_PRODUCT_ID = 1
        const val DUMMY_DOMAIN_NAME = "domainname.com"

        val dummySuccessfulOnShoppingCartCreated = OnShoppingCartCreated(
                CreateShoppingCartResponse(
                        1,
                        "dummy_cart_key",
                        emptyList()
                )
        )

        val dummySelectedDomainSuggestionItem = DomainSuggestionItem(
                domainName = DUMMY_DOMAIN_NAME,
                cost = "$20.00",
                isOnSale = false,
                saleCost = "0.0",
                isFree = false,
                supportsPrivacy = true,
                productId = DUMMY_PRODUCT_ID,
                productSlug = null,
                vendor = null,
                relevance = 1.0f,
                isSelected = true,
                isCostVisible = true,
                isFreeWithCredits = false,
                isEnabled = true
        )
    }
}
