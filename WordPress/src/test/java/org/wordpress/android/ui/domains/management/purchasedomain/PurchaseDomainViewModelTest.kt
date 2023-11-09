package org.wordpress.android.ui.domains.management.purchasedomain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_EXISTING_SITE_CHOSEN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_EXISTING_SITE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_NEW_DOMAIN_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_SHOWN
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient.CreateShoppingCartResponse.Extra
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient.CreateShoppingCartResponse.Product
import org.wordpress.android.fluxc.store.TransactionsStore
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.GoBack
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.GoToDomainPurchasing
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.ActionEvent.GoToSitePicker
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.UiState.Initial
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.UiState.SubmittingJustDomainCart
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.UiState.SubmittingSiteDomainCart
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.UiState.ErrorSubmittingCart
import org.wordpress.android.ui.domains.usecases.CreateCartUseCase
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@ExperimentalCoroutinesApi
class PurchaseDomainViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper
    @Mock
    private lateinit var createCartUseCase: CreateCartUseCase

    private lateinit var viewModel: PurchaseDomainViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mockCartCreation()
        viewModel = PurchaseDomainViewModel(
            mainDispatcher = testDispatcher(),
            createCartUseCase = createCartUseCase,
            analyticsTracker = analyticsTracker,
            productId = productId,
            domain = domain,
            privacy = supportsPrivacy,
        )
    }

    @Test
    fun `WHEN ViewModel initialized THEN track DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_SHOWN event`() {
        verify(analyticsTracker).track(DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_SHOWN)
    }

    @Test
    fun `WHEN ViewModel initialized THEN the ui is set to the Initial state`() {
        assertThat(viewModel.uiStateFlow.value).isEqualTo(Initial)
    }

    @Test
    fun `WHEN new domain selected THEN track DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_NEW_DOMAIN_TAPPED event`() {
        viewModel.onNewDomainSelected()
        verify(analyticsTracker).track(DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_NEW_DOMAIN_TAPPED)
    }

    @Test
    fun `WHEN new domain selected THEN the cart is submitted with the selected domain`() = test {
        viewModel.onNewDomainSelected()
        verify(createCartUseCase, atLeastOnce()).execute(null, productId, domain, supportsPrivacy, false, null)
    }

    @Test
    fun `WHEN new domain selected THEN the ui is set to the SubmittingJustDomainCart state`() = test {
        assertThat(viewModel.uiStateFlow.value).isEqualTo(Initial)
        viewModel.onNewDomainSelected()
        assertThat(viewModel.uiStateFlow.value).isEqualTo(SubmittingJustDomainCart)
        advanceUntilIdle()
        assertThat(viewModel.uiStateFlow.value).isEqualTo(Initial)
    }

    @Test
    fun `WHEN a site is chosen THEN track DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_EXISTING_SITE_CHOSEN event`() {
        viewModel.onSiteChosen(testSite)
        verify(analyticsTracker).track(DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_EXISTING_SITE_CHOSEN)
    }

    @Test
    fun `WHEN a site is chosen THEN the cart is submitted with the selected domain`() = test {
        viewModel.onSiteChosen(testSite)
        verify(createCartUseCase, atLeastOnce()).execute(testSite, productId, domain, supportsPrivacy, false, null)
    }

    @Test
    fun `WHEN a site is chosen THEN the ui is set to the SubmittingSiteDomainCart state`() = test {
        assertThat(viewModel.uiStateFlow.value).isEqualTo(Initial)
        viewModel.onSiteChosen(testSite)
        assertThat(viewModel.uiStateFlow.value).isEqualTo(SubmittingSiteDomainCart)
        advanceUntilIdle()
        assertThat(viewModel.uiStateFlow.value).isEqualTo(Initial)
    }

    @Test
    fun `WHEN an error occurs while submitting the cart THEN the ui is set to the ErrorSubmittingCart state`() = test {
        mockCartError()
        assertThat(viewModel.uiStateFlow.value).isEqualTo(Initial)
        viewModel.onNewDomainSelected()
        assertThat(viewModel.uiStateFlow.value).isEqualTo(ErrorSubmittingCart)
    }


    @Test
    fun `WHEN the error button is tapped THEN the ui is set to the Initial state`() = test {
        mockCartError()
        viewModel.onNewDomainSelected()
        viewModel.onErrorButtonTapped()
        assertThat(viewModel.uiStateFlow.value).isEqualTo(Initial)
    }

    @Test
    fun `WHEN a site is chosen THEN send the GoToExistingSite action event`() = testWithActionEvents { events ->
        viewModel.onSiteChosen(testSite)
        advanceUntilIdle()

        assertThat(events.last()).isEqualTo(ActionEvent.GoToExistingSite(domain, testSite))
    }

    @Test
    @Suppress("MaxLineLength")
    fun `WHEN existing domain selected THEN track DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_EXISTING_SITE_TAPPED event`() {
        viewModel.onExistingSiteSelected()
        verify(analyticsTracker).track(DOMAIN_MANAGEMENT_PURCHASE_DOMAIN_SCREEN_EXISTING_SITE_TAPPED)
    }

    @Test
    fun `WHEN new domain selected THEN send GoToDomainPurchasing action event`() = testWithActionEvents { events ->
        viewModel.onNewDomainSelected()
        advanceUntilIdle()

        assertThat(events.last()).isEqualTo(GoToDomainPurchasing(domain))
    }

    @Test
    fun `WHEN existing site selected THEN send GoToSitePicker action event`() = testWithActionEvents { events ->
        viewModel.onExistingSiteSelected()
        advanceUntilIdle()

        assertThat(events.last()).isEqualTo(GoToSitePicker(domain))
    }

    @Test
    fun `WHEN back button pressed THEN send GoBack action event`() = testWithActionEvents { events ->
        viewModel.onBackPressed()
        advanceUntilIdle()

        assertThat(events.last()).isEqualTo(GoBack)
    }

    private fun mockCartCreation() = test {
        whenever(
            createCartUseCase.execute(
                null, productId, domain,
                isDomainPrivacyEnabled = true,
                isTemporary = false,
                planProductId = null
            )
        ).thenReturn(
            TransactionsStore.OnShoppingCartCreated(
                TransactionsRestClient.CreateShoppingCartResponse(0, cartKey, listOf(testProduct))
            )
        )
        whenever(
            createCartUseCase.execute(
                testSite, productId, domain,
                isDomainPrivacyEnabled = true,
                isTemporary = false,
                planProductId = null
            )
        ).thenReturn(
            TransactionsStore.OnShoppingCartCreated(
                TransactionsRestClient.CreateShoppingCartResponse(siteId.toInt(), cartKey, listOf(testProduct))
            )
        )
    }

    private fun mockCartError() = test {
        whenever(
            createCartUseCase.execute(
                null, productId, domain,
                isDomainPrivacyEnabled = true,
                isTemporary = false,
                planProductId = null
            )
        ).thenReturn(
            TransactionsStore.OnShoppingCartCreated(shoppingCartCreateError)
        )
    }

    private fun testWithActionEvents(block: suspend TestScope.(events: List<ActionEvent>) -> Unit) = test {
        val actionEvents = mutableListOf<ActionEvent>()
        val job = launch { viewModel.actionEvents.toList(actionEvents) }

        block(actionEvents)

        job.cancel()
    }

    companion object {
        private const val productId = 8
        private const val domain = "domain.com"
        private const val cartKey = "cart_key"
        private const val siteId = 5L
        private const val supportsPrivacy = true
        private val testSite = SiteModel().also { it.siteId = siteId }
        private val testProduct = Product(productId, domain, Extra(privacy = true))
        private val shoppingCartCreateError = TransactionsStore.CreateShoppingCartError(
            TransactionsStore.CreateCartErrorType.GENERIC_ERROR,
            "Error Creating Cart"
        )
    }
}
