package org.wordpress.android.imageeditor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.imageeditor.crop.CropViewModel.CropResult
import org.wordpress.android.imageeditor.viewmodel.Event

class EditImageViewModel : ViewModel() {
    private val _cropResult = MutableLiveData<Event<CropResult>>()
    val cropResult: LiveData<Event<CropResult>> = _cropResult

    fun setCropResult(cropResult: CropResult) {
        _cropResult.value = Event(cropResult)
    }
}
