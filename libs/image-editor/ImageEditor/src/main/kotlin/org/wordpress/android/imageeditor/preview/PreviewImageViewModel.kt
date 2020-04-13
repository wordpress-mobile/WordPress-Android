package org.wordpress.android.imageeditor.preview

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageDataStartLoadingUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInHighResLoadFailedUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInHighResLoadSuccessUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInLowResLoadFailedUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInLowResLoadSuccessUiState

class PreviewImageViewModel : ViewModel() {
    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

//    private val _loadIntoFile = MutableLiveData<ImageLoadToFileState>(ImageLoadToFileIdleState)
//    val loadIntoFile: LiveData<ImageLoadToFileState> = _loadIntoFile

//    private val _navigateToCropScreenWithFileInfo = MutableLiveData<Pair<String, String?>>()
//    val navigateToCropScreenWithFileInfo: LiveData<Pair<String, String?>> = _navigateToCropScreenWithFileInfo

    fun onCreateView(imageDataList: List<ImageData>) {
        if (uiState.value == null) {
            val newImageUiStates = createViewPagerItemsInitialUiStates(imageDataList)
            val currentUiState = uiState.value?.copy(viewPagerItemsStates = newImageUiStates) ?: UiState(
                newImageUiStates
            )
            updateUiState(currentUiState)
        }
    }

    fun onLoadIntoImageViewSuccess(currentUrl: String, currentPosition: Int) {
        val newImageUiStates = updateViewPagerItemsUiStates(
            currentUrl = currentUrl,
            currentPosition = currentPosition,
            loadSuccess = true
        )
        // TODO: handle file save on done button action to be added later
//        val highResImageJustLoadedIntoView = newState != currentState && newState == ImageInHighResLoadSuccessUiState
//        val imageNotLoadedIntoFile = loadIntoFile.value !is ImageLoadToFileSuccessState
//        if (highResImageJustLoadedIntoView && imageNotLoadedIntoFile) {
//            updateLoadIntoFileState(ImageStartLoadingToFileState(currentUrl))
//        }
        val currentUiState = uiState.value as UiState
        updateUiState(currentUiState.copy(viewPagerItemsStates = newImageUiStates))
    }

    fun onLoadIntoImageViewFailed(currentUrl: String, currentPosition: Int) {
        val newImageUiStates = updateViewPagerItemsUiStates(
            currentUrl = currentUrl,
            currentPosition = currentPosition,
            loadSuccess = false
        )
        val currentUiState = uiState.value as UiState
        updateUiState(currentUiState.copy(viewPagerItemsStates = newImageUiStates))
    }

    /*fun onLoadIntoFileSuccess(inputFilePath: String) {
        updateLoadIntoFileState(ImageLoadToFileSuccessState(inputFilePath))
        _navigateToCropScreenWithFileInfo.value = Pair(inputFilePath, outputFileExtension)
    }

    fun onLoadIntoFileFailed() {
        // TODO: Do we need to display any error message to the user?
        updateLoadIntoFileState(ImageLoadToFileFailedState)
    }*/

    @VisibleForTesting
    private fun onLoadIntoImageViewRetry(currentUrl: String, currentPosition: Int) {
        val newImageUiStates = updateViewPagerItemsUiStates(
            currentUrl = currentUrl,
            currentPosition = currentPosition,
            loadSuccess = false,
            retry = true
        )
        val currentUiState = uiState.value as UiState
        updateUiState(currentUiState.copy(viewPagerItemsStates = newImageUiStates))
    }

    private fun createViewPagerItemsInitialUiStates(
        data: List<ImageData>
    ): List<ImageUiState> = data.map { createImageLoadStartUiState(it) }

