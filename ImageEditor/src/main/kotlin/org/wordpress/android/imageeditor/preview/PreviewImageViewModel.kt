package org.wordpress.android.imageeditor.preview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PreviewImageViewModel : ViewModel() {
    private lateinit var lowResImageUrl: String

    private val _loadImageFromUrl = MutableLiveData<String>()
    val loadImageFromUrl: LiveData<String> = _loadImageFromUrl

    private var isStarted = false

    fun start(loResImageUrl: String) {
        if (isStarted) {
            return
        }
        isStarted = true

        lowResImageUrl = loResImageUrl
        _loadImageFromUrl.value = lowResImageUrl
    }
}
