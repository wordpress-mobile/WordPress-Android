package org.wordpress.android.imageeditor.preview

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.imageeditor.ImageEditor.EditorAction
import org.wordpress.android.imageeditor.R
import org.wordpress.android.imageeditor.preview.PreviewImageFragment.Companion.EditImageData.InputData
import org.wordpress.android.imageeditor.preview.PreviewImageFragment.Companion.EditImageData.OutputData
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
import java.net.URI
import java.util.UUID

class PreviewImageViewModel : ViewModel() {
    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    private val _loadIntoFile = MutableLiveData<Event<ImageLoadToFileState>>(Event(ImageLoadToFileIdleState))
    val loadIntoFile: LiveData<Event<ImageLoadToFileState>> = _loadIntoFile

    private val _navigateToCropScreenWithFileInfo = MutableLiveData<Event<Triple<String, String?, Boolean>>>()
    val navigateToCropScreenWithFileInfo: LiveData<Event<Triple<String, String?, Boolean>>> =
        _navigateToCropScreenWithFileInfo

    private val _finishAction = MutableLiveData<Event<List<OutputData>>>()
    val finishAction: LiveData<Event<List<OutputData>>> = _finishAction

    private val _editorAction = MutableLiveData<Event<EditorAction>>()
    val editorAction: LiveData<Event<EditorAction>> = _editorAction

    var selectedPosition: Int = 0
        private set

    var numberOfImages = 0
        private set

    fun onCreateView(imageDataList: List<InputData>) {
        this.numberOfImages = imageDataList.size

        if (uiState.value == null) {
            val newImageUiStates = createViewPagerItemsInitialUiStates(
                convertInputDataToImageData(imageDataList)
            )
            val currentUiState = UiState(
                newImageUiStates,
                thumbnailsTabLayoutVisible = !newImageUiStates.hasSingleElement()
            )
            updateUiState(currentUiState)
        }
    }

    private fun convertInputDataToImageData(imageDataList: List<InputData>): List<ImageData> {
        return imageDataList
            .map { (highRes, lowRes, extension) ->
                ImageData(
                    highResImageUrl = highRes,
                    lowResImageUrl = lowRes,
                    outputFileExtension = extension
                )
            }
    }

    fun onLoadIntoImageViewSuccess(imageUrlAtPosition: String, position: Int) {
        val newImageUiState = createNewImageUiState(imageUrlAtPosition, position, loadSuccess = true)
        val newImageUiStates = updateViewPagerItemsUiStates(newImageUiState, position)

        val currentUiState = uiState.value as UiState
        val imageStateAtPosition = currentUiState.viewPagerItemsStates[position]

        if (currentUiState.viewPagerItemsStates.hasSingleElement() && canLoadToFile(newImageUiState)) {
            updateLoadIntoFileState(
                ImageStartLoadingToFileState(
                    imageUrlAtPosition = imageStateAtPosition.data.highResImageUrl,
                    position = position
                )
            )
        }

        val enableEditActions = if (position == selectedPosition) {
            shouldEnableEditActionsForImageState(newImageUiState)
        } else {
            currentUiState.editActionsEnabled
        }

        updateUiState(
            currentUiState.copy(
                viewPagerItemsStates = newImageUiStates,
                editActionsEnabled = enableEditActions
            )
        )
    }

    fun onLoadIntoImageViewFailed(imageUrlAtPosition: String, position: Int) {
        val newImageUiState = createNewImageUiState(imageUrlAtPosition, position, loadSuccess = false)
        val newImageUiStates = updateViewPagerItemsUiStates(newImageUiState, position)

        val currentUiState = uiState.value as UiState
        updateUiState(currentUiState.copy(viewPagerItemsStates = newImageUiStates))
    }

