package org.wordpress.android.ui.blaze.blazecampaigns

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CampaignViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableLiveData<BlazeCampaignPage>()
    val uiState: MutableLiveData<BlazeCampaignPage> = _uiState

    fun start(page: BlazeCampaignPage?) {
        page?.let { _uiState.value = it }
    }

    fun onNavigationUp() {
        _uiState.value = BlazeCampaignPage.Done
    }
}
