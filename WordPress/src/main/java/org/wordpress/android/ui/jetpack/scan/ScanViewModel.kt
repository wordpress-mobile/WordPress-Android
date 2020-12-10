package org.wordpress.android.ui.jetpack.scan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

class ScanViewModel @Inject constructor() : ViewModel() {
    private var isStarted = false

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    lateinit var site: SiteModel

    fun start(site: SiteModel) {
        if (isStarted) {
            return
        }
        this.site = site
        isStarted = true
    }

    sealed class UiState {
        open val contentVisibility = false

        data class Content(
            val items: List<ScanListItemState>
        ) : UiState() {
            override val contentVisibility = true
        }
    }
}
