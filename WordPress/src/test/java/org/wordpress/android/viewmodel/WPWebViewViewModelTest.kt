package org.wordpress.android.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.test
import org.wordpress.android.ui.PreviewMode
import org.wordpress.android.ui.PreviewMode.DESKTOP
import org.wordpress.android.ui.PreviewMode.MOBILE
import org.wordpress.android.ui.PreviewMode.TABLET
import org.wordpress.android.ui.WPWebViewUsageCategory
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.PreviewModeSelectorStatus
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewContentUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewFullscreenProgressUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewFullscreenUiState.WebPreviewFullscreenErrorUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewFullscreenUiState.WebPreviewFullscreenNotAvailableUiState

@RunWith(MockitoJUnitRunner::class)
class WPWebViewViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var connectionStatus: LiveData<ConnectionStatus>
    @Mock private lateinit var networkUtils: NetworkUtilsWrapper
    @Mock private lateinit var uiStateObserver: Observer<WebPreviewUiState>
    @Mock lateinit var displayUtilsWrapper: DisplayUtilsWrapper
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private lateinit var viewModel: WPWebViewViewModel

    @Before
    fun setUp() {
        viewModel = WPWebViewViewModel(displayUtilsWrapper, networkUtils, analyticsTrackerWrapper, connectionStatus)
        viewModel.uiState.observeForever(uiStateObserver)
        whenever(networkUtils.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `progress shown on start`() = test {
        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)
        assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewFullscreenProgressUiState::class.java)
    }

    @Test
    fun `error shown on start when internet access not available`() = test {
        whenever(networkUtils.isNetworkAvailable()).thenReturn(false)
        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)
        assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewFullscreenErrorUiState::class.java)
    }

    @Test
    fun `error shown on error failure`() {
        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)
        viewModel.onReceivedError()
        assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewFullscreenErrorUiState::class.java)
    }

    @Test
    fun `show content on UrlLoaded and enable preview mode switch`() {
        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)
        viewModel.onUrlLoaded()
        assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewContentUiState::class.java)
        assertThat(viewModel.previewModeSelector.value!!.isEnabled).isTrue()
    }

    @Test
    fun `show progress screen on retry clicked`() {
        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)
        viewModel.loadIfNecessary()
        assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewFullscreenProgressUiState::class.java)
    }

    @Test
    fun `on mobile initially navigation is not enabled and preview mode is set to mobile and disabled`() {
        whenever(displayUtilsWrapper.isTablet()).thenReturn(false)

        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)
        assertThat(viewModel.navbarUiState.value).isNotNull()
        assertThat(viewModel.navbarUiState.value!!.backNavigationEnabled).isFalse()
        assertThat(viewModel.navbarUiState.value!!.forwardNavigationEnabled).isFalse()
        assertThat(viewModel.navbarUiState.value!!.previewModeHintVisible).isFalse()
        assertThat(viewModel.previewMode.value).isEqualTo(MOBILE)
        assertThat(viewModel.previewModeSelector.value).isNotNull()
        assertThat(viewModel.previewModeSelector.value!!.isVisible).isFalse()
        assertThat(viewModel.previewModeSelector.value!!.isEnabled).isFalse()
        assertThat(viewModel.previewModeSelector.value!!.selectedPreviewMode).isEqualTo(MOBILE)
    }

    @Test
    fun `on tablet initially navigation is not enabled and preview mode is set to tablet and disabled`() {
        whenever(displayUtilsWrapper.isTablet()).thenReturn(true)

        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)
        assertThat(viewModel.navbarUiState.value).isNotNull()
        assertThat(viewModel.navbarUiState.value!!.backNavigationEnabled).isFalse()
        assertThat(viewModel.navbarUiState.value!!.forwardNavigationEnabled).isFalse()
        assertThat(viewModel.navbarUiState.value!!.previewModeHintVisible).isFalse()
        assertThat(viewModel.previewMode.value).isEqualTo(TABLET)
        assertThat(viewModel.previewModeSelector.value).isNotNull()
        assertThat(viewModel.previewModeSelector.value!!.isVisible).isFalse()
        assertThat(viewModel.previewModeSelector.value!!.isEnabled).isFalse()
        assertThat(viewModel.previewModeSelector.value!!.selectedPreviewMode).isEqualTo(TABLET)
    }

    @Test
    fun `clicking on nav buttons navigates back and forward`() {
        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)

        // navigate forward
        var forwardNavigationWasCalled = false
        viewModel.navigateForward.observeForever {
            forwardNavigationWasCalled = true
        }

        assertThat(forwardNavigationWasCalled).isFalse()
        viewModel.navigateForward()
        assertThat(forwardNavigationWasCalled).isTrue()

        // navigate back

        var backNavigationWasCalled = false
        viewModel.navigateBack.observeForever {
            backNavigationWasCalled = true
        }

        assertThat(backNavigationWasCalled).isFalse()
        viewModel.navigateBack()
        assertThat(backNavigationWasCalled).isTrue()
    }

    @Test
    fun `toggling nav buttons enabled state enables and disables them`() {
        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)

        var isForwardButtonEnabled = false
        var isBackButtonEnabled = false

        viewModel.navbarUiState.observeForever {
            isForwardButtonEnabled = it.forwardNavigationEnabled
            isBackButtonEnabled = it.backNavigationEnabled
        }

        assertThat(isForwardButtonEnabled).isFalse()
        assertThat(isBackButtonEnabled).isFalse()

        viewModel.toggleBackNavigation(true)
        assertThat(isForwardButtonEnabled).isFalse()
        assertThat(isBackButtonEnabled).isTrue()

        viewModel.toggleBackNavigation(false)
        assertThat(isForwardButtonEnabled).isFalse()
        assertThat(isBackButtonEnabled).isFalse()

        viewModel.toggleForwardNavigation(true)
        assertThat(isForwardButtonEnabled).isTrue()
        assertThat(isBackButtonEnabled).isFalse()

        viewModel.toggleForwardNavigation(false)
        assertThat(isForwardButtonEnabled).isFalse()
        assertThat(isBackButtonEnabled).isFalse()
    }

    @Test
    fun `clicking on share button starts sharing`() {
        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)

        var shareWasCalled = false
        viewModel.share.observeForever {
            shareWasCalled = true
        }

        assertThat(shareWasCalled).isFalse()
        viewModel.share()
        assertThat(shareWasCalled).isTrue()
    }

    @Test
    fun `clicking on external browser button opens page in external browser`() {
        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)

        var externalBrowserOpened = false
        viewModel.openExternalBrowser.observeForever {
            externalBrowserOpened = true
        }

        assertThat(externalBrowserOpened).isFalse()
        viewModel.openPageInExternalBrowser()
        assertThat(externalBrowserOpened).isTrue()
    }

    @Test
    fun `clicking on preview mode button toggles preview mode selector`() {
        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)

        var previewModeSelectorStatus: PreviewModeSelectorStatus? = null
        viewModel.previewModeSelector.observeForever {
            previewModeSelectorStatus = it
        }

        assertThat(previewModeSelectorStatus).isNotNull()
        assertThat(previewModeSelectorStatus!!.isVisible).isFalse()

        viewModel.togglePreviewModeSelectorVisibility(true)
        assertThat(previewModeSelectorStatus).isNotNull()
        assertThat(previewModeSelectorStatus!!.isVisible).isTrue()

        viewModel.togglePreviewModeSelectorVisibility(false)
        assertThat(previewModeSelectorStatus).isNotNull()
        assertThat(previewModeSelectorStatus!!.isVisible).isFalse()
    }

    @Test
    fun `selected preview mode is reflected in preview mode selector`() {
        whenever(displayUtilsWrapper.isTablet()).thenReturn(false)

        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)

        var previewModeSelectorStatus: PreviewModeSelectorStatus? = null
        viewModel.previewModeSelector.observeForever {
            previewModeSelectorStatus = it
        }

        assertThat(previewModeSelectorStatus).isNotNull()
        assertThat(previewModeSelectorStatus!!.isVisible).isFalse()
        assertThat(previewModeSelectorStatus!!.selectedPreviewMode).isEqualTo(MOBILE)

        viewModel.selectPreviewMode(DESKTOP)
        viewModel.togglePreviewModeSelectorVisibility(true)

        assertThat(previewModeSelectorStatus).isNotNull()
        assertThat(previewModeSelectorStatus!!.isVisible).isTrue()
        assertThat(previewModeSelectorStatus!!.selectedPreviewMode).isEqualTo(DESKTOP)
    }

    @Test
    fun `selecting preview mode triggers progress indicator`() {
        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)

        viewModel.selectPreviewMode(DESKTOP)
        assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewFullscreenProgressUiState::class.java)

        viewModel.onUrlLoaded()
        assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewContentUiState::class.java)
    }

    @Test
    fun `selecting a preview mode changes it if it's not already selected`() {
        whenever(displayUtilsWrapper.isTablet()).thenReturn(false)

        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)

        val selectedPreviewModes: ArrayList<PreviewMode> = ArrayList()
        viewModel.previewMode.observeForever {
            selectedPreviewModes.add(it)
        }

        // initial state
        assertThat(selectedPreviewModes.size).isEqualTo(1)
        assertThat(selectedPreviewModes[0]).isEqualTo(MOBILE)

        viewModel.selectPreviewMode(MOBILE)
        assertThat(selectedPreviewModes.size).isEqualTo(1)
        assertThat(selectedPreviewModes[0]).isEqualTo(MOBILE)

        viewModel.selectPreviewMode(DESKTOP)
        assertThat(selectedPreviewModes.size).isEqualTo(2)
        assertThat(selectedPreviewModes[1]).isEqualTo(DESKTOP)
    }

    @Test
    fun `selecting desktop preview mode shows hint label`() {
        viewModel.start(WPWebViewUsageCategory.WEBVIEW_STANDARD)

        var isDesktopPreviewModeHintVisible = false
        viewModel.navbarUiState.observeForever {
            isDesktopPreviewModeHintVisible = it.previewModeHintVisible
        }

        assertThat(isDesktopPreviewModeHintVisible).isFalse()

        viewModel.selectPreviewMode(DESKTOP)
        assertThat(isDesktopPreviewModeHintVisible).isTrue()

        viewModel.selectPreviewMode(MOBILE)
        assertThat(isDesktopPreviewModeHintVisible).isFalse()
    }

    @Test
    fun `preview not available actionable shown when asked`() = test {
        viewModel.start(WPWebViewUsageCategory.REMOTE_PREVIEW_NOT_AVAILABLE)
        assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewFullscreenNotAvailableUiState::class.java)
    }

    @Test
    fun `network not available actionable shown when asked`() = test {
        viewModel.start(WPWebViewUsageCategory.REMOTE_PREVIEW_NO_NETWORK)
        assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewFullscreenErrorUiState::class.java)
    }
}
