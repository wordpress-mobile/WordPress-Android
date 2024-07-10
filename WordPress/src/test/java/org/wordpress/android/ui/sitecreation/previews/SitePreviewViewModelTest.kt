package org.wordpress.android.ui.sitecreation.previews

import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.ui.sitecreation.FETCH_ERROR
import org.wordpress.android.ui.sitecreation.FETCH_SUCCESS
import org.wordpress.android.ui.sitecreation.RESULT_COMPLETED
import org.wordpress.android.ui.sitecreation.RESULT_CREATED
import org.wordpress.android.ui.sitecreation.RESULT_NOT_IN_LOCAL_DB
import org.wordpress.android.ui.sitecreation.SITE_CREATION_STATE
import org.wordpress.android.ui.sitecreation.SITE_MODEL
import org.wordpress.android.ui.sitecreation.SITE_REMOTE_ID
import org.wordpress.android.ui.sitecreation.SUB_DOMAIN
import org.wordpress.android.ui.sitecreation.SiteCreationResult
import org.wordpress.android.ui.sitecreation.SiteCreationResult.Created
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.ui.sitecreation.URL
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewContentUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewWebErrorUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SiteNotCreatedErrorUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SiteNotFoundInDbUiState
import org.wordpress.android.ui.sitecreation.progress.LOADING_STATE_TEXT_ANIMATION_DELAY
import org.wordpress.android.ui.sitecreation.services.FetchWpComSiteUseCase
import org.wordpress.android.util.UrlUtilsWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SitePreviewViewModelTest : BaseUnitTest() {
    private var dispatcher = mock<Dispatcher>()
    private var siteStore = mock<SiteStore>()
    private var fetchWpComSiteUseCase = mock<FetchWpComSiteUseCase>()

    @Mock
    private lateinit var urlUtils: UrlUtilsWrapper

    @Mock
    private lateinit var tracker: SiteCreationTracker

    @Mock
    private lateinit var uiStateObserver: Observer<SitePreviewUiState>

    @Mock
    private lateinit var onOkClickedObserver: Observer<Created?>

    @Mock
    private lateinit var preloadPreviewObserver: Observer<String>

    private lateinit var viewModel: SitePreviewViewModel

    @Before
    fun setUp() {
        viewModel = SitePreviewViewModel(
            dispatcher,
            siteStore,
            fetchWpComSiteUseCase,
            urlUtils,
            tracker,
            testDispatcher(),
            testDispatcher()
        )
        viewModel.uiState.observeForever(uiStateObserver)
        viewModel.onOkButtonClicked.observeForever(onOkClickedObserver)
        viewModel.preloadPreview.observeForever(preloadPreviewObserver)

        whenever(urlUtils.extractSubDomain(any())).thenReturn(SUB_DOMAIN)
        whenever(urlUtils.addUrlSchemeIfNeeded(any(), eq(true))).thenReturn(URL)
        whenever(siteStore.getSiteBySiteId(SITE_REMOTE_ID)).thenReturn(SITE_MODEL)
    }

    @Test
    fun `on start show error when result is not created`() = testWith(FETCH_SUCCESS) {
        startViewModel(SITE_CREATION_STATE.copy(result = SiteCreationResult.NotCreated))
        assertThat(viewModel.uiState.value).isInstanceOf(SiteNotCreatedErrorUiState::class.java)
    }

    @Test
    fun `on start fetches site by remote id when result is created`() = testWith(FETCH_SUCCESS) {
        startViewModel(SITE_CREATION_STATE.copy(result = RESULT_NOT_IN_LOCAL_DB))
        verify(fetchWpComSiteUseCase).fetchSiteWithRetry(SITE_REMOTE_ID)
    }

    @Test
    fun `on start if site is created but cannot be retrieved from fb fails show error`() = testWith(FETCH_SUCCESS) {
        whenever(siteStore.getSiteBySiteId(SITE_REMOTE_ID)).thenReturn(null)
        startViewModel(SITE_CREATION_STATE.copy(result = RESULT_NOT_IN_LOCAL_DB))
        assertThat(viewModel.uiState.value).isInstanceOf(SiteNotFoundInDbUiState::class.java)
    }

    @Test
    fun `on start does not show preview when fetching fails`() = testWith(FETCH_ERROR) {
        startViewModel()
        verify(siteStore, never()).getSiteBySiteId(SITE_REMOTE_ID)
        viewModel.onOkButtonClicked()
        verify(uiStateObserver, never()).onChanged(isA<SitePreviewContentUiState>())
    }

    @Test
    fun `show content on UrlLoaded`() {
        startViewModel()
        viewModel.onUrlLoaded()
        assertThat(viewModel.uiState.value).isInstanceOf(SitePreviewContentUiState::class.java)
    }

    @Test
    fun `displaying content cancels the progress animation job`() = test {
        startViewModel()
        viewModel.onUrlLoaded()
        repeat(100) {
            advanceTimeBy(LOADING_STATE_TEXT_ANIMATION_DELAY)
            assertThat(viewModel.uiState.value).isInstanceOf(SitePreviewContentUiState::class.java)
        }
    }

    @Test
    fun `show webview empty screen on WebViewError`() {
        startViewModel()
        viewModel.onWebViewError()
        assertThat(viewModel.uiState.value).isInstanceOf(SitePreviewWebErrorUiState::class.java)
    }

    @Test
    fun `on start preloads the preview when result is Completed`() {
        startViewModel(SITE_CREATION_STATE.copy(result = RESULT_COMPLETED))
        assertThat(viewModel.preloadPreview.value).isEqualTo(URL)
    }

    @Test
    fun `on ok button click is propagated`() = testWith(FETCH_SUCCESS) {
        startViewModel()
        viewModel.onOkButtonClicked()
        verify(onOkClickedObserver).onChanged(anyOrNull())
    }

    // region Helpers

    private fun testWith(response: OnSiteChanged, block: suspend CoroutineScope.() -> Unit) = test {
        whenever(fetchWpComSiteUseCase.fetchSiteWithRetry(SITE_REMOTE_ID)).thenReturn(response)
        block()
    }

    private fun startViewModel(state: SiteCreationState = SITE_CREATION_STATE.copy(result = RESULT_CREATED)) {
        whenever(RESULT_CREATED.site).thenReturn(SITE_MODEL)
        viewModel.start(state)
    }

    // endregion
}
