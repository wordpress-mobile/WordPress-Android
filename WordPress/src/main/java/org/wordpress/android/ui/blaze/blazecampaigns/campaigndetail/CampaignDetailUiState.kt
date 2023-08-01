package org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail

import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString

sealed class CampaignDetailUiState {
    object Preparing : CampaignDetailUiState()

    data class Prepared(
        val model: CampaignDetailModel
    ) : CampaignDetailUiState()

    object Loaded : CampaignDetailUiState()

    open class Error(
        val title: UiString,
        val description: UiString,
        val button: ErrorButton? = null
    ) : CampaignDetailUiState() {
        data class ErrorButton(
            val text: UiString,
            val click: () -> Unit
        )
    }

    data class NoNetworkError(val buttonClick: () -> Unit): Error(
        title = UiString.UiStringRes(R.string.campaign_detail_no_network_error_title),
        description = UiString.UiStringRes(R.string.campaign_detail_error_description),
        button = ErrorButton(
            text = UiString.UiStringRes(R.string.campaign_detail_error_button_text),
            click = buttonClick
        )
    )

    data class GenericError(val buttonClick: () -> Unit): Error(
        title = UiString.UiStringRes(R.string.campaign_detail_error_title),
        description = UiString.UiStringRes(R.string.campaign_detail_error_description),
        button = ErrorButton(
            text = UiString.UiStringRes(R.string.campaign_detail_error_button_text),
            click = buttonClick
        )
    )
}

data class CampaignDetailModel(
    val enableJavascript: Boolean = true,
    val enableDomStorage: Boolean = true,
    val enableChromeClient: Boolean = true,
    val userAgent: String = "",
    val url: String = "",
    val addressToLoad: String = ""
)
