package org.wordpress.android.ui.blaze.blazepromote

import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString

sealed class BlazePromoteUiState {
    object Preparing : BlazePromoteUiState()

    data class Loading(
        val model: BlazePromoteModel
    ) : BlazePromoteUiState()

    object Loaded : BlazePromoteUiState()

    open class Error(
        val title: UiString,
        val description: UiString,
        val button: ErrorButton? = null
    ) : BlazePromoteUiState() {
        data class ErrorButton(
            val text: UiString,
            val click: () -> Unit
        )
    }

    data class NoNetworkError(val buttonClick: () -> Unit): Error(
        title = UiString.UiStringRes(R.string.blaze_promote_no_network_error_title),
        description = UiString.UiStringRes(R.string.blaze_promote_error_description),
        button = ErrorButton(
            text = UiString.UiStringRes(R.string.blaze_promote_error_button_text),
            click = buttonClick
        )
    )

    data class GenericError(val buttonClick: () -> Unit): Error(
        title = UiString.UiStringRes(R.string.blaze_promote_error_title),
        description = UiString.UiStringRes(R.string.blaze_promote_error_description),
        button = ErrorButton(
            text = UiString.UiStringRes(R.string.blaze_promote_error_button_text),
            click = buttonClick
        )
    )
}

data class BlazePromoteModel(
    val enableJavascript: Boolean = true,
    val enableDomStorage: Boolean = true,
    val enableChromeClient: Boolean = true,
    val userAgent: String = "",
    val url: String = "",
    val addressToLoad: String = ""
)
