package org.wordpress.android.ui.sitecreation.previews

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.CreateSiteState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.CreateSiteState.SiteCreationCompleted
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.CreateSiteState.SiteNotCreated
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.CreateSiteState.SiteNotInLocalDb
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewStartServiceData
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewContentUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewFullscreenErrorUiState.SitePreviewConnectionErrorUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewFullscreenErrorUiState.SitePreviewGenericErrorUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewFullscreenProgressUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewWebErrorUiState
import org.wordpress.android.ui.sitecreation.services.FetchWpComSiteUseCase
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.CREATE_SITE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.FAILURE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.SUCCESS
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.UrlUtilsWrapper

private const val SUB_DOMAIN = "test"
private const val DOMAIN = ".wordpress.com"
private const val URL = "$SUB_DOMAIN$DOMAIN"
private const val REMOTE_SITE_ID = 1L
private const val LOCAL_SITE_ID = 2
private val SITE_CREATION_STATE = SiteCreationState(1, URL)

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SitePreviewViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var siteStore: SiteStore
    @Mock private lateinit var fetchWpComUseCase: FetchWpComSiteUseCase
    @Mock private lateinit var networkUtils: NetworkUtilsWrapper
    @Mock private lateinit var urlUtils: UrlUtilsWrapper
    @Mock private lateinit var tracker: SiteCreationTracker
    @Mock private lateinit var uiStateObserver: Observer<SitePreviewUiState>
    @Mock private lateinit var startServiceObserver: Observer<SitePreviewStartServiceData>
    @Mock private lateinit var onHelpedClickedObserver: Observer<Unit>
    @Mock private lateinit var onCancelWizardClickedObserver: Observer<CreateSiteState>
    @Mock private lateinit var onOkClickedObserver: Observer<CreateSiteState>
    @Mock private lateinit var hideGetStartedObserver: Observer<Unit>
    @Mock private lateinit var preloadPreviewObserver: Observer<String>

    private lateinit var viewModel: SitePreviewViewModel
    private val coroutineDispatcher = TestCoroutineDispatcher()

    @Before
    fun setUp() {
        viewModel = SitePreviewViewModel(
                dispatcher,
                siteStore,
                fetchWpComUseCase,
                networkUtils,
                urlUtils,
                tracker,
                coroutineDispatcher,
                coroutineDispatcher
        )
        viewModel.uiState.observeForever(uiStateObserver)
        viewModel.startCreateSiteService.observeForever(startServiceObserver)
        viewModel.onHelpClicked.observeForever(onHelpedClickedObserver)
        viewModel.onCancelWizardClicked.observeForever(onCancelWizardClickedObserver)
        viewModel.onOkButtonClicked.observeForever(onOkClickedObserver)
        viewModel.hideGetStartedBar.observeForever(hideGetStartedObserver)
        viewModel.preloadPreview.observeForever(preloadPreviewObserver)
        whenever(networkUtils.isNetworkAvailable()).thenReturn(true)
        whenever(urlUtils.extractSubDomain(URL)).thenReturn(SUB_DOMAIN)
        whenever(urlUtils.addUrlSchemeIfNeeded(URL, true)).thenReturn(URL)
        whenever(siteStore.getSiteBySiteId(REMOTE_SITE_ID)).thenReturn(createLocalDbSiteModelId())
    }

    private fun <T> testWithSuccessResponse(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(fetchWpComUseCase.fetchSiteWithRetry(REMOTE_SITE_ID))
                    .thenReturn(OnSiteChanged(1))
            block()
        }
    }

    private fun <T> testWithErrorResponse(block: suspend CoroutineScope.() -> T) {
        test {
            val onSiteChanged = OnSiteChanged(0)
            onSiteChanged.error = SiteError(GENERIC_ERROR)
            whenever(fetchWpComUseCase.fetchSiteWithRetry(REMOTE_SITE_ID))
                    .thenReturn(onSiteChanged)
            block()
        }
    }

    @Test
    fun `progress shown on start`() = test {
        initViewModel()
        assertThat(viewModel.uiState.value).isInstanceOf(SitePreviewFullscreenProgressUiState::class.java)
    }

    @Test
    fun `ProgressUiState's animate field is false only for first emitted event`() = test {
        initViewModel()
        assertThat((viewModel.uiState.value as SitePreviewFullscreenProgressUiState).animate).isFalse()
        for (i in 1..100) {
            coroutineDispatcher.advanceTimeBy(LOADING_STATE_TEXT_ANIMATION_DELAY)
            assertThat((viewModel.uiState.value as SitePreviewFullscreenProgressUiState).animate).isTrue()
        }
    }

    @Test
    fun `ProgressUiState's text changes every LOADING_STATE_TEXT_ANIMATION_DELAY seconds`() {
        initViewModel()
        for (i in 1..100) {
            val lastTextId = (viewModel.uiState.value as SitePreviewFullscreenProgressUiState).loadingTextResId
            coroutineDispatcher.advanceTimeBy(LOADING_STATE_TEXT_ANIMATION_DELAY)
            assertThat((viewModel.uiState.value as SitePreviewFullscreenProgressUiState).loadingTextResId)
                    .isNotEqualTo(lastTextId)
        }
    }

    @Test
    fun `service started on start`() = test {
        initViewModel()
        assertThat(viewModel.startCreateSiteService.value).isNotNull
    }

    @Test
    fun `error shown on start when internet access not available`() {
        whenever(networkUtils.isNetworkAvailable()).thenReturn(false)
        initViewModel()
        coroutineDispatcher.advanceUntilIdle() // skip delays
        assertThat(viewModel.uiState.value).isInstanceOf(SitePreviewConnectionErrorUiState::class.java)
    }

    @Test
    fun `error shown on service failure`() {
        initViewModel()
        viewModel.onSiteCreationServiceStateUpdated(createServiceFailureState())
        assertThat(viewModel.uiState.value).isInstanceOf(SitePreviewGenericErrorUiState::class.java)
    }

    @Test
    fun `displaying error screen cancels the progress animation job`() {
        initViewModel()
        viewModel.onSiteCreationServiceStateUpdated(createServiceFailureState())
        for (i in 1..100) {
            coroutineDispatcher.advanceTimeBy(LOADING_STATE_TEXT_ANIMATION_DELAY)
            assertThat(viewModel.uiState.value).isInstanceOf(SitePreviewGenericErrorUiState::class.java)
        }
    }

    @Test
    fun `service started on retry`() {
        initViewModel()
        viewModel.onSiteCreationServiceStateUpdated(createServiceFailureState())
        viewModel.retry()
        assertThat(viewModel.startCreateSiteService.value).isNotNull
    }

    @Test
    fun `on Help click is propagated`() {
        viewModel.onHelpClicked()
        verify(onHelpedClickedObserver).onChanged(null)
    }

    @Test
    fun `on WizardCanceled click is propagated`() {
        viewModel.onCancelWizardClicked()
        assertThat(viewModel.onCancelWizardClicked.value).isEqualTo(SiteNotCreated)
    }

    @Test
    fun `on OK click is propagated`() {
        viewModel.onOkButtonClicked()
        assertThat(viewModel.onOkButtonClicked.value).isEqualTo(SiteNotCreated)
    }

    @Test
    fun `hide GetStartedBar on UrlLoaded`() {
        initViewModel()
        viewModel.onUrlLoaded()
        verify(hideGetStartedObserver).onChanged(null)
    }

    @Test
    fun `show content on UrlLoaded`() {
        initViewModel()
        viewModel.onUrlLoaded()
        assertThat(viewModel.uiState.value).isInstanceOf(SitePreviewContentUiState::class.java)
    }

    @Test
    fun `displaying content cancels the progress animation job`() {
        initViewModel()
        viewModel.onUrlLoaded()
        for (i in 1..100) {
            coroutineDispatcher.advanceTimeBy(LOADING_STATE_TEXT_ANIMATION_DELAY)
            assertThat(viewModel.uiState.value).isInstanceOf(SitePreviewContentUiState::class.java)
        }
    }

    @Test
    fun `show webview empty screen on WebViewError`() {
        initViewModel()
        viewModel.onWebViewError()
        assertThat(viewModel.uiState.value).isInstanceOf(SitePreviewWebErrorUiState::class.java)
    }

    @Test
    fun `start pre-loading WebView on service success`() = testWithSuccessResponse {
        initViewModel()
        viewModel.onSiteCreationServiceStateUpdated(createServiceSuccessState())
        assertThat(viewModel.preloadPreview.value).isEqualTo(URL)
    }

    @Test
    fun `fetch newly created SiteModel on service success`() = testWithSuccessResponse {
        initViewModel()
        viewModel.onSiteCreationServiceStateUpdated(createServiceSuccessState())
        verify(fetchWpComUseCase).fetchSiteWithRetry(REMOTE_SITE_ID)
    }

    @Test
    fun `CreateSiteState is SiteNotCreated on init`() {
        initViewModel()
        assertThat(getCreateSiteState()).isEqualTo(SiteNotCreated)
    }

    @Test
    fun `CreateSiteState is SiteCreationCompleted on fetchFromRemote success`() = testWithSuccessResponse {
        initViewModel()
        viewModel.onSiteCreationServiceStateUpdated(createServiceSuccessState())
        assertThat(getCreateSiteState()).isEqualTo(SiteCreationCompleted(LOCAL_SITE_ID))
    }

    @Test
    fun `CreateSiteState is NotInLocalDb on fetchFromRemote failure`() = testWithErrorResponse {
        initViewModel()
        viewModel.onSiteCreationServiceStateUpdated(createServiceSuccessState())
        assertThat(getCreateSiteState()).isEqualTo(SiteNotInLocalDb(REMOTE_SITE_ID))
    }

    private fun initViewModel() {
        viewModel.start(SITE_CREATION_STATE)
    }

    private fun createServiceFailureState(): SiteCreationServiceState {
        val stateBeforeFailure = SiteCreationServiceState(CREATE_SITE)
        return SiteCreationServiceState(FAILURE, stateBeforeFailure)
    }

    private fun createServiceSuccessState(): SiteCreationServiceState {
        return SiteCreationServiceState(SUCCESS, REMOTE_SITE_ID)
    }

    private fun createLocalDbSiteModelId(): SiteModel {
        val localDbSiteModel = SiteModel()
        localDbSiteModel.id = LOCAL_SITE_ID
        return localDbSiteModel
    }

    /**
     * `createSiteState` is a private property -> get its value using `onOkButtonClicked`
     */
    private fun getCreateSiteState(): CreateSiteState? {
        viewModel.onOkButtonClicked()
        return viewModel.onOkButtonClicked.value
    }
}
