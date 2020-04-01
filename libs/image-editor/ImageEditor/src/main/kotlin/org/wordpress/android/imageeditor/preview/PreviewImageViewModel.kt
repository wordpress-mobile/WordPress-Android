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
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ThumbnailsUiState.ThumbnailsContentUiState

class PreviewImageViewModel : ViewModel() {
    private val _uiState: MutableLiveData<ImageUiState> = MutableLiveData()
    val uiState: LiveData<ImageUiState> = _uiState

    private val _thumbnailsUiState = MutableLiveData<ThumbnailsUiState>()
    val thumbnailsUiState: LiveData<ThumbnailsUiState> = _thumbnailsUiState

    private val _loadIntoFileState = MutableLiveData<ImageLoadToFileState>(ImageLoadToFileIdleState)
    val loadIntoFileState: LiveData<ImageLoadToFileState> = _loadIntoFileState

    private val _navigateToCropScreenWithFileInfo = MutableLiveData<Pair<String, String?>>()
    val navigateToCropScreenWithFileInfo: LiveData<Pair<String, String?>> = _navigateToCropScreenWithFileInfo

    private var outputFileExtension: String? = null
    private var isStarted = false

    fun onCreateView(loResImageUrl: String, hiResImageUrl: String, outputFileExtension: String?) {
        updateUiState(createImageDataStartLoadingUiState(loResImageUrl, hiResImageUrl))

        if (isStarted) {
            return
        }

        this.outputFileExtension = outputFileExtension

        // TODO: Dummy data
        val imageDataList = mutableListOf(
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

        updateThumbnailsUiState(createThumbnailsContentUiState(imageDataList))
        isStarted = true
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
        val imageNotLoadedIntoFile = loadIntoFileState.value !is ImageLoadToFileSuccessState
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

    /*fun onThumbnailClick(position: Int) {
        val thumbnailsContentUiState = (thumbnailsUiState.value as? ThumbnailsContentUiState)
        val imageDataList = thumbnailsContentUiState?.items?.toMutableList() ?: mutableListOf()
        imageDataList.forEachIndexed { index, image ->
            imageDataList[index] = image.copy(isSelected = index == position)
        }
        updateThumbnailsUiState(createThumbnailsContentUiState(imageDataList))
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

    private fun updateUiState(uiState: ImageUiState) {
        _uiState.value = uiState
    }

    private fun updateThumbnailsUiState(thumbnailsUiState: ThumbnailsUiState) {
        _thumbnailsUiState.value = thumbnailsUiState
    }

    private fun updateLoadIntoFileState(fileState: ImageLoadToFileState) {
        _loadIntoFileState.value = fileState
    }

    data class ImageData(
        val lowResImageUrl: String,
        val highResImageUrl: String,
        var isSelected: Boolean = false
    )

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

    sealed class ImageLoadToFileState {
        object ImageLoadToFileIdleState : ImageLoadToFileState()
        data class ImageStartLoadingToFileState(val imageUrl: String) : ImageLoadToFileState()
        data class ImageLoadToFileSuccessState(val filePath: String) : ImageLoadToFileState()
        object ImageLoadToFileFailedState : ImageLoadToFileState()
    }
}
