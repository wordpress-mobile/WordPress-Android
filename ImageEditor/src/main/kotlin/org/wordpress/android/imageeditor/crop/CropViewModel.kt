package org.wordpress.android.imageeditor.crop

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CropViewModel : ViewModel() {
    private val _shouldCropAndSaveImage = MutableLiveData<Boolean>(false)
    val shouldCropAndSaveImage: LiveData<Boolean> = _shouldCropAndSaveImage

    private val _navigateBackWithCropResult = MutableLiveData<Pair<Int, Intent>>()
    val navigateBackWithCropResult: LiveData<Pair<Int, Intent>> = _navigateBackWithCropResult

    fun onDoneMenuClicked() {
        _shouldCropAndSaveImage.value = true
    }

    fun onCropFinish(cropResultCode: Int, cropData: Intent) {
        this._navigateBackWithCropResult.value = Pair(cropResultCode, cropData)
    }
}
