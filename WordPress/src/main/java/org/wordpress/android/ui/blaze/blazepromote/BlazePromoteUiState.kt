package org.wordpress.android.ui.blaze.blazepromote

import org.wordpress.android.ui.utils.UiString

sealed class BlazePromoteUiState {
    object Preparing : BlazePromoteUiState()

    data class Loading(
        val model: BlazePromoteModel
    ) : BlazePromoteUiState()

    object Loaded : BlazePromoteUiState()

    data class Error(
        val title: UiString,
        val description: UiString,
        val button: ErrorButton? = null
    ) : BlazePromoteUiState() {
        data class ErrorButton(
            val text: UiString,
            val click: () -> Unit
        )
    }
}

data class BlazePromoteModel(
    val enableJavascript: Boolean = true,
    val enableDomStorage: Boolean = true,
    val enableChromeClient: Boolean = true,
    val userAgent: String = "",
    val url: String = "",
    val addressToLoad: String = ""
)
