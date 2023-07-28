package org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail

import org.wordpress.android.ui.utils.UiString

sealed class CampaignDetailUiState {
    object Preparing : CampaignDetailUiState()

    data class Prepared(
        val model: CampaignDetailModel
    ) : CampaignDetailUiState()

    object Loaded : CampaignDetailUiState()

    data class Error(
        val title: UiString,
        val description: UiString,
        val button: ErrorButton? = null
    ) : CampaignDetailUiState() {
        data class ErrorButton(
            val text: UiString,
            val click: () -> Unit
        )
    }
}

data class CampaignDetailModel(
    val enableJavascript: Boolean = true,
    val enableDomStorage: Boolean = true,
    val enableChromeClient: Boolean = true,
    val userAgent: String = "",
    val url: String = "",
    val addressToLoad: String = ""
)
