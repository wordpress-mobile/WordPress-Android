package org.wordpress.android.viewmodel.wpwebview

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewContentUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewFullscreenErrorUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewFullscreenProgressUiState
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

class WPWebViewViewModel
@Inject constructor(
    private val networkUtils: NetworkUtilsWrapper
) : ViewModel() {
    private var isStarted = false

    private val m_uiState: MutableLiveData<WebPreviewUiState> = MutableLiveData()
    val mUiState: LiveData<WebPreviewUiState> = m_uiState

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
        m_uiState.value = uiState
    }

    /**
     * Update the ui state if the Loading or Error screen is being shown.
     * In other words don't update it after a configuration change.
     */
    fun onUrlLoaded() {
        if (mUiState.value !is WebPreviewContentUiState) {
            updateUiState(WebPreviewContentUiState)
        }
    }

    /**
     * Update the ui state if the Loading or Success screen is being shown.
     */
    fun onError() {
        if (mUiState.value !is WebPreviewFullscreenErrorUiState) {
            updateUiState(WebPreviewFullscreenErrorUiState)
        }
    }

    fun retry() {
        if (mUiState.value !is WebPreviewFullscreenProgressUiState) {
            updateUiState(WebPreviewFullscreenProgressUiState)
        }
    }

    sealed class WebPreviewUiState(
        val fullscreenProgressLayoutVisibility: Boolean = false,
        val webViewVisibility: Boolean = false,
        val fullscreenErrorLayoutVisibility: Boolean = false
    ) {
        object WebPreviewContentUiState : WebPreviewUiState(
                webViewVisibility = true
        )

        object WebPreviewFullscreenProgressUiState : WebPreviewUiState(
                fullscreenProgressLayoutVisibility = true
        )

        object WebPreviewFullscreenErrorUiState : WebPreviewUiState(
                fullscreenErrorLayoutVisibility = true
        )
    }
}
