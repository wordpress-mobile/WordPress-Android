package org.wordpress.android.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.test
import org.wordpress.android.ui.WPWebViewUsageCategory
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewContentUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewFullscreenUiState.WebPreviewFullscreenErrorUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewFullscreenUiState.WebPreviewFullscreenNotAvailableUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewFullscreenProgressUiState

@RunWith(MockitoJUnitRunner::class)
class WPWebViewViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var connectionStatus: LiveData<ConnectionStatus>
    @Mock private lateinit var networkUtils: NetworkUtilsWrapper
    @Mock private lateinit var uiStateObserver: Observer<WebPreviewUiState>

    private lateinit var viewModel: WPWebViewViewModel

    @Before
    fun setUp() {
        viewModel = WPWebViewViewModel(networkUtils, connectionStatus)
        viewModel.uiState.observeForever(uiStateObserver)
        whenever(networkUtils.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `progress shown on start`() = test {
        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)
        Assertions.assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewFullscreenProgressUiState::class.java)
    }

    @Test
    fun `error shown on start when internet access not available`() = test {
        whenever(networkUtils.isNetworkAvailable()).thenReturn(false)
        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)
        Assertions.assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewFullscreenErrorUiState::class.java)
    }

    @Test
    fun `error shown on error failure`() {
        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)
        viewModel.onReceivedError()
        Assertions.assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewFullscreenErrorUiState::class.java)
    }

    @Test
    fun `show content on UrlLoaded`() {
        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)
        viewModel.onUrlLoaded()
        Assertions.assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewContentUiState::class.java)
    }

    @Test
    fun `show progress screen on retry clicked`() {
        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)
        viewModel.loadIfNecessary()
        Assertions.assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewFullscreenProgressUiState::class.java)
    }

    @Test
    fun `preview not available actionable shown when asked`() = test {
        viewModel.start(WPWebViewUsageCategory.REMOTE_PREVIEW_NOT_AVAILABLE)
        Assertions.assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewFullscreenNotAvailableUiState::class.java)
    }

    @Test
    fun `network not available actionable shown when asked`() = test {
        viewModel.start(WPWebViewUsageCategory.REMOTE_PREVIEW_NO_NETWORK)
        Assertions.assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewFullscreenErrorUiState::class.java)
    }
}
