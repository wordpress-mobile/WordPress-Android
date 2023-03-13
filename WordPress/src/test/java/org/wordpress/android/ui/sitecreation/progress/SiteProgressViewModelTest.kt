package org.wordpress.android.ui.sitecreation.progress

import android.os.Bundle
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atMost
import org.mockito.kotlin.check
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.notNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.ui.sitecreation.domains.DomainModel
import org.wordpress.android.ui.sitecreation.SiteCreationResult
import org.wordpress.android.ui.sitecreation.SiteCreationResult.NotCreated
import org.wordpress.android.ui.sitecreation.SiteCreationResult.NotInLocalDb
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.previews.KEY_CREATE_SITE_STATE
import org.wordpress.android.ui.sitecreation.previews.LOADING_STATE_TEXT_ANIMATION_DELAY
import org.wordpress.android.ui.sitecreation.progress.SiteProgressViewModel.SiteProgressUiState
import org.wordpress.android.ui.sitecreation.progress.SiteProgressViewModel.SiteProgressUiState.Error.ConnectionError
import org.wordpress.android.ui.sitecreation.progress.SiteProgressViewModel.SiteProgressUiState.Error.GenericError
import org.wordpress.android.ui.sitecreation.progress.SiteProgressViewModel.SiteProgressUiState.Loading
import org.wordpress.android.ui.sitecreation.progress.SiteProgressViewModel.StartServiceData
import org.wordpress.android.ui.sitecreation.services.FetchWpComSiteUseCase
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.CREATE_SITE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.FAILURE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.SUCCESS
import org.wordpress.android.ui.sitecreation.theme.defaultTemplateSlug
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.UrlUtilsWrapper
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

