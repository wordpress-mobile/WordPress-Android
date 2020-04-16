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
import org.wordpress.android.imageeditor.viewmodel.Event

class PreviewImageViewModel : ViewModel() {
    private val _uiState: MutableLiveData<Event<UiState>> = MutableLiveData()
    val uiState: LiveData<Event<UiState>> = _uiState

    private val _loadIntoFile = MutableLiveData<Event<ImageLoadToFileState>>(Event(ImageLoadToFileIdleState))
    val loadIntoFile: LiveData<Event<ImageLoadToFileState>> = _loadIntoFile

    private val _navigateToCropScreenWithFileInfo = MutableLiveData<Event<Triple<String, String?, Boolean>>>()
    val navigateToCropScreenWithFileInfo: LiveData<Event<Triple<String, String?, Boolean>>> =
            _navigateToCropScreenWithFileInfo

    fun onCreateView(imageDataList: List<ImageData>) {
        if (uiState.value == null) {
            val newImageUiStates = createViewPagerItemsInitialUiStates(imageDataList)
            val currentUiState = UiState(
                newImageUiStates,
                thumbnailsTabLayoutVisible = !newImageUiStates.hasSingleElement()
            )
            updateUiState(currentUiState)
        }
    }

    fun onLoadIntoImageViewSuccess(imageUrlAtPosition: String, position: Int) {
        val newImageUiStates = updateViewPagerItemsUiStates(
            imageUrlAtPosition = imageUrlAtPosition,
            position = position,
            loadSuccess = true
        )
        val newImageState = newImageUiStates[position]

        val currentUiState = uiState.value?.peekContent() as UiState
        val imageStateAtPosition = currentUiState.viewPagerItemsStates[position]

        if (currentUiState.viewPagerItemsStates.hasSingleElement() && canLoadToFile(newImageState)) {
            updateLoadIntoFileState(
                ImageStartLoadingToFileState(
                    imageUrlAtPosition = imageStateAtPosition.data.highResImageUrl,
                    position = position
                )
            )
        }
        updateUiState(currentUiState.copy(
                viewPagerItemsStates = newImageUiStates,
                editActionsEnabled = shouldEnableEditActionsForImageState(newImageState)
            )
        )
    }

    fun onLoadIntoImageViewFailed(imageUrlAtPosition: String, position: Int) {
        val newImageUiStates = updateViewPagerItemsUiStates(
            imageUrlAtPosition = imageUrlAtPosition,
            position = position,
            loadSuccess = false
        )
        val currentUiState = uiState.value?.peekContent() as UiState
        updateUiState(currentUiState.copy(viewPagerItemsStates = newImageUiStates))
    }

    fun onLoadIntoFileSuccess(inputFilePathAtPosition: String, position: Int) {
        val imageStateAtPosition = (uiState.value?.peekContent() as UiState).viewPagerItemsStates[position]
        val outputFileExtension = imageStateAtPosition.data.outputFileExtension

        updateLoadIntoFileState(ImageLoadToFileSuccessState(inputFilePathAtPosition, position))

        val currentUiState = uiState.value?.peekContent() as UiState
        _navigateToCropScreenWithFileInfo.value = Event(
            Triple(
                inputFilePathAtPosition,
                outputFileExtension,
                !currentUiState.viewPagerItemsStates.hasSingleElement()
            )
        )
    }

    fun onLoadIntoFileFailed() {
        // TODO: Do we need to display any error message to the user?
        updateLoadIntoFileState(ImageLoadToFileFailedState)
    }

    fun onCropMenuClicked(selectedPosition: Int) {
        val selectedImageState = (uiState.value?.peekContent() as UiState).viewPagerItemsStates[selectedPosition]
        updateLoadIntoFileState(
            ImageStartLoadingToFileState(
                imageUrlAtPosition = selectedImageState.data.highResImageUrl,
                position = selectedPosition
            )
        )
    }

