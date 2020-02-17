package org.wordpress.android.imageeditor.preview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInitialContentUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageLoadFailedUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageLoadSuccessUiState

class PreviewImageViewModel : ViewModel() {
    private val _uiState: MutableLiveData<ImageUiState> = MutableLiveData()
    val uiState: LiveData<ImageUiState> = _uiState

    fun onCreateView(loResImageUrl: String, hiResImageUrl: String) {
        updateUiState(
            ImageInitialContentUiState(
                ImageData(
                    lowResImageUrl = loResImageUrl,
                    highResImageUrl = hiResImageUrl
                )
            )
        )
    }

    fun onImageLoadSuccess(imageData: ImageData, model: Any?) {
        if (uiState.value !is ImageLoadSuccessUiState) {
            updateUiState(ImageLoadSuccessUiState)
        }
    }

    fun onImageLoadFailed(imageData: ImageData, model: Any?) {
        if (uiState.value !is ImageLoadFailedUiState) {
            updateUiState(ImageLoadFailedUiState)
        }
    }

    private fun updateUiState(uiState: ImageUiState) {
        _uiState.value = uiState
    }

    data class ImageData(val lowResImageUrl: String, val highResImageUrl: String)

    sealed class ImageUiState(
        val progressBarVisible: Boolean = false
    ) {
        data class ImageInitialContentUiState(val imageData: ImageData) : ImageUiState(progressBarVisible = true)
        object ImageLoadSuccessUiState : ImageUiState(progressBarVisible = false)
        object ImageLoadFailedUiState : ImageUiState(progressBarVisible = false)
    }
}
