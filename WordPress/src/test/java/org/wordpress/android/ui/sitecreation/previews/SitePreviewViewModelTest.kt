package org.wordpress.android.ui.sitecreation.previews

import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.ui.sitecreation.domains.DomainModel
import org.wordpress.android.ui.sitecreation.misc.CreateSiteState
import org.wordpress.android.ui.sitecreation.misc.CreateSiteState.SiteCreationCompleted
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewContentUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewWebErrorUiState
import org.wordpress.android.ui.sitecreation.theme.defaultTemplateSlug
import org.wordpress.android.util.UrlUtilsWrapper

private const val SUB_DOMAIN = "test"
private const val URL = "$SUB_DOMAIN.wordpress.com"
private val DOMAIN = DomainModel(URL, true, "", 1)
private val SITE_CREATION_STATE = SiteCreationState(segmentId = 1, siteDesign = defaultTemplateSlug, domain = DOMAIN)

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SitePreviewViewModelTest : BaseUnitTest() {

    @Mock
    private lateinit var urlUtils: UrlUtilsWrapper

    @Mock
    private lateinit var tracker: SiteCreationTracker

    @Mock
    private lateinit var uiStateObserver: Observer<SitePreviewUiState>

    @Mock
    private lateinit var onHelpedClickedObserver: Observer<Unit>

    @Mock
    private lateinit var onOkClickedObserver: Observer<CreateSiteState>

    @Mock
    private lateinit var preloadPreviewObserver: Observer<String>

    private lateinit var viewModel: SitePreviewViewModel

    @Before
    fun setUp() {
        viewModel = SitePreviewViewModel(
            urlUtils,
            tracker,
            testDispatcher(),
            testDispatcher()
        )
        viewModel.uiState.observeForever(uiStateObserver)
        viewModel.onHelpClicked.observeForever(onHelpedClickedObserver)
        viewModel.onOkButtonClicked.observeForever(onOkClickedObserver)
        viewModel.preloadPreview.observeForever(preloadPreviewObserver)
        whenever(urlUtils.extractSubDomain(URL)).thenReturn(SUB_DOMAIN)
        whenever(urlUtils.addUrlSchemeIfNeeded(URL, true)).thenReturn(URL)
    }

    @Test
    fun `show content on UrlLoaded`() {
        initViewModel()
        viewModel.onUrlLoaded()
        assertThat(viewModel.uiState.value).isInstanceOf(SitePreviewContentUiState::class.java)
    }

    @Test
    fun `displaying content cancels the progress animation job`() = test {
        initViewModel()
        viewModel.onUrlLoaded()
        (1..100).forEach {
            advanceTimeBy(LOADING_STATE_TEXT_ANIMATION_DELAY)
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
    fun `start pre-loading WebView when restoring from SiteCreationCompleted state`() {
        initViewModel(createSiteState = SiteCreationCompleted(2, false, URL))

        assertThat(viewModel.preloadPreview.value).isEqualTo(URL)
    }

    private fun initViewModel(
        siteCreationState: SiteCreationState = SITE_CREATION_STATE,
        createSiteState: CreateSiteState = mock(),
    ) {
        viewModel.start(siteCreationState, createSiteState)
    }
}
