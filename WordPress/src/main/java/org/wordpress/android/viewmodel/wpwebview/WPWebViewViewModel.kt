package org.wordpress.android.viewmodel.wpwebview

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewContentUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewFullscreenErrorUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewFullscreenProgressUiState
import javax.inject.Inject

class WPWebViewViewModel
@Inject constructor(
    private val networkUtils: NetworkUtilsWrapper
) : ViewModel() {
    private var isStarted = false

    private val _uiState: MutableLiveData<WebPreviewUiState> = MutableLiveData()
    val uiState: LiveData<WebPreviewUiState> = _uiState

    /**
     * If there is no internet show the error screen
     */
    fun start() {
        if (isStarted) {
            return
        }
        isStarted = true
        if (!networkUtils.isNetworkAvailable()) {
            updateUiState(WebPreviewFullscreenErrorUiState)
        } else {
            updateUiState(WebPreviewFullscreenProgressUiState)
        }
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
    }

    /**
     * Update the ui state if the Loading or Success screen is being shown.
     */
    fun onError() {
        if (uiState.value !is WebPreviewFullscreenErrorUiState) {
            updateUiState(WebPreviewFullscreenErrorUiState)
        }
    }

    fun retry() {
        if (uiState.value !is WebPreviewFullscreenProgressUiState) {
            updateUiState(WebPreviewFullscreenProgressUiState)
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
