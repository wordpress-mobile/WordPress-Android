package org.wordpress.android.ui.sitecreation.verticals

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.support.annotation.StringRes
import org.wordpress.android.R
import javax.inject.Inject
import kotlin.properties.Delegates

class NewSiteCreationSiteInfoViewModel @Inject constructor() : ViewModel() {
    private var currentUiState: SiteInfoUiState by Delegates.observable(
            SiteInfoUiState(
                    businessName = "",
                    tagLine = ""
            )
    ) { _, _, newValue ->
        _uiState.value = newValue
    }

    private val _uiState: MutableLiveData<SiteInfoUiState> = MutableLiveData()
    val uiState: LiveData<SiteInfoUiState> = _uiState

    fun updateBusinessName(businessName: String) {
        currentUiState = currentUiState.copy(businessName = businessName)
    }

    fun updateTagLine(tagLine: String) {
        currentUiState = currentUiState.copy(tagLine = tagLine)
    }

    data class SiteInfoUiState(
        val businessName: String,
        val tagLine: String
    ) {
        @StringRes val skipNextButtonTitleRes: Int = if (businessName.isEmpty() && tagLine.isEmpty()) {
            R.string.new_site_creation_button_skip
        } else R.string.new_site_creation_button_next
    }
}
