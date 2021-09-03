package org.wordpress.android.ui.reader.subfilter

import org.wordpress.android.ui.utils.UiString.UiStringRes

sealed class SubfilterBottomSheetEmptyUiState {
    object HiddenEmptyUiState : SubfilterBottomSheetEmptyUiState()

    data class VisibleEmptyUiState(
        val title: UiStringRes,
        val buttonText: UiStringRes,
        val action: ActionType
    ) : SubfilterBottomSheetEmptyUiState()
}

sealed class ActionType {
    data class OpenSubsAtPage(
        val tabIndex: Int
    ) : ActionType()

    object OpenLoginPage : ActionType()
}
