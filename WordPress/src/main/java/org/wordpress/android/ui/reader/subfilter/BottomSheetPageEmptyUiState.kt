package org.wordpress.android.ui.reader.subfilter

import org.wordpress.android.ui.utils.UiString

sealed class SubfilterBottomSheetEmptyUiState {
    object HiddenEmptyUiState : SubfilterBottomSheetEmptyUiState()

    data class VisibleEmptyUiState(
        val title: UiString? = null,
        val text: UiString,
        val primaryButton: Button? = null,
        val secondaryButton: Button? = null
    ) : SubfilterBottomSheetEmptyUiState() {
        data class Button(
            val text: UiString,
            val action: ActionType,
        )
    }
}

sealed interface ActionType {
    data class OpenSubsAtPage(
        val tabIndex: Int
    ) : ActionType

    data object OpenLoginPage : ActionType

    data object OpenSearchPage : ActionType

    data object OpenSuggestedTagsPage : ActionType
}
