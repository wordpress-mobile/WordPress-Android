package org.wordpress.android.imageeditor.preview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileFailedState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileIdleState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileSuccessState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageStartLoadingToFileState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.UiState.ImageUiState.ImageDataStartLoadingUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.UiState.ImageUiState.ImageInHighResLoadFailedUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.UiState.ImageUiState.ImageInHighResLoadSuccessUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.UiState.ImageUiState.ImageInLowResLoadFailedUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.UiState.ImageUiState.ImageInLowResLoadSuccessUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.UiState.ThumbnailsUiState.ThumbnailsContentUiState

class PreviewImageViewModel : ViewModel() {
    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    private val _loadIntoFileState = MutableLiveData<ImageLoadToFileState>(ImageLoadToFileIdleState)
    val loadIntoFileState: LiveData<ImageLoadToFileState> = _loadIntoFileState

    private val _navigateToCropScreenWithFileInfo = MutableLiveData<Pair<String, String?>>()
    val navigateToCropScreenWithFileInfo: LiveData<Pair<String, String?>> = _navigateToCropScreenWithFileInfo

    private var outputFileExtension: String? = null

    private val imageDataList = mutableListOf(
            ImageData(
                lowResImageUrl = "https://testtravel123com.files.wordpress.com/2020/02/pexels-photo-1363876.jpg?w=268",
                highResImageUrl = "https://testtravel123com.files.wordpress.com/2020/02/pexels-photo-1363876.jpg"
            ),
            ImageData(
                lowResImageUrl = "https://testtravel123com.files.wordpress.com/2020/01/img_0001.jpg?w=268",
                highResImageUrl = "https://testtravel123com.files.wordpress.com/2020/01/img_0001.jpg"
            ),
            ImageData(
                lowResImageUrl = "https://testtravel123com.files.wordpress.com/2019/11/store-2209526_1920.jpg?w=268",
                highResImageUrl = "https://testtravel123com.files.wordpress.com/2019/11/store-2209526_1920.jpg"
            ),
            ImageData(
                lowResImageUrl = "https://testtravel123com.files.wordpress.com/2019/11/bangkok-3481970_1920.jpg?w=268",
                highResImageUrl = "https://testtravel123com.files.wordpress.com/2019/11/bangkok-3481970_1920.jpg"
            ),
            ImageData(
                lowResImageUrl = "https://testtravel123com.files.wordpress.com/2019/11/shop-2607121_1920.jpg?w=268",
                highResImageUrl = "https://testtravel123com.files.wordpress.com/2019/11/shop-2607121_1920.jpg"
            ),
            ImageData(
                lowResImageUrl = "https://testtravel123com.files.wordpress.com/2019/11/books-1163695_1920.jpg?w=268",
                highResImageUrl = "https://testtravel123com.files.wordpress.com/2019/11/books-1163695_1920.jpg"
            )
    )

    fun onCreateView(loResImageUrl: String, hiResImageUrl: String, outputFileExtension: String?) {
        val imageUiState = createImageDataStartLoadingUiState(
            loResImageUrl,
            hiResImageUrl
        )
        val thumbnailsUiState = uiState.value?.thumbnailsUiState ?: createThumbnailsContentUiState(imageDataList)

        updateUiState(UiState(imageUiState, thumbnailsUiState))

        this.outputFileExtension = outputFileExtension
    }

    fun onLoadIntoImageViewSuccess(currentUrl: String, imageData: ImageData) {
        val currentState = uiState.value as UiState
        val isHighResImageAlreadyLoaded = currentState.imageUiState == ImageInHighResLoadSuccessUiState

        val newState = if (currentUrl == imageData.lowResImageUrl) {
            if (!isHighResImageAlreadyLoaded) {
                ImageInLowResLoadSuccessUiState
            } else {
                null // don't update state if high res image already loaded
            }
        } else {
            ImageInHighResLoadSuccessUiState
        }

        val highResImageJustLoadedIntoView = newState != currentState.imageUiState &&
                newState == ImageInHighResLoadSuccessUiState
        val imageNotLoadedIntoFile = loadIntoFileState.value !is ImageLoadToFileSuccessState
        if (highResImageJustLoadedIntoView && imageNotLoadedIntoFile) {
            updateLoadIntoFileState(ImageStartLoadingToFileState(currentUrl))
        }

        newState?.let {
            updateUiState(currentState.copy(imageUiState = it))
        }
    }

    fun onLoadIntoImageViewFailed(url: String) {
        val currentUiState = (uiState.value as UiState)
        val newImageUiState = when (val currentImageUiState = currentUiState.imageUiState) {
            is ImageDataStartLoadingUiState -> {
                val lowResImageUrl = currentImageUiState.imageData.lowResImageUrl
                if (url == lowResImageUrl) {
                    ImageInLowResLoadFailedUiState
                } else {
                    ImageInHighResLoadFailedUiState
                }
            }
            else -> ImageInHighResLoadFailedUiState
        }

        updateUiState(currentUiState.copy(imageUiState = newImageUiState))
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
        val currentState = uiState.value as UiState
        updateUiState(
            currentState.copy(imageUiState = createImageDataStartLoadingUiState(loResImageUrl, hiResImageUrl))
        )
    }

    /*fun onThumbnailClick(position: Int) {
        val currentUiState = uiState.value as UiState
        val thumbnailsUiState = currentUiState.thumbnailsUiState as ThumbnailsContentUiState
        val imageDataList = thumbnailsUiState.items.toMutableList()
        imageDataList.forEachIndexed { index, image ->
            imageDataList[index] = image.copy(isSelected = index == position)
        }

        val selectedItem = imageDataList[position]
        updateUiState(
            currentUiState.copy(
                imageUiState = createImageDataStartLoadingUiState(
                    selectedItem.lowResImageUrl,
                    selectedItem.highResImageUrl
                ),
                thumbnailsUiState = createThumbnailsContentUiState(imageDataList)
            )
        )
    }*/

    private fun createImageDataStartLoadingUiState(
        loResImageUrl: String,
        hiResImageUrl: String
    ): ImageDataStartLoadingUiState = ImageDataStartLoadingUiState(
        ImageData(loResImageUrl, hiResImageUrl)
    )

    private fun createThumbnailsContentUiState(
        imageDataList: MutableList<ImageData>
    ): ThumbnailsContentUiState = ThumbnailsContentUiState(imageDataList)

    private fun updateUiState(uiState: UiState) {
        _uiState.value = uiState
    }

    private fun updateLoadIntoFileState(fileState: ImageLoadToFileState) {
        _loadIntoFileState.value = fileState
    }

    data class ImageData(
        val lowResImageUrl: String,
        val highResImageUrl: String,
        var isSelected: Boolean = false
    )

    data class UiState(
        val imageUiState: ImageUiState,
        val thumbnailsUiState: ThumbnailsUiState
    ) {
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
        sealed class ThumbnailsUiState {
            data class ThumbnailsContentUiState(val items: List<ImageData>) : ThumbnailsUiState()
        }
    }

    sealed class ImageLoadToFileState {
        object ImageLoadToFileIdleState : ImageLoadToFileState()
        data class ImageStartLoadingToFileState(val imageUrl: String) : ImageLoadToFileState()
        data class ImageLoadToFileSuccessState(val filePath: String) : ImageLoadToFileState()
        object ImageLoadToFileFailedState : ImageLoadToFileState()
    }
}
