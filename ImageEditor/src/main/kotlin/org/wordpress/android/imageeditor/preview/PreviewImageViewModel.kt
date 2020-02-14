package org.wordpress.android.imageeditor.preview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageLoadInProgressContentUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageLoadFailedContentUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageLoadSuccessContentUiState

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
        updateUiState(ImageLoadInProgressContentUiState)
    }

    private fun updateUiState(uiState: ImageUiState) {
        _uiState.value = uiState
    }

    fun onImageLoadSuccess() {
        if (uiState.value !is ImageLoadSuccessContentUiState) {
            updateUiState(ImageLoadSuccessContentUiState)
        }
    }

    fun onImageLoadFailed() {
        if (uiState.value !is ImageLoadFailedContentUiState) {
            updateUiState(ImageLoadFailedContentUiState)
        }
    }

    data class ImageData(val lowResImageUrl: String, val highResImageUrl: String)

    sealed class ImageUiState(
        val progressBarVisible: Boolean = false
    ) {
        object ImageLoadInProgressContentUiState : ImageUiState(progressBarVisible = true)
        object ImageLoadSuccessContentUiState : ImageUiState(progressBarVisible = false)
        object ImageLoadFailedContentUiState : ImageUiState(progressBarVisible = false)
    }
}
