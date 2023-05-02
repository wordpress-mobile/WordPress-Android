package org.wordpress.android.ui.sitecreation.progress

import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atMost
import org.mockito.kotlin.check
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.refEq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.TransactionsStore.OnShoppingCartCreated
import org.wordpress.android.ui.domains.DomainRegistrationCheckoutWebViewActivity.OpenCheckout.CheckoutDetails
import org.wordpress.android.ui.domains.usecases.CreateCartUseCase
import org.wordpress.android.ui.sitecreation.CART_ERROR
import org.wordpress.android.ui.sitecreation.CART_SUCCESS
import org.wordpress.android.ui.sitecreation.PAID_DOMAIN
import org.wordpress.android.ui.sitecreation.RESULT_CREATED
import org.wordpress.android.ui.sitecreation.RESULT_IN_CART
import org.wordpress.android.ui.sitecreation.SERVICE_ERROR
import org.wordpress.android.ui.sitecreation.SERVICE_SUCCESS
import org.wordpress.android.ui.sitecreation.SITE_CREATION_STATE
import org.wordpress.android.ui.sitecreation.SITE_REMOTE_ID
import org.wordpress.android.ui.sitecreation.SITE_SLUG
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.progress.SiteCreationProgressViewModel.SiteProgressUiState
import org.wordpress.android.ui.sitecreation.progress.SiteCreationProgressViewModel.SiteProgressUiState.Error.CartError
import org.wordpress.android.ui.sitecreation.progress.SiteCreationProgressViewModel.SiteProgressUiState.Error.ConnectionError
import org.wordpress.android.ui.sitecreation.progress.SiteCreationProgressViewModel.SiteProgressUiState.Error.GenericError
import org.wordpress.android.ui.sitecreation.progress.SiteCreationProgressViewModel.SiteProgressUiState.Loading
import org.wordpress.android.ui.sitecreation.progress.SiteCreationProgressViewModel.StartServiceData
import org.wordpress.android.util.NetworkUtilsWrapper
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteCreationProgressViewModelTest : BaseUnitTest() {
    private var networkUtils = mock<NetworkUtilsWrapper>()
    private var tracker = mock<SiteCreationTracker>()
    private val createCartUseCase = mock<CreateCartUseCase>()

    private val uiStateObserver = mock<Observer<SiteProgressUiState>>()
    private val startServiceObserver = mock<Observer<StartServiceData>>()
    private val onHelpClickedObserver = mock<Observer<Unit>>()
    private val onCancelWizardClickedObserver = mock<Observer<Unit>>()
    private val onRemoteSiteCreatedObserver = mock<Observer<SiteModel>>()
    private val onCartCreatedObserver = mock<Observer<CheckoutDetails>>()

    private lateinit var viewModel: SiteCreationProgressViewModel

    @Before
    fun setUp() {
        viewModel = SiteCreationProgressViewModel(
            networkUtils,
            tracker,
            createCartUseCase,
            testDispatcher(),
        )
        viewModel.uiState.observeForever(uiStateObserver)
        viewModel.startCreateSiteService.observeForever(startServiceObserver)
        viewModel.onHelpClicked.observeForever(onHelpClickedObserver)
        viewModel.onCancelWizardClicked.observeForever(onCancelWizardClickedObserver)
        viewModel.onFreeSiteCreated.observeForever(onRemoteSiteCreatedObserver)
        viewModel.onCartCreated.observeForever(onCartCreatedObserver)

        whenever(networkUtils.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `on start shows progress`() = test {
        startViewModel()
        assertIs<SiteProgressUiState>(viewModel.uiState.value)
    }

    @Test
    fun `on start emits service event`() = test {
        startViewModel()
        assertNotNull(viewModel.startCreateSiteService.value)
    }

    @Test
    fun `on start emits service event for free domains with isFree true`() = test {
        startViewModel(SITE_CREATION_STATE)
        val request = assertNotNull(viewModel.startCreateSiteService.value).serviceData
        assertTrue(request.isFree)
    }

    @Test
    fun `on start emits service event for paid domains with isFree false`() = test {
        startViewModel(SITE_CREATION_STATE.copy(domain = PAID_DOMAIN))
        val request = assertNotNull(viewModel.startCreateSiteService.value).serviceData
        assertFalse(request.isFree)
    }

    @Test
    fun `on start shows error when network is not available`() = test {
        whenever(networkUtils.isNetworkAvailable()).thenReturn(false)
        startViewModel()
        advanceUntilIdle()
        assertIs<ConnectionError>(viewModel.uiState.value)
    }

    @Test
    fun `on start shows first loading text without animation`() = test {
        startViewModel()
        verify(uiStateObserver).onChanged(check<Loading> { !it.animate })
    }

    @Test
    fun `on start changes the loading text with animation after delay`() = test {
        startViewModel()
        advanceTimeBy(LOADING_STATE_TEXT_ANIMATION_DELAY)
        verify(uiStateObserver).onChanged(check<Loading> { it.animate })
    }

    @Test
    fun `on start changes the loading text with animation after delay 4 times`() = test {
        startViewModel()
        val captor = argumentCaptor<Loading>()
        (1..9).forEach {
            verify(uiStateObserver, atMost(it)).onChanged(captor.capture())
            advanceTimeBy(LOADING_STATE_TEXT_ANIMATION_DELAY)
        }
        assertThat(captor.allValues.distinctBy { it.loadingTextResId }).hasSize(4)
    }

    @Test
    fun `on retry click after service error emits service event with the previous result`() = test {
        startViewModel()
        viewModel.onSiteCreationServiceStateUpdated(SERVICE_ERROR)
        viewModel.retry()
        assertEquals(viewModel.startCreateSiteService.value?.previousState, SERVICE_ERROR.payload)
    }

    @Test
    fun `on retry click after network error restarts site creation service`() = test {
        whenever(networkUtils.isNetworkAvailable()).thenReturn(false, true)
        startViewModel()
        advanceUntilIdle()
        viewModel.retry()
        verify(startServiceObserver).onChanged(any())
    }

    @Test
    fun `on retry click after cart error retries to create cart`() = testWith(CART_ERROR) {
        startViewModel(SITE_CREATION_STATE.copy(domain = PAID_DOMAIN))
        viewModel.onSiteCreationServiceStateUpdated(SERVICE_SUCCESS)
        viewModel.retry()
        verify(createCartUseCase, times(2)).execute(any(), any(), any(), any(), eq(false))
    }

    @Test
    fun `on help click is propagated`() {
        startViewModel()
        viewModel.onHelpClicked()
        verify(onHelpClickedObserver).onChanged(isNull())
    }

    @Test
    fun `on cancel wizard click is propagated`() {
        startViewModel()
        viewModel.onCancelWizardClicked()
        verify(onCancelWizardClickedObserver).onChanged(isNull())
    }

    @Test
    fun `on service success propagates site`() {
        startViewModel()
        viewModel.onSiteCreationServiceStateUpdated(SERVICE_SUCCESS)
        verify(onRemoteSiteCreatedObserver).onChanged(argThat {
            assertEquals(SITE_REMOTE_ID, siteId)
            url == SITE_SLUG
        })
    }

    @Test
    fun `on service success for paid domain creates cart`() = test {
        startViewModel(SITE_CREATION_STATE.copy(domain = PAID_DOMAIN))
        viewModel.onSiteCreationServiceStateUpdated(SERVICE_SUCCESS)
        verify(createCartUseCase).execute(
            any(),
            eq(PAID_DOMAIN.productId),
            eq(PAID_DOMAIN.domainName),
            eq(PAID_DOMAIN.supportsPrivacy),
            any(),
        )
    }

    @Test
    fun `on cart success propagates checkout details`() = testWith(CART_SUCCESS) {
        startViewModel(SITE_CREATION_STATE.copy(domain = PAID_DOMAIN))
        viewModel.onSiteCreationServiceStateUpdated(SERVICE_SUCCESS)
        verify(onCartCreatedObserver).onChanged(argThat {
            assertEquals(SITE_REMOTE_ID, site.siteId)
            assertEquals(SITE_SLUG, site.url)
            domainName == PAID_DOMAIN.domainName
        })
    }

    @Test
    fun `on cart failure shows cart error`() = testWith(CART_ERROR) {
        startViewModel(SITE_CREATION_STATE.copy(domain = PAID_DOMAIN))
        viewModel.onSiteCreationServiceStateUpdated(SERVICE_SUCCESS)
        assertIs<CartError>(viewModel.uiState.value)
    }

    @Test
    fun `on service failure shows generic error`() {
        startViewModel()
        viewModel.onSiteCreationServiceStateUpdated(SERVICE_ERROR)
        assertIs<GenericError>(viewModel.uiState.value)
    }

    @Test
    fun `on restart with same paid domain reuses previous blog to create new cart`() = testWith(CART_SUCCESS) {
        val state = SITE_CREATION_STATE.copy(domain = PAID_DOMAIN)
        startViewModel(state)
        val previous = SiteModel().apply { siteId = 9L; url = "blog.wordpress.com" }
        viewModel.onSiteCreationServiceStateUpdated(SERVICE_SUCCESS.copy(payload = previous.siteId to previous.url))

        startViewModel(state.copy(result = RESULT_IN_CART))

        verify(startServiceObserver, atMost(1)).onChanged(any())
        verify(createCartUseCase).execute(
            refEq(previous),
            eq(PAID_DOMAIN.productId),
            eq(PAID_DOMAIN.domainName),
            eq(PAID_DOMAIN.supportsPrivacy),
            any()
        )
    }

    @Test
    fun `on restart with same free domain reuses it`() = testWith(CART_SUCCESS) {
        val state = SITE_CREATION_STATE
        startViewModel(state)
        val previous = SiteModel().apply { siteId = 9L; url = "blog.wordpress.com" }
        viewModel.onSiteCreationServiceStateUpdated(SERVICE_SUCCESS.copy(payload = previous.siteId to previous.url))

        startViewModel(state.copy(result = RESULT_CREATED))

        verify(startServiceObserver, atMost(1)).onChanged(any())
    }

    @Test
    fun `on restart with different paid domain emits service event`() = testWith(CART_SUCCESS) {
        startViewModel(SITE_CREATION_STATE.copy(domain = PAID_DOMAIN))
        viewModel.onSiteCreationServiceStateUpdated(SERVICE_SUCCESS)

        startViewModel(SITE_CREATION_STATE.copy(domain = PAID_DOMAIN.copy(domainName = "different")))

        verify(startServiceObserver, times(2)).onChanged(any())
    }

    @Test
    fun `on restart with free domain emits service event`() = testWith(CART_SUCCESS) {
        startViewModel(SITE_CREATION_STATE.copy(domain = PAID_DOMAIN))
        viewModel.onSiteCreationServiceStateUpdated(SERVICE_SUCCESS)
        clearInvocations(createCartUseCase)

        startViewModel(SITE_CREATION_STATE)

        verify(startServiceObserver, times(2)).onChanged(any())
        verifyNoMoreInteractions(createCartUseCase)
    }

    // region Helpers
    private fun testWith(response: OnShoppingCartCreated, block: suspend CoroutineScope.() -> Unit) = test {
        whenever(createCartUseCase.execute(any(), any(), any(), any(), any())).thenReturn(response)
        block()
    }

    private fun startViewModel(state: SiteCreationState = SITE_CREATION_STATE) {
        viewModel.start(state)
    }

    // endregion
}
