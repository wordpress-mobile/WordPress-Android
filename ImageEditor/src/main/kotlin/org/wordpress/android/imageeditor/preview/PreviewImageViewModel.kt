package org.wordpress.android.imageeditor.preview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

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
            uiState = ImageUiState.IMAGE_LOAD_IN_PROGRESS,
            lowResImageUrl = loResImageUrl,
            highResImageUrl = hiResImageUrl
        )
    }

    data class ImageData(val uiState: ImageUiState, val lowResImageUrl: String, val highResImageUrl: String)

    enum class ImageUiState(
        val progressBarVisible: Boolean = false
    ) {
        IMAGE_LOAD_IN_PROGRESS(progressBarVisible = true),
        IMAGE_LOAD_SUCCESS(progressBarVisible = false),
        IMAGE_LOAD_FAILED(progressBarVisible = false);
    }
}