    private fun updateViewPagerItemsUiStates(
        currentUrl: String,
        currentPosition: Int,
        loadSuccess: Boolean,
        retry: Boolean = false
    ): List<ImageUiState> {
        val currentUiState = uiState.value as UiState
        val currentImageState = currentUiState.viewPagerItemsStates[currentPosition]
        val currentImageData = currentImageState.data

        val items = currentUiState.viewPagerItemsStates.toMutableList()
        items[currentPosition] = when {
            loadSuccess -> createImageLoadSuccessUiState(currentUrl, currentImageData, currentImageState)
            retry -> createImageLoadStartUiState(currentImageData)
            else -> createImageLoadFailedUiState(currentUrl, currentImageData, currentPosition)
        }
        return items
    }

    private fun createImageLoadStartUiState(
        currentImageData: ImageData
    ): ImageUiState {
        return ImageDataStartLoadingUiState(currentImageData)
    }

    private fun createImageLoadSuccessUiState(
        currentUrl: String,
        currentImageData: ImageData,
        currentImageState: ImageUiState
    ): ImageUiState {
        return if (currentUrl == currentImageData.lowResImageUrl) {
            val isHighResImageAlreadyLoaded = currentImageState is ImageInHighResLoadSuccessUiState
            if (!isHighResImageAlreadyLoaded) {
                val isRetryShown = currentImageState is ImageInHighResLoadFailedUiState
                ImageInLowResLoadSuccessUiState(currentImageData, isRetryShown)
            } else {
                currentImageState
            }
        } else {
            ImageInHighResLoadSuccessUiState(currentImageData)
        }
    }

    private fun createImageLoadFailedUiState(
        currentUrl: String,
        currentImageData: ImageData,
        currentPosition: Int
    ): ImageUiState {
        val imageUiState = if (currentUrl == currentImageData.lowResImageUrl) {
            ImageInLowResLoadFailedUiState(currentImageData)
        } else {
            ImageInHighResLoadFailedUiState(currentImageData)
        }

        imageUiState.onItemTapped = {
            onLoadIntoImageViewRetry(currentUrl, currentPosition)
        }

        return imageUiState
    }

    private fun updateUiState(state: UiState) {
        _uiState.value = state
    }

    /*private fun updateLoadIntoFileState(fileState: ImageLoadToFileState) {
        _loadIntoFile.value = fileState
    }*/

    data class ImageData(val lowResImageUrl: String, val highResImageUrl: String, val outputFileExtension: String)

    data class UiState(val viewPagerItemsStates: List<ImageUiState>)

    sealed class ImageUiState(
        val data: ImageData,
        val progressBarVisible: Boolean = false,
        val retryLayoutVisible: Boolean
    ) {
        var onItemTapped: (() -> Unit)? = null
        data class ImageDataStartLoadingUiState(val imageData: ImageData) : ImageUiState(
            data = imageData,
            progressBarVisible = true,
            retryLayoutVisible = false
        )
        // Continue displaying progress bar on low res image load success
        data class ImageInLowResLoadSuccessUiState(val imageData: ImageData, val isRetryShown: Boolean = false)
            : ImageUiState(
            data = imageData,
            progressBarVisible = !isRetryShown,
            retryLayoutVisible = isRetryShown
        )
        data class ImageInLowResLoadFailedUiState(val imageData: ImageData) : ImageUiState(
            data = imageData,
            progressBarVisible = true,
            retryLayoutVisible = false
        )
        data class ImageInHighResLoadSuccessUiState(val imageData: ImageData) : ImageUiState(
            data = imageData,
            progressBarVisible = false,
            retryLayoutVisible = false
        )
        // Display retry only when high res image load failed
        data class ImageInHighResLoadFailedUiState(val imageData: ImageData) : ImageUiState(
            data = imageData,
            progressBarVisible = false,
            retryLayoutVisible = true
        )
    }

    /*sealed class ImageLoadToFileState {
        object ImageLoadToFileIdleState : ImageLoadToFileState()
        data class ImageStartLoadingToFileState(val imageUrl: String) : ImageLoadToFileState()
        data class ImageLoadToFileSuccessState(val filePath: String) : ImageLoadToFileState()
        object ImageLoadToFileFailedState : ImageLoadToFileState()
    }*/
}
