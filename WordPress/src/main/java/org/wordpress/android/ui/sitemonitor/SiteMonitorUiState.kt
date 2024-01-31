package org.wordpress.android.ui.sitemonitor

import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString

sealed class SiteMonitorUiState {
    object Preparing : SiteMonitorUiState()

    data class Prepared(
        val model: SiteMonitorModel
    ) : SiteMonitorUiState()

    object Loaded : SiteMonitorUiState()

    open class Error(
        val title: UiString,
        val description: UiString,
        val button: ErrorButton? = null
    ) : SiteMonitorUiState() {
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

data class SiteMonitorModel(
    val siteMonitorType: SiteMonitorType,
    val enableJavascript: Boolean = true,
    val enableDomStorage: Boolean = true,
    val enableChromeClient: Boolean = true,
    val userAgent: String = "",
    val url: String,
    val addressToLoad: String
)
enum class SiteMonitorType(val analyticsDescription: String) {
    METRICS("metrics"),
    PHP_LOGS("php_logs"),
    WEB_SERVER_LOGS("server_logs")
}
