package org.wordpress.android.imageeditor.preview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInLowResLoadFailedUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInLowResLoadSuccessUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageDataStartLoadingUiState

class PreviewImageViewModel : ViewModel() {
    private val _uiState: MutableLiveData<ImageUiState> = MutableLiveData()
    val uiState: LiveData<ImageUiState> = _uiState

    fun onCreateView(loResImageUrl: String, hiResImageUrl: String) {
        updateUiState(
            ImageDataStartLoadingUiState(
                ImageData(
                    lowResImageUrl = loResImageUrl,
                    highResImageUrl = hiResImageUrl
                )
            )
        )
    }

    fun onImageLoadSuccess(url: String) {
        val state = uiState.value
        if (state is ImageDataStartLoadingUiState) {
            val lowResImageUrl = state.imageData.lowResImageUrl
            if (url == lowResImageUrl) {
                updateUiState(ImageInLowResLoadSuccessUiState)
            }
        }
        // TODO: Update state for high res image
    }

    fun onImageLoadFailed(url: String) {
        val state = uiState.value
        if (state is ImageDataStartLoadingUiState) {
            val lowResImageUrl = state.imageData.lowResImageUrl
            if (url == lowResImageUrl) {
                updateUiState(ImageInLowResLoadFailedUiState)
            }
        }
        // TODO: Update state for high res image
    }

    private fun updateUiState(uiState: ImageUiState) {
        _uiState.value = uiState
    }

    data class ImageData(val lowResImageUrl: String, val highResImageUrl: String)

    sealed class ImageUiState(
        val progressBarVisible: Boolean = false
    ) {
        data class ImageDataStartLoadingUiState(val imageData: ImageData) : ImageUiState(progressBarVisible = true)
        // Continue displaying progress bar on low res image load
        object ImageInLowResLoadSuccessUiState : ImageUiState(progressBarVisible = true)
        object ImageInLowResLoadFailedUiState : ImageUiState(progressBarVisible = false)
        // TODO: Add states for high res image
    }
}
