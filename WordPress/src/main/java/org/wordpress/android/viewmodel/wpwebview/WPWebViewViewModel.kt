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
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.CrashLoggingUtils
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.helpers.ConnectionStatus.AVAILABLE
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewContentUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState
        .WebPreviewFullscreenUiState.WebPreviewFullscreenErrorUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState
        .WebPreviewFullscreenProgressUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState
        .WebPreviewFullscreenUiState.WebPreviewFullscreenNotAvailableUiState
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

    fun start(actionableDirectUsageState: WPWebViewActivity.ActionableReusableState) {
        if (isStarted) {
            return
        }
        isStarted = true
        if (actionableDirectUsageState != WPWebViewActivity.ActionableReusableState.NONE) {
            updateUiState(WPWebViewActivity.ActionableReusableState.toWebPreviewUiState(actionableDirectUsageState))
        } else if (networkUtils.isNetworkAvailable()) {
            // If there is no internet show the error screen
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
        if (uiState.value !is WebPreviewFullscreenProgressUiState &&
                uiState.value !is WebPreviewContentUiState &&
                uiState.value !is WebPreviewFullscreenNotAvailableUiState
        ) {
            updateUiState(WebPreviewFullscreenProgressUiState)
            _loadNeeded.value = true
        }
    }

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

        sealed class WebPreviewFullscreenUiState : WebPreviewUiState(actionableEmptyView = true) {
            abstract val imageRes: Int
            abstract val titleText: UiStringRes?
            abstract val subtitleText: UiStringRes?
            abstract val buttonVisibility: Boolean

            object WebPreviewFullscreenErrorUiState : WebPreviewFullscreenUiState() {
                @DrawableRes
                override val imageRes: Int = R.drawable.img_illustration_cloud_off_152dp
                override val titleText: UiStringRes = UiStringRes(R.string.error_browser_no_network)
                override val subtitleText: UiStringRes = UiStringRes(R.string.error_network_connection)
                override val buttonVisibility: Boolean = true
            }

            object WebPreviewFullscreenNotAvailableUiState : WebPreviewFullscreenUiState() {
                @DrawableRes
                override val imageRes: Int = R.drawable.img_illustration_empty_results_216dp
                override val titleText: UiStringRes = UiStringRes(R.string.preview_unavailable_self_hosted_sites)
                override val subtitleText: UiStringRes? = null
                override val buttonVisibility: Boolean = false
            }
        }
    }
}
