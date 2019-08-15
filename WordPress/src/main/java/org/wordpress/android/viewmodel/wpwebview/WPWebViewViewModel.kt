package org.wordpress.android.viewmodel.wpwebview

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import org.wordpress.android.util.CrashLoggingUtils
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.helpers.ConnectionStatus.AVAILABLE
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.PreviewMode.DEFAULT
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.PreviewMode.DESKTOP
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewContentUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewFullscreenErrorUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewFullscreenProgressUiState
import javax.inject.Inject

class WPWebViewViewModel
@Inject constructor(
    private val networkUtils: NetworkUtilsWrapper,
    connectionStatus: LiveData<ConnectionStatus>
) : ViewModel(), LifecycleOwner {
    private var isStarted = false

    private val _uiState: MutableLiveData<WebPreviewUiState> = MutableLiveData()
    val uiState: LiveData<WebPreviewUiState> = _uiState
    private val _loadNeeded = SingleLiveEvent<Boolean>()
    val loadNeeded: LiveData<Boolean> = _loadNeeded

    private val _navigateBack = SingleLiveEvent<Unit>()
    val navigateBack: LiveData<Unit> = _navigateBack

    private val _navigateForward = SingleLiveEvent<Unit>()
    val navigateForward: LiveData<Unit> = _navigateForward

    private val _share = SingleLiveEvent<Unit>()
    val share: LiveData<Unit> = _share

    private val _openInExternalBrowser = SingleLiveEvent<Unit>()
    val openExternalBrowser: LiveData<Unit> = _openInExternalBrowser

    private val _previewModeSelector = MutableLiveData<PreviewModeSelectorStatus>()
    val previewModeSelector: LiveData<PreviewModeSelectorStatus> = _previewModeSelector

    private val _navbarUiState: MutableLiveData<NavBarUiState> = MutableLiveData()
    val navbarUiState: LiveData<NavBarUiState> = _navbarUiState

    private val _previewMode: MutableLiveData<PreviewMode> = MutableLiveData()
    val previewMode: LiveData<PreviewMode> = _previewMode

    private val lifecycleRegistry = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    init {
        lifecycleRegistry.markState(Lifecycle.State.CREATED)
        connectionStatus.observe(this, Observer {
            if (it == AVAILABLE) {
                loadIfNecessary()
            }
        })
    }

    fun start() {
        if (isStarted) {
            return
        }
        isStarted = true

        _navbarUiState.value = NavBarUiState(
                forwardNavigationEnabled = false,
                backNavigationEnabled = false,
                desktopPreviewHintVisible = false
        )

        _previewMode.value = DEFAULT
        _previewModeSelector.value = PreviewModeSelectorStatus(
                isVisible = false,
                isEnabled = false,
                selectedPreviewMode = DEFAULT
        )

        // If there is no internet show the error screen
        if (networkUtils.isNetworkAvailable()) {
            updateUiState(WebPreviewFullscreenProgressUiState)
        } else {
            updateUiState(WebPreviewFullscreenErrorUiState)
        }
        lifecycleRegistry.markState(Lifecycle.State.STARTED)
    }

    override fun onCleared() {
        lifecycleRegistry.markState(Lifecycle.State.DESTROYED)
        super.onCleared()
    }

    private fun updateUiState(uiState: WebPreviewUiState) {
        _uiState.value = uiState
    }

    /**
     * Update the ui state if the Loading or Error screen is being shown.
     * In other words don't update it after a configuration change.
     */
    fun onUrlLoaded() {
        if (uiState.value !is WebPreviewContentUiState) {
            updateUiState(WebPreviewContentUiState)
            _previewModeSelector.value = _previewModeSelector.value?.copy(isEnabled = true)
        }
        _loadNeeded.value = false
    }

    /**
     * Update the ui state if the Loading or Success screen is being shown.
     */
    fun onReceivedError() {
        if (uiState.value is WebPreviewContentUiState) {
            CrashLoggingUtils.log(
                    IllegalStateException(
                            "WPWebViewViewModel.onReceivedError() called with uiState WebPreviewContentUiState"
                    )
            )
            return
        }
        if (uiState.value !is WebPreviewFullscreenErrorUiState) {
            updateUiState(WebPreviewFullscreenErrorUiState)
        }
        _loadNeeded.value = false
    }

    fun loadIfNecessary() {
        if (uiState.value !is WebPreviewFullscreenProgressUiState && uiState.value !is WebPreviewContentUiState) {
            updateUiState(WebPreviewFullscreenProgressUiState)
            _loadNeeded.value = true
        }
    }

    fun navigateBack() {
        _navigateBack.call()
    }

    fun navigateForward() {
        _navigateForward.call()
    }

    fun toggleBackNavigation(isEnabled: Boolean) {
        _navbarUiState.value = navbarUiState.value!!.copy(backNavigationEnabled = isEnabled)
    }

    fun toggleForwardNavigation(isEnabled: Boolean) {
        _navbarUiState.value = navbarUiState.value!!.copy(forwardNavigationEnabled = isEnabled)
    }

    fun share() {
        _share.call()
    }

    fun openPageInExternalBrowser() {
        _openInExternalBrowser.call()
    }

    fun togglePreviewModeSelectorVisibility(isVisible: Boolean) {
        _previewModeSelector.value = PreviewModeSelectorStatus(isVisible, true, previewMode.value!!)
    }

    fun selectPreviewMode(selectedPreviewMode: PreviewMode) {
        if (previewMode.value != selectedPreviewMode) {
            _previewMode.value = selectedPreviewMode
            _navbarUiState.value =
                    navbarUiState.value!!.copy(desktopPreviewHintVisible = selectedPreviewMode == DESKTOP)
            updateUiState(WebPreviewFullscreenProgressUiState)
        }
    }

    data class NavBarUiState(
        val forwardNavigationEnabled: Boolean,
        val backNavigationEnabled: Boolean,
        val desktopPreviewHintVisible: Boolean
    )

    enum class PreviewMode {
        DEFAULT,
        DESKTOP
    }

    data class PreviewModeSelectorStatus(
        val isVisible: Boolean,
        val isEnabled: Boolean,
        val selectedPreviewMode: PreviewMode
    )

    sealed class WebPreviewUiState(
        val fullscreenProgressLayoutVisibility: Boolean = false,
        val webViewVisibility: Boolean = false,
        val actionableEmptyView: Boolean = false
    ) {
        object WebPreviewContentUiState : WebPreviewUiState(
                webViewVisibility = true
        )

        object WebPreviewFullscreenProgressUiState : WebPreviewUiState(
                fullscreenProgressLayoutVisibility = true
        )

        object WebPreviewFullscreenErrorUiState : WebPreviewUiState(
                actionableEmptyView = true
        )
    }
}
