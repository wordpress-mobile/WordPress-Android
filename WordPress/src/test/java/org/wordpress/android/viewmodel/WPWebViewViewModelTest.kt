package org.wordpress.android.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.test
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.PreviewMode
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.PreviewMode.DEFAULT
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.PreviewMode.DESKTOP
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.PreviewModeSelectorStatus
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewContentUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewFullscreenErrorUiState
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
        viewModel.start()
        assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewFullscreenProgressUiState::class.java)
    }

    @Test
    fun `error shown on start when internet access not available`() = test {
        whenever(networkUtils.isNetworkAvailable()).thenReturn(false)
        viewModel.start()
        assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewFullscreenErrorUiState::class.java)
    }

    @Test
    fun `error shown on error failure`() {
        viewModel.start()
        viewModel.onReceivedError()
        assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewFullscreenErrorUiState::class.java)
    }

    @Test
    fun `show content on UrlLoaded`() {
        viewModel.start()
        viewModel.onUrlLoaded()
        assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewContentUiState::class.java)
    }

    @Test
    fun `show progress screen on retry clicked`() {
        viewModel.start()
        viewModel.loadIfNecessary()
        assertThat(viewModel.uiState.value).isInstanceOf(WebPreviewFullscreenProgressUiState::class.java)
    }

    @Test
    fun `initially navigation is not enabled and preview mode is set to default`() {
        viewModel.start()
        assertThat(viewModel.navbarUiState.value).isNotNull()
        assertThat(viewModel.navbarUiState.value!!.backNavigationEnabled).isFalse()
        assertThat(viewModel.navbarUiState.value!!.forwardNavigationEnabled).isFalse()
        assertThat(viewModel.previewMode.value).isEqualTo(DEFAULT)
        assertThat(viewModel.previewModeSelector.value).isNotNull()
        assertThat(viewModel.previewModeSelector.value!!.isVisible).isFalse()
        assertThat(viewModel.previewModeSelector.value!!.selectedPreviewMode).isEqualTo(DEFAULT)
    }

    @Test
    fun `clicking on nav buttons navigates back and forward`() {
        viewModel.start()

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
        viewModel.start()

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
        viewModel.start()

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
        viewModel.start()

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
        viewModel.start()

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
        viewModel.start()

        var previewModeSelectorStatus: PreviewModeSelectorStatus? = null
        viewModel.previewModeSelector.observeForever {
            previewModeSelectorStatus = it
        }

        assertThat(previewModeSelectorStatus).isNotNull()
        assertThat(previewModeSelectorStatus!!.isVisible).isFalse()
        assertThat(previewModeSelectorStatus!!.selectedPreviewMode).isEqualTo(DEFAULT)

        viewModel.selectPreviewMode(DESKTOP)
        viewModel.togglePreviewModeSelectorVisibility(true)

        assertThat(previewModeSelectorStatus).isNotNull()
        assertThat(previewModeSelectorStatus!!.isVisible).isTrue()
        assertThat(previewModeSelectorStatus!!.selectedPreviewMode).isEqualTo(DESKTOP)
    }

    @Test
    fun `selecting a preview mode changes it if it's not already selected`() {
        viewModel.start()

        val selectedPreviewModes: ArrayList<PreviewMode> = ArrayList()
        viewModel.previewMode.observeForever {
            selectedPreviewModes.add(it)
        }

        // initial state
        assertThat(selectedPreviewModes.size).isEqualTo(1)
        assertThat(selectedPreviewModes[0]).isEqualTo(DEFAULT)

        viewModel.selectPreviewMode(DEFAULT)
        assertThat(selectedPreviewModes.size).isEqualTo(1)
        assertThat(selectedPreviewModes[0]).isEqualTo(DEFAULT)

        viewModel.selectPreviewMode(DESKTOP)
        assertThat(selectedPreviewModes.size).isEqualTo(2)
        assertThat(selectedPreviewModes[1]).isEqualTo(DESKTOP)
    }
}
