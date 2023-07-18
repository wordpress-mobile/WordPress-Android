package org.wordpress.android.ui.blazeCampaigns

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import javax.inject.Inject

@HiltViewModel
class CampaignViewModel @Inject constructor(
    private val blazeFeatureUtils: BlazeFeatureUtils
) : ViewModel() {

    private val _uiState = MutableLiveData<BlazeCampaignPage>()
    val uiState: MutableLiveData<BlazeCampaignPage> = _uiState

    fun start(page: BlazeCampaignPage?) {
        page?.let { _uiState.value = it }
    }

    fun onNavigationUp() {
        _uiState.value = BlazeCampaignPage.Done
    }
}
