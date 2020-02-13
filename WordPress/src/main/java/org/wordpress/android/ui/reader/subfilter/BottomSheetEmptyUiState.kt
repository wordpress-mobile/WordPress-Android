package org.wordpress.android.ui.reader.subfilter

import org.wordpress.android.ui.utils.UiString.UiStringRes

sealed class BottomSheetEmptyUiState {
    object HiddenEmptyUiState : BottomSheetEmptyUiState()

    data class VisibleEmptyUiState(
        val title: UiStringRes,
        val buttonText: UiStringRes,
        val actionTabIndex: Int
    ) : BottomSheetEmptyUiState()
}
