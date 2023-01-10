package org.wordpress.android.ui.reader.subfilter

import org.wordpress.android.ui.utils.UiString

sealed class BottomSheetUiState(val isVisible: Boolean) {
    data class BottomSheetVisible(
        val title: UiString,
        val categories: List<SubfilterCategory>
    ) : BottomSheetUiState(true)

    object BottomSheetHidden : BottomSheetUiState(false)
}
