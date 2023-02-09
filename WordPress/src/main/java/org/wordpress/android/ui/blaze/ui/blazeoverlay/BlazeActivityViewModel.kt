package org.wordpress.android.ui.blaze.ui.blazeoverlay

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BlazeActivityViewModel @Inject constructor(private val blazeFeatureUtils: BlazeFeatureUtils) : ViewModel() {

    private val _refreshAppTheme = MutableLiveData<Unit>()
    val refreshAppTheme: LiveData<Unit> = _refreshAppTheme

    private val _refreshAppLanguage = MutableLiveData<String>()
    val refreshAppLanguage: LiveData<String> = _refreshAppLanguage


    fun setAppLanguage(locale: Locale) {
        _refreshAppLanguage.value = locale.language
    }

    fun trackOverlayDisplayed() {
        blazeFeatureUtils.trackOverlayDisplayed()
    }

    fun onPromoteWithBlazeClicked() {
        blazeFeatureUtils.trackPromoteWithBlazeClicked()
    }
}
