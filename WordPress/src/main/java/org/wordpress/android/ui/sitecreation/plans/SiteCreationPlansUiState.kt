package org.wordpress.android.ui.sitecreation.plans

import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString

sealed class SiteCreationPlansUiState {
    object Preparing : SiteCreationPlansUiState()

    data class Prepared(
        val model: SiteCreationPlansModel
    ) : SiteCreationPlansUiState()

    object Loaded : SiteCreationPlansUiState()

    open class Error(
        val title: UiString,
        val description: UiString,
        val button: ErrorButton? = null
    ) : SiteCreationPlansUiState() {
        data class ErrorButton(
            val text: UiString,
            val click: () -> Unit
        )
    }

    data class NoNetworkError(val buttonClick: () -> Unit): Error(
        title = UiString.UiStringRes(R.string.no_network_title),
        description = UiString.UiStringRes(R.string.request_failed_message),
        button = ErrorButton(
            text = UiString.UiStringRes(R.string.retry),
            click = buttonClick
        )
    )

    data class GenericError(val buttonClick: () -> Unit): Error(
        title = UiString.UiStringRes(R.string.jp_migration_generic_error_title),
        description = UiString.UiStringRes(R.string.request_failed_message),
        button = ErrorButton(
            text = UiString.UiStringRes(R.string.retry),
            click = buttonClick
        )
    )
}

data class SiteCreationPlansModel(
    val enableJavascript: Boolean = true,
    val enableDomStorage: Boolean = true,
    val enableChromeClient: Boolean = true,
    val userAgent: String = "",
    val url: String = "",
    val addressToLoad: String = ""
)
