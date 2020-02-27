package org.wordpress.android.imageeditor.crop

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CropViewModel : ViewModel() {
    private val _shouldCropAndSaveImage = MutableLiveData<Boolean>(false)
    val shouldCropAndSaveImage: LiveData<Boolean> = _shouldCropAndSaveImage

    fun onDoneMenuClicked() {
        _shouldCropAndSaveImage.value = true
    }
}
