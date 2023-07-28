package org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail

import org.wordpress.android.R
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.utils.UiString
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

    fun toNoNetworkError(buttonClick: () -> Unit) = CampaignDetailUiState.Error(
        title = UiString.UiStringRes(R.string.campaign_detail_no_network_error_title),
        description = UiString.UiStringRes(R.string.campaign_detail_error_description),
        button = CampaignDetailUiState.Error.ErrorButton(
            text = UiString.UiStringRes(R.string.campaign_detail_error_button_text),
            click = buttonClick
        )
    )

    fun toGenericError(buttonClick: () -> Unit) = CampaignDetailUiState.Error(
        title = UiString.UiStringRes(R.string.campaign_detail_error_title),
        description = UiString.UiStringRes(R.string.campaign_detail_error_description),
        button = CampaignDetailUiState.Error.ErrorButton(
            text = UiString.UiStringRes(R.string.campaign_detail_error_button_text),
            click = buttonClick
        )
    )
}