private const val URL = "test.wordpress.com"
private const val SITE_REMOTE_ID = 1L
private val RESULT_NOT_IN_LOCAL_DB = NotInLocalDb(SITE_REMOTE_ID, false)
private val SUCCESS_SERVICE_STATE = SiteCreationServiceState(SUCCESS, Pair(SITE_REMOTE_ID, URL))
private val ERROR_SERVICE_STATE = SiteCreationServiceState(FAILURE, SiteCreationServiceState(CREATE_SITE))
private val SUCCESS_RESPONSE = OnSiteChanged(1)
private val ERROR_RESPONSE = OnSiteChanged(0).apply { error = SiteError(SiteErrorType.GENERIC_ERROR) }

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteProgressViewModelTest : BaseUnitTest() {
    private var dispatcher = mock<Dispatcher>()
    private var siteStore = mock<SiteStore>()
    private var fetchWpComSiteUseCase = mock<FetchWpComSiteUseCase>()
    private var networkUtils = mock<NetworkUtilsWrapper>()
    private var urlUtils = mock<UrlUtilsWrapper>()
    private var tracker = mock<SiteCreationTracker>()

    private val uiStateObserver = mock<Observer<SiteProgressUiState>>()
    private val startServiceObserver = mock<Observer<StartServiceData>>()
    private val onHelpedClickedObserver = mock<Observer<Unit>>()
    private val onCancelWizardClickedObserver = mock<Observer<SiteCreationResult>>()
    private val onSiteCreationCompletedObserver = mock<Observer<SiteCreationResult>>()

    private val bundle = mock<Bundle>()

    private lateinit var viewModel: SiteProgressViewModel

    @Before
    fun setUp() {
        viewModel = SiteProgressViewModel(
            dispatcher = dispatcher,
            siteStore = siteStore,
            fetchWpComSiteUseCase = fetchWpComSiteUseCase,
            networkUtils = networkUtils,
            urlUtils = urlUtils,
            tracker = tracker,
            bgDispatcher = testDispatcher(),
            mainDispatcher = testDispatcher()
        )
        viewModel.uiState.observeForever(uiStateObserver)
        viewModel.startCreateSiteService.observeForever(startServiceObserver)
        viewModel.onHelpClicked.observeForever(onHelpedClickedObserver)
        viewModel.onCancelWizardClicked.observeForever(onCancelWizardClickedObserver)
        viewModel.onSiteCreationCompleted.observeForever(onSiteCreationCompletedObserver)

        whenever(networkUtils.isNetworkAvailable()).thenReturn(true)
        whenever(urlUtils.removeScheme(URL)).thenReturn(URL)
        whenever(siteStore.getSiteBySiteId(SITE_REMOTE_ID)).thenReturn(SiteModel().apply { id = 1; url = URL })
    }

    @Test
    fun `on start shows progress`() = test {
        startViewModel()
        assertIs<SiteProgressUiState>(viewModel.uiState.value)
    }

    @Test
    fun `on start shows progress when restoring SiteNotCreated`() = testWith(SUCCESS_RESPONSE) {
        startViewModel(NotCreated)
        assertIs<SiteProgressUiState>(viewModel.uiState.value)
    }

    @Test
    fun `on start shows progress when restoring SiteNotInLocalDb`() = testWith(ERROR_RESPONSE) {
        startViewModel(RESULT_NOT_IN_LOCAL_DB)
        assertIs<Loading>(viewModel.uiState.value)
    }

    @Test
    fun `on start emits service event`() = test {
        startViewModel()
        assertNotNull(viewModel.startCreateSiteService.value)
    }

    @Test
    fun `on start emits service event when restoring from SiteNotCreated`() {
        startViewModel(NotCreated)
        assertNotNull(viewModel.startCreateSiteService.value)
    }

    @Test
    fun `on start shows error when network is not available`() = test {
        whenever(networkUtils.isNetworkAvailable()).thenReturn(false)
        startViewModel()
        advanceUntilIdle()
        assertIs<ConnectionError>(viewModel.uiState.value)
    }

    @Test
    fun `on start fetches site by remote id when restoring SiteNotInLocalDb`() = testWith(SUCCESS_RESPONSE) {
        startViewModel(RESULT_NOT_IN_LOCAL_DB)
        verify(fetchWpComSiteUseCase).fetchSiteWithRetry(SITE_REMOTE_ID)
    }

    @Test
    fun `on start shows first loading text without animation`() = test {
        startViewModel()
        verify(uiStateObserver).onChanged(check<Loading> { !it.animate })
    }

    @Test
    fun `on start changes the loading text with delayed animation`() = test {
        startViewModel()
        advanceTimeBy(LOADING_STATE_TEXT_ANIMATION_DELAY)
        verify(uiStateObserver).onChanged(check<Loading> { it.animate })
    }

    @Test
    fun `on start changes the loading text with delayed animation only 4 times`() = test {
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
        viewModel.onSiteCreationServiceStateUpdated(ERROR_SERVICE_STATE)
        viewModel.retry()
        assertEquals(viewModel.startCreateSiteService.value?.previousState, ERROR_SERVICE_STATE.payload)
    }

    @Test
    fun `on help click is propagate`() {
        startViewModel()
        viewModel.onHelpClicked()
        verify(onHelpedClickedObserver).onChanged(isNull())
    }

    @Test
    fun `on cancel wizard click is propagated`() {
        viewModel.onCancelWizardClicked()
        assertEquals(viewModel.onCancelWizardClicked.value, NotCreated)
    }

    @Test
    fun `on write to bundle saves the state`() {
        startViewModel()
        viewModel.writeToBundle(bundle)
        verify(bundle).putParcelable(eq(KEY_CREATE_SITE_STATE), notNull())
    }

    @Test
    fun `on service success fetches site by remote id`() = testWith(SUCCESS_RESPONSE) {
        startViewModel()
        viewModel.onSiteCreationServiceStateUpdated(SUCCESS_SERVICE_STATE)
        verify(fetchWpComSiteUseCase).fetchSiteWithRetry(SITE_REMOTE_ID)
    }

    @Test
    fun `on service success will emit completion event when animations end`() = testWith(SUCCESS_RESPONSE) {
        startViewModel()
        viewModel.onSiteCreationServiceStateUpdated(SUCCESS_SERVICE_STATE)
        advanceUntilIdle()
        verify(onSiteCreationCompletedObserver).onChanged(any())
    }

    @Test
    fun `on service failure will emit completion event when animations end`() = testWith(ERROR_RESPONSE) {
        startViewModel()
        viewModel.onSiteCreationServiceStateUpdated(SUCCESS_SERVICE_STATE)
        advanceUntilIdle()
        verify(onSiteCreationCompletedObserver).onChanged(any())
    }

    @Test
    fun `on service failure shows error`() {
        startViewModel()
        viewModel.onSiteCreationServiceStateUpdated(ERROR_SERVICE_STATE)
        assertIs<GenericError>(viewModel.uiState.value)
    }

    // region Helpers

    private fun testWith(response: OnSiteChanged, block: suspend CoroutineScope.() -> Unit) = test {
        whenever(fetchWpComSiteUseCase.fetchSiteWithRetry(SITE_REMOTE_ID)).thenReturn(response)
        block()
    }

    private fun startViewModel(restoredState: SiteCreationResult? = null) {
        viewModel.start(
            SiteCreationState(
                segmentId = 1,
                siteDesign = defaultTemplateSlug,
                domain = DomainModel(URL, true, "", 1)
            ),
            bundle.apply {
                restoredState?.let { whenever(getParcelable<SiteCreationResult>(KEY_CREATE_SITE_STATE)) doReturn it }
            }
        )
    }

    // endregion
}
