package org.wordpress.android.imageeditor.preview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileFailedState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileIdleState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileSuccessState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageDataStartLoadingUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.UiState.ViewPagerItemsUiState

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
        this.outputFileExtension = outputFileExtension
        updateUiState(UiState(viewPagerItemsUiState = createViewPagerItemsUiState(imageDataList)))
    }

    fun onLoadIntoImageViewSuccess(currentUrl: String, imageData: ImageData) {
        /*val currentState = uiState.value
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

        newState?.let { updateUiState(it) }*/
    }

    fun onLoadIntoImageViewFailed(url: String) {
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

    private fun createImageDataStartLoadingUiState(
        loResImageUrl: String,
        hiResImageUrl: String
    ): ImageDataStartLoadingUiState = ImageDataStartLoadingUiState(
        ImageData(loResImageUrl, hiResImageUrl)
    )

    private fun createViewPagerItemsUiState(imageDataList: List<ImageData>): ViewPagerItemsUiState {
        val items = createImagesUiStates(data = imageDataList)
        return ViewPagerItemsUiState.ImagesUiContentState(items)
    }

    private fun createImagesUiStates(
        data: List<ImageData>
    ): List<ImageUiState> {
        val items: ArrayList<ImageUiState> = ArrayList()

        data.forEach { imageData ->
            val itemUiState = ImageDataStartLoadingUiState(imageData)
            itemUiState.onItemTapped = {
            }
            items.add(itemUiState)
        }

        return items
    }

    private fun updateUiState(state: UiState) {
        _uiState.value = state
    }

    private fun updateLoadIntoFileState(fileState: ImageLoadToFileState) {
        _loadIntoFile.value = fileState
    }

    data class ImageData(val lowResImageUrl: String, val highResImageUrl: String)

    data class UiState(
        val viewPagerItemsUiState: ViewPagerItemsUiState
    ) {
        sealed class ViewPagerItemsUiState(
            val items: List<ImageUiState>
        ) {
            class ImagesUiContentState(items: List<ImageUiState>) : ViewPagerItemsUiState(
                items = items
            )
        }
    }

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