    private fun onLoadIntoImageViewRetry(selectedImageUrl: String, selectedPosition: Int) {
        val newImageUiStates = updateViewPagerItemsUiStates(
            imageUrlAtPosition = selectedImageUrl,
            position = selectedPosition,
            loadSuccess = false,
            retry = true
        )
        val currentUiState = uiState.value?.peekContent() as UiState
        updateUiState(currentUiState.copy(viewPagerItemsStates = newImageUiStates))
    }

    private fun createViewPagerItemsInitialUiStates(
        data: List<ImageData>
    ): List<ImageUiState> = data.map { createImageLoadStartUiState(it) }

    private fun updateViewPagerItemsUiStates(
        imageUrlAtPosition: String,
        position: Int,
        loadSuccess: Boolean,
        retry: Boolean = false
    ): List<ImageUiState> {
        val currentUiState = uiState.value?.peekContent() as UiState
        val imageStateAtPosition = currentUiState.viewPagerItemsStates[position]
        val imageDataAtPosition = imageStateAtPosition.data

        val items = currentUiState.viewPagerItemsStates.toMutableList()
        items[position] = when {
            loadSuccess -> createImageLoadSuccessUiState(imageUrlAtPosition, imageDataAtPosition, imageStateAtPosition)
            retry -> createImageLoadStartUiState(imageDataAtPosition)
            else -> createImageLoadFailedUiState(imageUrlAtPosition, imageDataAtPosition, position)
        }
        return items
    }

    private fun createImageLoadStartUiState(
        imageData: ImageData
    ): ImageUiState {
        return ImageDataStartLoadingUiState(imageData)
    }

    private fun createImageLoadSuccessUiState(
        imageUrl: String,
        imageData: ImageData,
        imageState: ImageUiState
    ): ImageUiState {
        return if (imageUrl == imageData.lowResImageUrl) {
            val isHighResImageAlreadyLoaded = imageState is ImageInHighResLoadSuccessUiState
            if (!isHighResImageAlreadyLoaded) {
                val isRetryShown = imageState is ImageInHighResLoadFailedUiState
                ImageInLowResLoadSuccessUiState(imageData, isRetryShown)
            } else {
                imageState
            }
        } else {
            ImageInHighResLoadSuccessUiState(imageData)
        }
    }

    private fun createImageLoadFailedUiState(
        imageUrlAtPosition: String,
        imageDataAtPosition: ImageData,
        position: Int
    ): ImageUiState {
        val imageUiState = if (imageUrlAtPosition == imageDataAtPosition.lowResImageUrl) {
            ImageInLowResLoadFailedUiState(imageDataAtPosition)
        } else {
            ImageInHighResLoadFailedUiState(imageDataAtPosition)
        }

        imageUiState.onItemTapped = {
            onLoadIntoImageViewRetry(imageUrlAtPosition, position)
        }

        return imageUiState
    }

    private fun updateUiState(state: UiState) {
        _uiState.value = Event(state)
    }

    private fun updateLoadIntoFileState(fileState: ImageLoadToFileState) {
        _loadIntoFile.value = Event(fileState)
    }

    private fun shouldEnableEditActionsForImageState(imageState: ImageUiState) =
            imageState is ImageInHighResLoadSuccessUiState

    // TODO: revisit
    private fun canLoadToFile(imageState: ImageUiState) = imageState is ImageInHighResLoadSuccessUiState

    private fun List<ImageUiState>.hasSingleElement() = this.size == 1

    data class ImageData(val lowResImageUrl: String, val highResImageUrl: String, val outputFileExtension: String)

    data class UiState(
        val viewPagerItemsStates: List<ImageUiState>,
        val editActionsEnabled: Boolean = false,
        val thumbnailsTabLayoutVisible: Boolean = true
    )

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

    sealed class ImageLoadToFileState {
        object ImageLoadToFileIdleState : ImageLoadToFileState()
        data class ImageStartLoadingToFileState(val imageUrlAtPosition: String, val position: Int) :
                ImageLoadToFileState()
        data class ImageLoadToFileSuccessState(val filePathAtPosition: String, val position: Int) :
                ImageLoadToFileState()
        object ImageLoadToFileFailedState : ImageLoadToFileState()
    }
}