    fun onLoadIntoFileSuccess(inputFilePathAtPosition: String, position: Int) {
        val imageStateAtPosition = (uiState.value as UiState).viewPagerItemsStates[position]
        val outputFileExtension = imageStateAtPosition.data.outputFileExtension

        updateLoadIntoFileState(ImageLoadToFileSuccessState(inputFilePathAtPosition, position))

        val currentUiState = uiState.value as UiState
        _navigateToCropScreenWithFileInfo.value = Event(
            Triple(
                inputFilePathAtPosition,
                outputFileExtension,
                !currentUiState.viewPagerItemsStates.hasSingleElement()
            )
        )
    }

    fun onLoadIntoFileFailed(exception: Exception?) {
        updateLoadIntoFileState(
            ImageLoadToFileFailedState(
                exception?.message,
                R.string.error_failed_to_load_into_file
            )
        )
    }

    fun onCropMenuClicked() {
        val highResImageUrl = getHighResImageUrl(selectedPosition)

        if (isFileUrl(highResImageUrl)) {
            onLoadIntoFileSuccess(
                inputFilePathAtPosition = URI(highResImageUrl).path as String,
                position = selectedPosition
            )
        } else {
            updateLoadIntoFileState(
                ImageStartLoadingToFileState(
                    imageUrlAtPosition = highResImageUrl,
                    position = selectedPosition
                )
            )
        }
    }

    fun onPageSelected(selectedPosition: Int) {
        this.selectedPosition = selectedPosition
        val currentUiState = uiState.value as UiState
        val imageStateAtPosition = currentUiState.viewPagerItemsStates[selectedPosition]

        updateUiState(
            currentUiState.copy(
                editActionsEnabled = shouldEnableEditActionsForImageState(imageStateAtPosition)
            )
        )
    }

    private fun onLoadIntoImageViewRetry(selectedImageUrl: String, selectedPosition: Int) {
        val newImageUiState = createNewImageUiState(
            imageUrlAtPosition = selectedImageUrl,
            position = selectedPosition,
            loadSuccess = false,
            retry = true
        )
        val newImageUiStates = updateViewPagerItemsUiStates(newImageUiState, selectedPosition)

        val currentUiState = uiState.value as UiState
        updateUiState(currentUiState.copy(viewPagerItemsStates = newImageUiStates))
    }

    fun onCropResult(outputFilePath: String) {
        val imageStateAtPosition = (uiState.value as UiState).viewPagerItemsStates[selectedPosition]

        // Update urls with cache file path
        val newImageData = imageStateAtPosition.data.copy(
            lowResImageUrl = outputFilePath,
            highResImageUrl = outputFilePath
        )
        val newImageUiState = ImageDataStartLoadingUiState(newImageData)
        val newImageUiStates = updateViewPagerItemsUiStates(newImageUiState, selectedPosition)

        val currentUiState = uiState.value as UiState
        updateUiState(currentUiState.copy(viewPagerItemsStates = newImageUiStates))
    }

    private fun createViewPagerItemsInitialUiStates(
        data: List<ImageData>
    ): List<ImageUiState> = data.map { createImageLoadStartUiState(it) }

    private fun updateViewPagerItemsUiStates(
        newImageUiState: ImageUiState,
        position: Int
    ): List<ImageUiState> {
        val currentUiState = uiState.value as UiState
        val items = currentUiState.viewPagerItemsStates.toMutableList()
        items[position] = newImageUiState
        return items
    }

    private fun createNewImageUiState(
        imageUrlAtPosition: String,
        position: Int,
        loadSuccess: Boolean,
        retry: Boolean = false
    ): ImageUiState {
        val currentUiState = uiState.value as UiState
        val imageStateAtPosition = currentUiState.viewPagerItemsStates[position]
        val imageDataAtPosition = imageStateAtPosition.data
        return when {
            loadSuccess -> createImageLoadSuccessUiState(imageUrlAtPosition, imageStateAtPosition)
            retry -> createImageLoadStartUiState(imageDataAtPosition)
            else -> createImageLoadFailedUiState(imageUrlAtPosition, imageStateAtPosition, position)
        }
    }

