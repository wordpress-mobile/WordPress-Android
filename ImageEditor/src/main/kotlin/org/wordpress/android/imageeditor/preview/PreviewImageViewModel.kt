package org.wordpress.android.imageeditor.preview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PreviewImageViewModel : ViewModel() {
    private lateinit var lowResImageUrl: String
    private lateinit var highResImageUrl: String

    private val _loadImageFromData = MutableLiveData<ImageData>()
    val loadImageFromData: LiveData<ImageData> = _loadImageFromData

    private var isStarted = false

    fun start(loResImageUrl: String, hiResImageUrl: String) {
        if (isStarted) {
            return
        }
        isStarted = true

        lowResImageUrl = loResImageUrl
        highResImageUrl = hiResImageUrl

        _loadImageFromData.value = ImageData(lowResImageUrl, highResImageUrl)
    }

    data class ImageData(val lowResImageUrl: String, val highResImageUrl: String)
}
