package org.wordpress.android.imageeditor.preview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileFailedState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileIdleState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileSuccessState
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

    private val _navigateToCropScreenWithFileInfo = MutableLiveData<Pair<String, String?>>()
    val navigateToCropScreenWithFileInfo: LiveData<Pair<String, String?>> = _navigateToCropScreenWithFileInfo

    private var outputFileExtension: String? = null

    fun onCreateView(loResImageUrl: String, hiResImageUrl: String, outputFileExtension: String?) {
        this.outputFileExtension = outputFileExtension
        updateUiState(createImageDataStartLoadingUiState(loResImageUrl, hiResImageUrl))
    }

    fun onLoadIntoImageViewSuccess(currentUrl: String, imageData: ImageData) {
        val currentState = uiState.value
        val isHighResImageAlreadyLoaded = currentState == ImageInHighResLoadSuccessUiState

        val newState = if (currentUrl == imageData.lowResImageUrl) {
            if (!isHighResImageAlreadyLoaded) {
                ImageInLowResLoadSuccessUiState
            } else {
                null // don't update state if high res image already loaded
            }
        } else {
            ImageInHighResLoadSuccessUiState
        }

        val highResImageJustLoadedIntoView = newState != currentState && newState == ImageInHighResLoadSuccessUiState
        val imageNotLoadedIntoFile = loadIntoFile.value !is ImageLoadToFileSuccessState
        if (highResImageJustLoadedIntoView && imageNotLoadedIntoFile) {
            updateLoadIntoFileState(ImageStartLoadingToFileState(currentUrl))
        }

        newState?.let { updateUiState(it) }
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
        _navigateToCropScreenWithFileInfo.value = Pair(inputFilePath, outputFileExtension)
    }

    fun onLoadIntoFileFailed() {
        // TODO: Do we need to display any error message to the user?
        updateLoadIntoFileState(ImageLoadToFileFailedState)
    }

    fun onLoadIntoImageViewRetry(loResImageUrl: String, hiResImageUrl: String) {
        updateUiState(createImageDataStartLoadingUiState(loResImageUrl, hiResImageUrl))
    }

    private fun createImageDataStartLoadingUiState(
        loResImageUrl: String,
        hiResImageUrl: String
    ): ImageDataStartLoadingUiState = ImageDataStartLoadingUiState(
        ImageData(loResImageUrl, hiResImageUrl)
    )

    private fun updateUiState(uiState: ImageUiState) {
        _uiState.value = uiState
    }

    private fun updateLoadIntoFileState(fileState: ImageLoadToFileState) {
        _loadIntoFile.value = fileState
    }

    data class ImageData(val lowResImageUrl: String, val highResImageUrl: String)

    sealed class ImageUiState(
        val progressBarVisible: Boolean = false,
        val retryLayoutVisible: Boolean
    ) {
        data class ImageDataStartLoadingUiState(val imageData: ImageData) : ImageUiState(
            progressBarVisible = true,
            retryLayoutVisible = false
        )
        // Continue displaying progress bar on low res image load success
        object ImageInLowResLoadSuccessUiState : ImageUiState(
            progressBarVisible = true,
            retryLayoutVisible = false
        )
        object ImageInLowResLoadFailedUiState : ImageUiState(
            progressBarVisible = true,
            retryLayoutVisible = false
        )
        object ImageInHighResLoadSuccessUiState : ImageUiState(
            progressBarVisible = false,
            retryLayoutVisible = false
        )
        // Display retry only when high res image load failed
        object ImageInHighResLoadFailedUiState : ImageUiState(
            progressBarVisible = false,
            retryLayoutVisible = true
        )
    }

    sealed class ImageLoadToFileState {
        object ImageLoadToFileIdleState : ImageLoadToFileState()
        data class ImageStartLoadingToFileState(val imageUrl: String) : ImageLoadToFileState()
        data class ImageLoadToFileSuccessState(val filePath: String) : ImageLoadToFileState()
        object ImageLoadToFileFailedState : ImageLoadToFileState()
    }
}
