package org.wordpress.android.viewmodel.wpwebview

import androidx.annotation.DrawableRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.PreviewMode
import org.wordpress.android.ui.PreviewMode.DESKTOP
import org.wordpress.android.ui.PreviewMode.MOBILE
import org.wordpress.android.ui.PreviewMode.TABLET
import org.wordpress.android.ui.PreviewModeHandler
import org.wordpress.android.ui.WPWebViewUsageCategory
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.helpers.ConnectionStatus.AVAILABLE
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewContentUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewFullscreenProgressUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewFullscreenUiState.WebPreviewFullscreenErrorUiState
import javax.inject.Inject

class WPWebViewViewModel
@Inject constructor(
    private val displayUtils: DisplayUtilsWrapper,
    private val networkUtils: NetworkUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    connectionStatus: LiveData<ConnectionStatus>
) : ViewModel(), PreviewModeHandler {
    private var isStarted = false
    private var wpWebViewUsageCategory: WPWebViewUsageCategory = WPWebViewUsageCategory.WEBVIEW_STANDARD

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

    private val lifecycleOwner = object : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this)
        override fun getLifecycle(): Lifecycle = lifecycleRegistry
    }

    private val defaultPreviewMode: PreviewMode
        get() = if (displayUtils.isTablet()) TABLET else MOBILE

    init {
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.CREATED
        connectionStatus.observe(lifecycleOwner, Observer {
            if (it == AVAILABLE) {
                loadIfNecessary()
            }
        })
    }

    fun start(webViewUsageCategory: WPWebViewUsageCategory) {
        if (isStarted) {
            return
        }
        isStarted = true
        wpWebViewUsageCategory = webViewUsageCategory
        _navbarUiState.value = NavBarUiState(
            forwardNavigationEnabled = false,
            backNavigationEnabled = false,
            previewModeHintVisible = false,
            reviewHintResId = getPreviewHintResId(defaultPreviewMode)
        )
        _previewMode.value = defaultPreviewMode
        _previewModeSelector.value = PreviewModeSelectorStatus(
            isVisible = false,
            isEnabled = false,
            selectedPreviewMode = defaultPreviewMode
        )

        if (WPWebViewUsageCategory.isActionableDirectUsage(wpWebViewUsageCategory)) {
            updateUiState(WPWebViewUsageCategory.actionableDirectUsageToWebPreviewUiState(wpWebViewUsageCategory))
        } else if (networkUtils.isNetworkAvailable()) {
            updateUiState(WebPreviewFullscreenProgressUiState)
        } else {
            updateUiState(WebPreviewFullscreenErrorUiState())
        }
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onCleared() {
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
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
            return
        }
        if (uiState.value !is WebPreviewFullscreenErrorUiState) {
            updateUiState(WebPreviewFullscreenErrorUiState())
        }
        _loadNeeded.value = false
    }

    fun loadIfNecessary() {
        if (isActionableDirectUsage()) return

        if (uiState.value !is WebPreviewFullscreenProgressUiState &&
            uiState.value !is WebPreviewContentUiState
        ) {
            updateUiState(WebPreviewFullscreenProgressUiState)
            _loadNeeded.value = true
        }
    }

    fun isActionableDirectUsage() = WPWebViewUsageCategory.isActionableDirectUsage(wpWebViewUsageCategory)

    fun getMenuUiState() = wpWebViewUsageCategory.menuUiState

    fun navigateBack() {
        analyticsTrackerWrapper.track(Stat.WEBVIEW_NAVIGATED_BACK)
        _navigateBack.call()
    }

    fun navigateForward() {
        analyticsTrackerWrapper.track(Stat.WEBVIEW_NAVIGATED_FORWARD)
        _navigateForward.call()
    }

    fun toggleBackNavigation(isEnabled: Boolean) {
        _navbarUiState.value = navbarUiState.value!!.copy(backNavigationEnabled = isEnabled)
    }

    fun toggleForwardNavigation(isEnabled: Boolean) {
        _navbarUiState.value = navbarUiState.value!!.copy(forwardNavigationEnabled = isEnabled)
    }

    fun share() {
        analyticsTrackerWrapper.track(Stat.WEBVIEW_SHARE_TAPPED)
        _share.call()
    }

    fun openPageInExternalBrowser() {
        analyticsTrackerWrapper.track(Stat.WEBVIEW_OPEN_IN_BROWSER_TAPPED)
        _openInExternalBrowser.call()
    }

    fun togglePreviewModeSelectorVisibility(isVisible: Boolean) {
        _previewModeSelector.value = PreviewModeSelectorStatus(isVisible, true, previewMode.value!!)
    }

    fun selectPreviewMode(selectedPreviewMode: PreviewMode) {
        analyticsTrackerWrapper.track(
            Stat.WEBVIEW_PREVIEW_DEVICE_CHANGED,
            mapOf(TRACK_SELECTED_OPTION_NAME to selectedPreviewMode.name.lowercase())
        )
        if (previewMode.value != selectedPreviewMode) {
            _previewMode.value = selectedPreviewMode
            _navbarUiState.value =
                navbarUiState.value!!.copy(
                    previewModeHintVisible = selectedPreviewMode != MOBILE,
                    reviewHintResId = getPreviewHintResId(selectedPreviewMode)
                )
            updateUiState(WebPreviewFullscreenProgressUiState)
        }
    }

    fun track(stat: Stat) {
        analyticsTrackerWrapper.track(stat)
    }

    private fun getPreviewHintResId(previewMode: PreviewMode) = when (previewMode) {
        MOBILE -> R.string.web_preview_mobile
        TABLET -> R.string.web_preview_tablet
        DESKTOP -> R.string.web_preview_desktop
    }

    data class NavBarUiState(
        val forwardNavigationEnabled: Boolean,
        val backNavigationEnabled: Boolean,
        val previewModeHintVisible: Boolean,
        val reviewHintResId: Int
    )

    data class PreviewModeSelectorStatus(
        val isVisible: Boolean,
        val isEnabled: Boolean,
        val selectedPreviewMode: PreviewMode
    )

    sealed class WebPreviewUiState(
        val fullscreenProgressLayoutVisibility: Boolean = false,
        val actionableEmptyView: Boolean = false
    ) {
        object WebPreviewContentUiState : WebPreviewUiState()

        object WebPreviewFullscreenProgressUiState : WebPreviewUiState(
            fullscreenProgressLayoutVisibility = true
        )

        sealed class WebPreviewFullscreenUiState : WebPreviewUiState(actionableEmptyView = true) {
            abstract val imageRes: Int
            abstract val titleText: UiStringRes?
            abstract val subtitleText: UiStringRes?
            abstract val buttonVisibility: Boolean

            data class WebPreviewFullscreenErrorUiState(
                @DrawableRes
                override val imageRes: Int = R.drawable.img_illustration_cloud_off_152dp,
                override val titleText: UiStringRes = UiStringRes(R.string.error_browser_no_network),
                override val subtitleText: UiStringRes = UiStringRes(R.string.error_network_connection),
                override val buttonVisibility: Boolean = true
            ) : WebPreviewFullscreenUiState()

            object WebPreviewFullscreenNotAvailableUiState : WebPreviewFullscreenUiState() {
                @DrawableRes
                override val imageRes: Int = R.drawable.img_illustration_empty_results_216dp
                override val titleText: UiStringRes = UiStringRes(R.string.preview_unavailable_self_hosted_sites)
                override val subtitleText: UiStringRes? = null
                override val buttonVisibility: Boolean = false
            }
        }
    }

    override fun selectedPreviewMode(): PreviewMode = _previewMode.value ?: defaultPreviewMode

    override fun onPreviewModeChanged(mode: PreviewMode) {
        togglePreviewModeSelectorVisibility(false)
        selectPreviewMode(mode)
    }

    companion object {
        const val TRACK_SELECTED_OPTION_NAME = "selected_option_name"
    }
}
