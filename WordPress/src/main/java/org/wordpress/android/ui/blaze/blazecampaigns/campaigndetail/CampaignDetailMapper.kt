package org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail

import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import javax.inject.Inject

class CampaignDetailMapper @Inject constructor(
    private val blazeFeatureUtils: BlazeFeatureUtils,
) {
    fun toPrepared(url: String, addressToLoad: String) = CampaignDetailUiState.Prepared(
        model = CampaignDetailModel(
            enableJavascript = true,
            enableDomStorage = true,
            userAgent = blazeFeatureUtils.getUserAgent(),
            enableChromeClient = true,
            url = url,
            addressToLoad = addressToLoad
        )
    )

    fun toNoNetworkError(buttonClick: () -> Unit) = CampaignDetailUiState.NoNetworkError(buttonClick = buttonClick)

    fun toGenericError(buttonClick: () -> Unit) = CampaignDetailUiState.GenericError(buttonClick =  buttonClick)
}

