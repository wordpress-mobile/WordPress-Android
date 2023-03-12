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
import org.wordpress.android.ui.sitecreation.misc.CreateSiteState
import org.wordpress.android.ui.sitecreation.misc.CreateSiteState.SiteNotCreated
import org.wordpress.android.ui.sitecreation.misc.CreateSiteState.SiteNotInLocalDb
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.previews.KEY_CREATE_SITE_STATE
import org.wordpress.android.ui.sitecreation.previews.LOADING_STATE_TEXT_ANIMATION_DELAY
import org.wordpress.android.ui.sitecreation.progress.SiteProgressViewModel.SiteProgressUiState
import org.wordpress.android.ui.sitecreation.progress.SiteProgressViewModel.SiteProgressUiState.SiteProgressErrorUiState.SiteProgressConnectionErrorUiState
import org.wordpress.android.ui.sitecreation.progress.SiteProgressViewModel.SiteProgressUiState.SiteProgressErrorUiState.SiteProgressGenericErrorUiState
import org.wordpress.android.ui.sitecreation.progress.SiteProgressViewModel.SiteProgressUiState.SiteProgressLoadingUiState
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

private const val url = "test.wordpress.com"
private const val siteRemoteId = 1L
private val siteNotInLocalDb = SiteNotInLocalDb(siteRemoteId, false)
private val successServiceState = SiteCreationServiceState(SUCCESS, Pair(siteRemoteId, url))
private val errorServiceState = SiteCreationServiceState(FAILURE, SiteCreationServiceState(CREATE_SITE))
private val errorResponse = OnSiteChanged(1)
private val successResponse = OnSiteChanged(0).apply { error = SiteError(SiteErrorType.GENERIC_ERROR) }

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
    private val onCancelWizardClickedObserver = mock<Observer<CreateSiteState>>()
    private val onSiteCreationCompletedObserver = mock<Observer<CreateSiteState>>()

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
        whenever(urlUtils.removeScheme(url)).thenReturn(url)
        whenever(siteStore.getSiteBySiteId(siteRemoteId)).thenReturn(SiteModel().apply { id = 1; url = url })
    }

    @Test
    fun `on start shows progress`() = test {
        startViewModel()
        assertIs<SiteProgressUiState>(viewModel.uiState.value)
    }

    @Test
    fun `on start shows progress when restoring SiteNotCreated`() = testWith(successResponse) {
        startViewModel(SiteNotCreated)
        assertIs<SiteProgressUiState>(viewModel.uiState.value)
    }

    @Test
    fun `on start shows progress when restoring SiteNotInLocalDb`() = testWith(errorResponse) {
        startViewModel(siteNotInLocalDb)
        assertIs<SiteProgressLoadingUiState>(viewModel.uiState.value)
    }

    @Test
    fun `on start emits service event`() = test {
        startViewModel()
        assertNotNull(viewModel.startCreateSiteService.value)
    }

    @Test
    fun `on start emits service event when restoring from SiteNotCreated`() {
        startViewModel(SiteNotCreated)
        assertNotNull(viewModel.startCreateSiteService.value)
    }

    @Test
    fun `on start shows error when network is not available`() = test {
        whenever(networkUtils.isNetworkAvailable()).thenReturn(false)
        startViewModel()
        advanceUntilIdle()
        assertIs<SiteProgressConnectionErrorUiState>(viewModel.uiState.value)
    }

    @Test
    fun `on start fetches site by remote id when restoring SiteNotInLocalDb`() = test {
        startViewModel(siteNotInLocalDb)
        verify(fetchWpComSiteUseCase).fetchSiteWithRetry(siteRemoteId)
    }

    @Test
    fun `on start shows first loading text without animation`() = test {
        startViewModel()
        verify(uiStateObserver).onChanged(check<SiteProgressLoadingUiState> { !it.animate })
    }

    @Test
    fun `on start changes the loading text with delayed animation`() = test {
        startViewModel()
        advanceTimeBy(LOADING_STATE_TEXT_ANIMATION_DELAY)
        verify(uiStateObserver).onChanged(check<SiteProgressLoadingUiState> { it.animate })
    }

    @Test
    fun `on start changes the loading text with delayed animation only 4 times`() = test {
        startViewModel()
        val captor = argumentCaptor<SiteProgressLoadingUiState>()
        (1..9).forEach {
            verify(uiStateObserver, atMost(it)).onChanged(captor.capture())
            advanceTimeBy(LOADING_STATE_TEXT_ANIMATION_DELAY)
        }
        assertThat(captor.allValues.distinctBy { it.loadingTextResId }).hasSize(4)
    }

    @Test
    fun `on retry click emits service event with the previous result`() {
        startViewModel()
        viewModel.onSiteCreationServiceStateUpdated(errorServiceState)
        viewModel.retry()
        assertEquals(viewModel.startCreateSiteService.value?.previousState, errorServiceState.payload)
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
        assertEquals(viewModel.onCancelWizardClicked.value, SiteNotCreated)
    }

    @Test
    fun `on write to bundle saves the state`() {
        startViewModel()
        viewModel.writeToBundle(bundle)
        verify(bundle).putParcelable(eq(KEY_CREATE_SITE_STATE), notNull())
    }

    @Test
    fun `on service success fetches site by remote id`() = testWith(successResponse) {
        startViewModel()
        viewModel.onSiteCreationServiceStateUpdated(successServiceState)
        verify(fetchWpComSiteUseCase).fetchSiteWithRetry(siteRemoteId)
    }

    @Test
    fun `on service success will emit completion event when animations end`() = testWith(successResponse) {
        startViewModel()
        viewModel.onSiteCreationServiceStateUpdated(successServiceState)
        advanceUntilIdle()
        verify(onSiteCreationCompletedObserver).onChanged(any())
    }

    @Test
    fun `on service failure will emit completion event when animations end`() = testWith(errorResponse) {
        startViewModel()
        viewModel.onSiteCreationServiceStateUpdated(successServiceState)
        advanceUntilIdle()
        verify(onSiteCreationCompletedObserver).onChanged(any())
    }

    @Test
    fun `on service failure shows error`() {
        startViewModel()
        viewModel.onSiteCreationServiceStateUpdated(errorServiceState)
        assertIs<SiteProgressGenericErrorUiState>(viewModel.uiState.value)
    }

    // region Helpers

    private fun testWith(response: OnSiteChanged, block: suspend CoroutineScope.() -> Unit) = test {
        whenever(fetchWpComSiteUseCase.fetchSiteWithRetry(siteRemoteId)).thenReturn(response)
        block()
    }

    private fun startViewModel(restoredState: CreateSiteState? = null) {
        viewModel.start(
            SiteCreationState(
                segmentId = 1,
                siteDesign = defaultTemplateSlug,
                domain = DomainModel(url, true, "", 1)
            ),
            bundle.apply {
                restoredState?.let { whenever(getParcelable<CreateSiteState>(KEY_CREATE_SITE_STATE)) doReturn it }
            }
        )
    }

    // endregion
}
