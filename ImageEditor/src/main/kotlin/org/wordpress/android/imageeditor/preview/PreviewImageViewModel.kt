package org.wordpress.android.imageeditor.preview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageLoadInProgressUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageLoadFailedUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageLoadSuccessUiState

class PreviewImageViewModel : ViewModel() {
    private val _loadImageFromData = MutableLiveData<ImageData>()
    val loadImageFromData: LiveData<ImageData> = _loadImageFromData

    private val _uiState: MutableLiveData<ImageUiState> = MutableLiveData()
    val uiState: LiveData<ImageUiState> = _uiState

    private var isStarted = false

    fun start(loResImageUrl: String, hiResImageUrl: String) {
        if (isStarted) {
            return
        }
        isStarted = true

        _loadImageFromData.value = ImageData(
            lowResImageUrl = loResImageUrl,
            highResImageUrl = hiResImageUrl
        )
        updateUiState(ImageLoadInProgressUiState)
    }

    private fun updateUiState(uiState: ImageUiState) {
        _uiState.value = uiState
    }

    fun onImageLoadSuccess() {
        if (uiState.value !is ImageLoadSuccessUiState) {
            updateUiState(ImageLoadSuccessUiState)
        }
    }

    fun onImageLoadFailed() {
        if (uiState.value !is ImageLoadFailedUiState) {
            updateUiState(ImageLoadFailedUiState)
        }
    }

    data class ImageData(val lowResImageUrl: String, val highResImageUrl: String)

    sealed class ImageUiState(
        val progressBarVisible: Boolean = false
    ) {
        object ImageLoadInProgressUiState : ImageUiState(progressBarVisible = true)
        object ImageLoadSuccessUiState : ImageUiState(progressBarVisible = false)
        object ImageLoadFailedUiState : ImageUiState(progressBarVisible = false)
    }
}
