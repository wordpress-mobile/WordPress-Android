package org.wordpress.android.imageeditor.preview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageDataStartLoadingUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInHighResLoadFailedUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInHighResLoadSuccessUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInLowResLoadFailedUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInLowResLoadSuccessUiState

class PreviewImageViewModel : ViewModel() {
    private val _uiState: MutableLiveData<ImageUiState> = MutableLiveData()
    val uiState: LiveData<ImageUiState> = _uiState

    private val _loadIntoFile: MutableLiveData<String> = MutableLiveData()
    val loadIntoFile: LiveData<String> = _loadIntoFile

    fun onCreateView(loResImageUrl: String, hiResImageUrl: String) {
        updateUiState(
            ImageDataStartLoadingUiState(
                ImageData(loResImageUrl, hiResImageUrl)
            )
        )
    }

    fun onImageLoadSuccess(url: String) {
        val newState = when (val currentState = uiState.value) {
            is ImageDataStartLoadingUiState -> {
                if (url == currentState.imageData.lowResImageUrl) {
                    ImageInLowResLoadSuccessUiState
                } else {
                    ImageInHighResLoadSuccessUiState
                }
            }
            else -> ImageInHighResLoadSuccessUiState
        }

        if (newState == ImageInHighResLoadSuccessUiState) {
            updateUrlToLoadIntoFile(url)
        }
        updateUiState(newState)
    }

    fun onImageLoadFailed(url: String) {
        val newState = when (val currentState = uiState.value) {
            is ImageDataStartLoadingUiState -> {
                val lowResImageUrl = currentState.imageData.lowResImageUrl
                if (url == lowResImageUrl) {
                    ImageInLowResLoadFailedUiState
                } else {
                    ImageInHighResLoadFailedUiState
                }
            }
            else -> ImageInHighResLoadFailedUiState
        }

        updateUiState(newState)
    }

    private fun updateUiState(uiState: ImageUiState) {
        _uiState.value = uiState
    }

    private fun updateUrlToLoadIntoFile(url: String) {
        _loadIntoFile.value = url
    }

    data class ImageData(val lowResImageUrl: String, val highResImageUrl: String)

    sealed class ImageUiState(
        val progressBarVisible: Boolean = false
    ) {
        data class ImageDataStartLoadingUiState(val imageData: ImageData) : ImageUiState(progressBarVisible = true)
        // Continue displaying progress bar on low res image load success
        object ImageInLowResLoadSuccessUiState : ImageUiState(progressBarVisible = true)
        object ImageInLowResLoadFailedUiState : ImageUiState(progressBarVisible = false)
        object ImageInHighResLoadSuccessUiState : ImageUiState(progressBarVisible = false)
        object ImageInHighResLoadFailedUiState : ImageUiState(progressBarVisible = false)
    }
}