    private fun createImageLoadStartUiState(
        imageData: ImageData
    ): ImageUiState {
        return ImageDataStartLoadingUiState(imageData)
    }

    private fun createImageLoadSuccessUiState(
        imageUrl: String,
        imageState: ImageUiState
    ): ImageUiState {
        val imageData = imageState.data
        return if (imageData.hasValidLowResImageUrlEqualTo(imageUrl)) {
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
        imageStateAtPosition: ImageUiState,
        position: Int
    ): ImageUiState {
        val imageDataAtPosition = imageStateAtPosition.data
        val imageUiState = if (imageDataAtPosition.hasValidLowResImageUrlEqualTo(imageUrlAtPosition)) {
            val isRetryShown = imageStateAtPosition is ImageInHighResLoadFailedUiState
            ImageInLowResLoadFailedUiState(imageDataAtPosition, isRetryShown)
        } else {
            ImageInHighResLoadFailedUiState(imageDataAtPosition)
        }

        imageUiState.onItemTapped = {
            onLoadIntoImageViewRetry(imageUrlAtPosition, position)
        }

        return imageUiState
    }

    private fun updateUiState(state: UiState) {
        _uiState.value = state
    }

    private fun updateLoadIntoFileState(fileState: ImageLoadToFileState) {
        _loadIntoFile.value = Event(fileState)
    }

    private fun shouldEnableEditActionsForImageState(imageState: ImageUiState) =
        imageState is ImageInHighResLoadSuccessUiState

    // TODO: revisit
    private fun canLoadToFile(imageState: ImageUiState) = imageState is ImageInHighResLoadSuccessUiState

    private fun List<ImageUiState>.hasSingleElement() = this.size == 1

    fun onInsertClicked() {
        val outputData = uiState.value?.viewPagerItemsStates?.map { OutputData(it.data.highResImageUrl) }
            ?: emptyList()
        _finishAction.value = Event(outputData)
    }

    fun getThumbnailImageUrl(position: Int): String {
        return uiState.value?.viewPagerItemsStates?.get(position)?.data?.let { imageData ->
            return if (TextUtils.isEmpty(imageData.lowResImageUrl))
                imageData.highResImageUrl
            else
                imageData.lowResImageUrl as String
        } ?: ""
    }

    private fun getHighResImageUrl(position: Int): String =
        uiState.value?.viewPagerItemsStates?.get(position)?.data?.highResImageUrl ?: ""

    private fun isFileUrl(url: String): Boolean = url.toLowerCase().startsWith(FILE_BASE)

    data class ImageData(
        val id: Long = UUID.randomUUID().hashCode().toLong(),
        val lowResImageUrl: String?,
        val highResImageUrl: String,
        val outputFileExtension: String
    ) {
        fun hasValidLowResImageUrlEqualTo(imageUrl: String): Boolean {
            val hasValidLowResImageUrl = this.lowResImageUrl?.isNotEmpty() == true &&
                this.lowResImageUrl != this.highResImageUrl
            val isGivenUrlEqualToLowResImageUrl = imageUrl == this.lowResImageUrl

            return hasValidLowResImageUrl && isGivenUrlEqualToLowResImageUrl
        }
    }

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
        data class ImageInLowResLoadSuccessUiState(val imageData: ImageData, val isRetryShown: Boolean = false) :
            ImageUiState(
                data = imageData,
                progressBarVisible = !isRetryShown,
                retryLayoutVisible = isRetryShown
            )

        data class ImageInLowResLoadFailedUiState(val imageData: ImageData, val isRetryShown: Boolean = false) :
            ImageUiState(
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

        data class ImageLoadToFileFailedState(val errorMsg: String?, val errorResId: Int) :
            ImageLoadToFileState()
    }

    companion object {
        private const val FILE_BASE = "file:"
    }
}
