package org.wordpress.android.imageeditor.preview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInitialContentUiState

class PreviewImageViewModel : ViewModel() {
    private val _loadImageFromData = MutableLiveData<ImageData>()
    val loadImageFromData: LiveData<ImageData> = _loadImageFromData

    private var isStarted = false

    fun start(loResImageUrl: String, hiResImageUrl: String) {
        if (isStarted) {
            return
        }
        isStarted = true

        _loadImageFromData.value = ImageData(
            uiState = ImageInitialContentUiState,
            lowResImageUrl = loResImageUrl,
            highResImageUrl = hiResImageUrl
        )
    }

    data class ImageData(val uiState: ImageUiState, val lowResImageUrl: String, val highResImageUrl: String)

    sealed class ImageUiState(
        val progressBarVisible: Boolean = false
    ) {
        object ImageInitialContentUiState : ImageUiState(progressBarVisible = true)
        object ImageLoadSuccessContentUiState : ImageUiState(progressBarVisible = false)
        object ImageLoadFailedContentUiState : ImageUiState(progressBarVisible = false)
    }
}
