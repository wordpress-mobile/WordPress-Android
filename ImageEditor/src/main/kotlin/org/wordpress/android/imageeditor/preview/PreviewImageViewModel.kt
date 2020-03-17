package org.wordpress.android.imageeditor.preview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileSuccessState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileFailedState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileIdleState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageStartLoadingToFileState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageDataStartLoadingUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInHighResLoadFailedUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInHighResLoadSuccessUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInLowResLoadFailedUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInLowResLoadSuccessUiState

class PreviewImageViewModel : ViewModel() {
    private val _uiState: MutableLiveData<ImageUiState> = MutableLiveData()
    val uiState: LiveData<ImageUiState> = _uiState

    private val _loadIntoFile = MutableLiveData<ImageLoadToFileState>(ImageLoadToFileIdleState)
    val loadIntoFile: LiveData<ImageLoadToFileState> = _loadIntoFile

    private val _navigateToCropScreenWithInputFilePath = MutableLiveData<String>()
    val navigateToCropScreenWithInputFilePath: LiveData<String> = _navigateToCropScreenWithInputFilePath

    fun onCreateView(loResImageUrl: String, hiResImageUrl: String) {
        updateUiState(
            ImageDataStartLoadingUiState(
                ImageData(loResImageUrl, hiResImageUrl)
            )
        )
    }

    fun onLoadIntoImageViewSuccess(url: String) {
        val currentState = uiState.value
        val newState = when (currentState) {
            is ImageDataStartLoadingUiState -> {
                if (url == currentState.imageData.lowResImageUrl) {
                    ImageInLowResLoadSuccessUiState
                } else {
                    ImageInHighResLoadSuccessUiState
                }
            }
            else -> ImageInHighResLoadSuccessUiState
        }

        val highResImageJustLoadedIntoView = newState != currentState && newState == ImageInHighResLoadSuccessUiState
        val imageNotLoadedIntoFile = loadIntoFile.value !is ImageLoadToFileSuccessState
        if (highResImageJustLoadedIntoView && imageNotLoadedIntoFile) {
            updateLoadIntoFileState(ImageStartLoadingToFileState(url))
        }
        updateUiState(newState)
    }

    fun onLoadIntoImageViewFailed(url: String) {
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

    fun onLoadIntoFileSuccess(inputFilePath: String) {
        updateLoadIntoFileState(ImageLoadToFileSuccessState(inputFilePath))
        _navigateToCropScreenWithInputFilePath.value = inputFilePath
    }

    fun onLoadIntoFileFailed() {
        // TODO: Do we need to display any error message to the user?
        updateLoadIntoFileState(ImageLoadToFileFailedState)
    }

    private fun updateUiState(uiState: ImageUiState) {
        _uiState.value = uiState
    }

    private fun updateLoadIntoFileState(fileState: ImageLoadToFileState) {
        _loadIntoFile.value = fileState
    }

    data class ImageData(val lowResImageUrl: String, val highResImageUrl: String)

    sealed class ImageUiState(
        val progressBarVisible: Boolean = false
    ) {
        data class ImageDataStartLoadingUiState(val imageData: ImageData) : ImageUiState(progressBarVisible = true)
        // Continue displaying progress bar on low res image load success
        object ImageInLowResLoadSuccessUiState : ImageUiState(progressBarVisible = true)
        object ImageInLowResLoadFailedUiState : ImageUiState(progressBarVisible = true)
        object ImageInHighResLoadSuccessUiState : ImageUiState(progressBarVisible = false)
        object ImageInHighResLoadFailedUiState : ImageUiState(progressBarVisible = false)
    }

    sealed class ImageLoadToFileState {
        object ImageLoadToFileIdleState : ImageLoadToFileState()
        data class ImageStartLoadingToFileState(val imageUrl: String) : ImageLoadToFileState()
        data class ImageLoadToFileSuccessState(val filePath: String) : ImageLoadToFileState()
        object ImageLoadToFileFailedState : ImageLoadToFileState()
    }
}
