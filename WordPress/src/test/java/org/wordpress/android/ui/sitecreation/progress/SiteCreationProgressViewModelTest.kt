package org.wordpress.android.ui.sitecreation.progress

import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atMost
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.sitecreation.PAID_DOMAIN
import org.wordpress.android.ui.sitecreation.SERVICE_ERROR
import org.wordpress.android.ui.sitecreation.SERVICE_SUCCESS
import org.wordpress.android.ui.sitecreation.SITE_CREATION_STATE
import org.wordpress.android.ui.sitecreation.SITE_REMOTE_ID
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.progress.SiteCreationProgressViewModel.SiteProgressUiState
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

    private val uiStateObserver = mock<Observer<SiteProgressUiState>>()
    private val startServiceObserver = mock<Observer<StartServiceData>>()
    private val onHelpClickedObserver = mock<Observer<Unit>>()
    private val onCancelWizardClickedObserver = mock<Observer<Unit>>()
    private val onRemoteSiteCreatedObserver = mock<Observer<Long>>()

    private lateinit var viewModel: SiteCreationProgressViewModel

    @Before
    fun setUp() {
        viewModel = SiteCreationProgressViewModel(
            networkUtils,
            tracker,
            testDispatcher(),
        )
        viewModel.uiState.observeForever(uiStateObserver)
        viewModel.startCreateSiteService.observeForever(startServiceObserver)
        viewModel.onHelpClicked.observeForever(onHelpClickedObserver)
        viewModel.onCancelWizardClicked.observeForever(onCancelWizardClickedObserver)
        viewModel.onRemoteSiteCreated.observeForever(onRemoteSiteCreatedObserver)

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
    fun `on retry click emits service event with the previous result`() {
        startViewModel()
        viewModel.onSiteCreationServiceStateUpdated(SERVICE_ERROR)
        viewModel.retry()
        assertEquals(viewModel.startCreateSiteService.value?.previousState, SERVICE_ERROR.payload)
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
    fun `on service success propagates remote id`() {
        startViewModel()
        viewModel.onSiteCreationServiceStateUpdated(SERVICE_SUCCESS)
        verify(onRemoteSiteCreatedObserver).onChanged(SITE_REMOTE_ID)
    }

    @Test
    fun `on service failure shows generic error`() {
        startViewModel()
        viewModel.onSiteCreationServiceStateUpdated(SERVICE_ERROR)
        verify(uiStateObserver).onChanged(eq(GenericError))
    }

    @Test
    fun `on service failure shows error`() {
        startViewModel()
        viewModel.onSiteCreationServiceStateUpdated(SERVICE_ERROR)
        assertIs<GenericError>(viewModel.uiState.value)
    }

    // region Helpers

    private fun startViewModel(state: SiteCreationState = SITE_CREATION_STATE) {
        viewModel.start(state)
    }

    // endregion
}
