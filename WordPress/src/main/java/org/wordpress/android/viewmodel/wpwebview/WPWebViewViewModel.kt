package org.wordpress.android.viewmodel.wpwebview

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.helpers.ConnectionStatus.AVAILABLE
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

    private val lifecycleRegistry = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    init {
        lifecycleRegistry.markState(Lifecycle.State.CREATED)
        connectionStatus.observe(this, Observer {
            if (it == AVAILABLE) {
                load()
            }
        })
    }

    fun start() {
        if (isStarted) {
            return
        }
        isStarted = true
        // If there is no internet show the error screen
        if (!networkUtils.isNetworkAvailable()) {
            updateUiState(WebPreviewFullscreenErrorUiState)
        } else {
            updateUiState(WebPreviewFullscreenProgressUiState)
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
    fun onError() {
        if (uiState.value !is WebPreviewFullscreenErrorUiState) {
            updateUiState(WebPreviewFullscreenErrorUiState)
        }
        _loadNeeded.value = false
    }

    fun load() {
        if (uiState.value !is WebPreviewFullscreenProgressUiState) {
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

        object WebPreviewFullscreenErrorUiState : WebPreviewUiState(
                actionableEmptyView = true
        )
    }
}
