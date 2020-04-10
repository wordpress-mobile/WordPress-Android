package org.wordpress.android.imageeditor.preview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileFailedState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileIdleState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileSuccessState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageDataStartLoadingUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInHighResLoadSuccessUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInLowResLoadSuccessUiState

class PreviewImageViewModel : ViewModel() {
    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    private val _loadIntoFile = MutableLiveData<ImageLoadToFileState>(ImageLoadToFileIdleState)
    val loadIntoFile: LiveData<ImageLoadToFileState> = _loadIntoFile

    private val _navigateToCropScreenWithFileInfo = MutableLiveData<Pair<String, String?>>()
    val navigateToCropScreenWithFileInfo: LiveData<Pair<String, String?>> = _navigateToCropScreenWithFileInfo

    private var outputFileExtension: String? = null

    // TODO: Dummy data
    private val imageDataList = listOf(
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/10/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/10/1000/1000.jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/20/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/20/1000/1000.jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/30/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/30/1000/1000.jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/40/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/40/1000/1000.jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/50/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/50/1000/1000.jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/60/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/60/1000/1000.jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/70/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/70/1000/1000.jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/80/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/80/1000/1000.jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/90/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/90/1000/1000.jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/100/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/100/400/400.jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/110/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/110/1000/1000.jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/120/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/120/400/400.jpg"
        )
    )

    fun onCreateView(loResImageUrl: String, hiResImageUrl: String, outputFileExtension: String?) {
        this.outputFileExtension = outputFileExtension

        val newImageUiStates = createViewPagerItemsInitialUiStates(imageDataList)
        val currentUiState = uiState.value?.copy(viewPagerItemsStates = newImageUiStates) ?: UiState(newImageUiStates)

        updateUiState(currentUiState)
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

    fun onLoadIntoImageViewFailed(url: String, position: Int) {
        /*val newState = when (val currentState = uiState.value) {
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

        updateUiState(newState)*/
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
//        updateUiState(createImageDataStartLoadingUiState(loResImageUrl, hiResImageUrl))
    }

    private fun createViewPagerItemsInitialUiStates(
        data: List<ImageData>
    ): List<ImageUiState> = data.map { createImageLoadStartUiState(it) }

    private fun updateViewPagerItemsUiStates(
        currentUrl: String,
        currentPosition: Int,
        loadSuccess: Boolean // TODO: To be used later to differentiate image load success/failed state
    ): List<ImageUiState> {
        val currentUiState = uiState.value as UiState
        val currentImageState = currentUiState.viewPagerItemsStates[currentPosition]
        val currentImageData = currentImageState.data

        val items = currentUiState.viewPagerItemsStates.toMutableList()
        items[currentPosition] = createImageLoadSuccessUiState(currentUrl, currentImageData, currentImageState)
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
                ImageInLowResLoadSuccessUiState(currentImageData)
            } else {
                currentImageState
            }
        } else {
            ImageInHighResLoadSuccessUiState(currentImageData)
        }
    }

    private fun updateUiState(state: UiState) {
        _uiState.value = state
    }

    private fun updateLoadIntoFileState(fileState: ImageLoadToFileState) {
        _loadIntoFile.value = fileState
    }

    data class ImageData(val lowResImageUrl: String, val highResImageUrl: String)

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
        data class ImageInLowResLoadSuccessUiState(val imageData: ImageData) : ImageUiState(
            data = imageData,
            progressBarVisible = true,
            retryLayoutVisible = false
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

    sealed class ImageLoadToFileState {
        object ImageLoadToFileIdleState : ImageLoadToFileState()
        data class ImageStartLoadingToFileState(val imageUrl: String) : ImageLoadToFileState()
        data class ImageLoadToFileSuccessState(val filePath: String) : ImageLoadToFileState()
        object ImageLoadToFileFailedState : ImageLoadToFileState()
    }
}
